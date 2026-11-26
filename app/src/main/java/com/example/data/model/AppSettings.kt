package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedServer(
    val id: String,
    val name: String,
    val url: String,
    val username: String,
    val userId: String,
    val accessToken: String,
    val serverVersion: String = ""
)

enum class ThemeMode {
    CINEMA_DARK,
    AMOLED,
    LIGHT,
    SYSTEM
}

enum class AppLanguage {
    ARABIC,
    ENGLISH,
    SYSTEM
}

enum class PlaybackMode {
    DIRECT_PLAY,
    AUTO_TRANSCODE,
    FORCE_HLS
}

enum class StreamQuality(val labelAr: String, val labelEn: String, val bitrateBps: Long?) {
    ORIGINAL("الجودة الأصلية (دون ضغط)", "Original (Uncompressed)", null),
    UHD_4K_80("4K فائق الدقة (80 Mbps)", "4K Ultra HD (80 Mbps)", 80_000_000L),
    FHD_20("1080p عالية (20 Mbps)", "1080p High (20 Mbps)", 20_000_000L),
    FHD_10("1080p متوسطة (10 Mbps)", "1080p Medium (10 Mbps)", 10_000_000L),
    HD_4("720p HD (4 Mbps)", "720p HD (4 Mbps)", 4_000_000L),
    SD_1_5("480p SD (1.5 Mbps)", "480p SD (1.5 Mbps)", 1_500_000L),
    LOW_0_8("360p توفير البيانات (800 Kbps)", "360p Data Saver (800 Kbps)", 800_000L)
}

enum class PreferredCodec(val label: String, val codecValue: String) {
    AUTO("تلقائي (Auto)", "h264,hevc"),
    H264("H.264 (توافق أعلى)", "h264"),
    HEVC("HEVC / H.265 (ضغط أفضل)", "hevc")
}

enum class SubtitleSize(val spValue: Float, val labelAr: String, val labelEn: String) {
    SMALL(14f, "صغير", "Small"),
    MEDIUM(18f, "متوسط", "Medium"),
    LARGE(22f, "كبير", "Large"),
    EXTRA_LARGE(27f, "ضخم", "Extra Large")
}

enum class ResumeBehavior {
    ASK,
    AUTO_RESUME,
    START_OVER
}

enum class GridDensity {
    COMFORTABLE,
    COMPACT
}

enum class SortOption(val sortBy: String, val sortOrder: String) {
    LATEST("DateCreated", "Descending"),
    NAME("SortName", "Ascending"),
    RATING("CommunityRating", "Descending"),
    YEAR("ProductionYear", "Descending")
}

enum class AspectRatioMode {
    FIT,
    FILL,
    ZOOM
}

data class UserSettings(
    val activeServer: SavedServer? = null,
    val savedServers: List<SavedServer> = emptyList(),
    val autoLogin: Boolean = true,
    val playbackMode: PlaybackMode = PlaybackMode.AUTO_TRANSCODE,
    val wifiQuality: StreamQuality = StreamQuality.ORIGINAL,
    val mobileQuality: StreamQuality = StreamQuality.HD_4,
    val preferredCodec: PreferredCodec = PreferredCodec.AUTO,
    val defaultSpeed: Float = 1.0f,
    val seekSeconds: Int = 15,
    val autoPlayNextEpisode: Boolean = true,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.ASK,
    val preferredAudioLang: String = "auto",
    val preferredSubLang: String = "ara",
    val subtitleSize: SubtitleSize = SubtitleSize.MEDIUM,
    val keepScreenOn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.CINEMA_DARK,
    val language: AppLanguage = AppLanguage.ARABIC,
    val showProgressBars: Boolean = true,
    val gridDensity: GridDensity = GridDensity.COMFORTABLE,
    val timeoutSeconds: Int = 20,
    val recentSearches: List<String> = emptyList()
)
