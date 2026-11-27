package com.jellyarr.player

import com.jellyarr.player.data.nuvio.NuvioRepository
import com.jellyarr.player.data.streams.StreamParser
import com.jellyarr.player.data.stremio.StremioClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamParserTest {

    @Test
    fun parsesAioStreamsStyleName() {
        val t = StreamParser.parse(
            "[RD+] AIOStreams 4K",
            "Movie.2023.2160p.UHD.BluRay.REMUX.DV.HDR10.HEVC.TrueHD.Atmos.7.1-GROUP\n💾 58.3 GB 👤 120 🇺🇸 🇫🇷",
        )
        assertEquals(2160, t.resolution)
        assertEquals("REMUX", t.source)
        assertEquals("HEVC", t.codec)
        assertTrue(t.hdr.contains("DV"))
        assertTrue(t.hdr.contains("HDR10"))
        assertTrue(t.audio.contains("Atmos"))
        assertTrue(t.audio.contains("TrueHD"))
        assertEquals("7.1", t.channels)
        assertEquals(120, t.seeders)
        assertEquals(true, t.cached)
        assertEquals("Real-Debrid", t.debrid)
        assertEquals(2, t.languages.size)
        assertTrue((t.sizeBytes ?: 0) > 58L * 1_000_000_000)
    }

    @Test
    fun parsesUncachedAndWebDl() {
        val t = StreamParser.parse("[TB download] Torrentio\n1080p", "Show.S01E02.1080p.WEB-DL.DDP5.1.H.264\n💾 2.1 GB")
        assertEquals(1080, t.resolution)
        assertEquals("WEB-DL", t.source)
        assertEquals("H.264", t.codec)
        assertEquals(false, t.cached)
        assertEquals("TorBox", t.debrid)
        assertTrue(t.audio.contains("DD+"))
    }

    @Test
    fun unknownResolution() {
        val t = StreamParser.parse("Some stream", "no info here")
        assertEquals(0, t.resolution)
        assertNull(t.resolutionLabel)
    }

    @Test
    fun nuvioQuality() {
        assertEquals(1080, StreamParser.qualityToResolution("1080p"))
        assertEquals(2160, StreamParser.qualityToResolution("4K"))
        assertEquals(720, StreamParser.qualityToResolution("720"))
    }

    @Test
    fun manifestUrls() {
        assertEquals("https://example.com/abc/manifest.json", StremioClient.normalizeManifestUrl("stremio://example.com/abc/manifest.json"))
        assertEquals("https://example.com/abc/manifest.json", StremioClient.normalizeManifestUrl("https://example.com/abc/"))
        assertEquals("https://example.com/abc/manifest.json", StremioClient.normalizeManifestUrl("example.com/abc"))
        assertEquals(
            "https://raw.githubusercontent.com/tapframe/nuvio-providers/main/manifest.json",
            NuvioRepository.manifestUrl("https://github.com/tapframe/nuvio-providers"),
        )
        assertEquals("https://x.dev/repo/manifest.json", NuvioRepository.manifestUrl("https://x.dev/repo/manifest.json"))
    }
}
