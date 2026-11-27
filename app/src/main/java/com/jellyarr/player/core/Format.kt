package com.jellyarr.player.core

import java.util.Locale

object Format {
    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun runtime(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}س ${m}د"
            h > 0 -> "${h}س"
            else -> "${m}د"
        }
    }

    fun bytes(size: Long?): String? {
        if (size == null || size <= 0) return null
        val gb = size / 1_073_741_824.0
        return if (gb >= 1) String.format(Locale.US, "%.2f GB", gb)
        else String.format(Locale.US, "%.0f MB", size / 1_048_576.0)
    }

    fun episodeCode(season: Int?, episode: Int?): String? {
        if (season == null || episode == null) return null
        return String.format(Locale.US, "S%02dE%02d", season, episode)
    }
}
