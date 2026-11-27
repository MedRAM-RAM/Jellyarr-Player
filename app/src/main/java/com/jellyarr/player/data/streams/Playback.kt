package com.jellyarr.player.data.streams

import com.jellyarr.player.AppContainer
import com.jellyarr.player.core.Format
import com.jellyarr.player.data.HistoryEntry
import com.jellyarr.player.data.jellyfin.JfItem

object Playback {

    fun fromJellyfin(c: AppContainer, item: JfItem, mediaSourceId: String? = null, resume: Boolean = true): PlaybackRequest {
        val jf = c.jellyfin
        val prefs = c.settings.state.value.playback
        val msId = mediaSourceId ?: item.mediaSources.firstOrNull()?.id ?: item.id
        val maxBitrate = prefs.maxBitrateMbps.takeIf { it > 0 }?.let { it * 1_000_000L }
        val direct = jf.directStreamUrl(item.id, msId)
        val hls = jf.hlsTranscodeUrl(item.id, msId, maxBitrate)
        val transcodeFirst = prefs.jellyfinTranscode || maxBitrate != null
        val title = if (item.type == "Episode") item.seriesName ?: item.name else item.name
        val subtitle = if (item.type == "Episode") listOfNotNull(Format.episodeCode(item.parentIndexNumber, item.indexNumber), item.name).joinToString(" · ") else item.year?.toString()
        return PlaybackRequest(
            url = if (transcodeFirst) hls else direct,
            fallbackUrl = if (transcodeFirst) null else hls,
            title = title,
            subtitle = subtitle,
            artwork = jf.backdropUrl(item),
            startPositionMs = if (resume) (item.userData?.playbackPositionTicks ?: 0) / 10_000 else 0,
            jellyfinItemId = item.id,
            jellyfinMediaSourceId = msId,
            stremioType = if (item.type == "Episode") "series" else "movie",
            stremioId = item.imdbId?.let { imdb ->
                if (item.type == "Episode") null else imdb
            },
        )
    }

    fun fromStream(c: AppContainer, req: StreamRequest, s: StreamSource): PlaybackRequest {
        if (s.providerKind == ProviderKind.JELLYFIN && s.jellyfinItemId != null) {
            val prefs = c.settings.state.value.playback
            val maxBitrate = prefs.maxBitrateMbps.takeIf { it > 0 }?.let { it * 1_000_000L }
            val hls = c.jellyfin.hlsTranscodeUrl(s.jellyfinItemId, s.jellyfinMediaSourceId, maxBitrate)
            val transcodeFirst = prefs.jellyfinTranscode || maxBitrate != null
            return PlaybackRequest(
                url = if (transcodeFirst) hls else s.url!!,
                fallbackUrl = if (transcodeFirst) null else hls,
                title = req.title,
                subtitle = subtitleOf(req),
                artwork = req.backdrop ?: req.poster,
                jellyfinItemId = s.jellyfinItemId,
                jellyfinMediaSourceId = s.jellyfinMediaSourceId,
                stremioType = req.type,
                stremioId = req.stremioId,
                startPositionMs = -1, // resolved from server
            )
        }
        val history = req.metaId?.let { metaId ->
            HistoryEntry(
                metaId = metaId, type = req.type, name = req.title, poster = req.poster, backdrop = req.backdrop,
                videoId = req.stremioId, season = req.season, episode = req.episode, episodeTitle = req.episodeTitle,
            )
        }
        val saved = req.metaId?.let { c.history.find(req.type, it) }
            ?.takeIf { it.videoId == req.stremioId && it.progress in 0.02f..0.92f }
        return PlaybackRequest(
            url = s.url!!,
            headers = s.headers,
            title = req.title,
            subtitle = subtitleOf(req),
            artwork = req.backdrop ?: req.poster,
            startPositionMs = saved?.positionMs ?: 0,
            externalSubtitles = s.subtitles,
            stremioType = req.type,
            stremioId = req.stremioId ?: req.imdbId,
            history = history,
            filename = s.filename,
        )
    }

    private fun subtitleOf(req: StreamRequest) =
        listOfNotNull(Format.episodeCode(req.season, req.episode), req.episodeTitle).joinToString(" · ").ifBlank { null }
}
