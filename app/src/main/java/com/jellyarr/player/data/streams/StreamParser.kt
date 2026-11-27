package com.jellyarr.player.data.streams

import java.util.Locale

/** Extracts quality/size/codec/debrid info from free-form stream names (AIOStreams, Torrentio, Comet, …). */
object StreamParser {
    private val RES = listOf(
        Regex("(?i)\\b(2160p|4k|uhd)\\b") to 2160,
        Regex("(?i)\\b1440p\\b") to 1440,
        Regex("(?i)\\b(1080p|1080i|fhd)\\b") to 1080,
        Regex("(?i)\\b(720p)\\b") to 720,
        Regex("(?i)\\b576p\\b") to 576,
        Regex("(?i)\\b(480p|sd)\\b") to 480,
        Regex("(?i)\\b360p\\b") to 360,
    )
    private val SIZE = Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(TB|TiB|GB|GiB|MB|MiB)\\b")
    private val SEEDERS = listOf(Regex("👤\\s*(\\d+)"), Regex("(?i)(\\d+)\\s*seed(?:er)?s?\\b"), Regex("(?i)seed(?:er)?s?[:\\s]+(\\d+)"))
    private val DEBRID_TAG = Regex("\\[(RD|AD|TB|PM|DL|ED|OC|PKP|PP|DR|TR|NZB|STRM|EN|ST|SB)\\s*([+⚡✓]|download|⏳)?\\s*]", RegexOption.IGNORE_CASE)
    private val FLAG = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}")

    private val DEBRID_NAMES = mapOf(
        "RD" to "Real-Debrid", "AD" to "AllDebrid", "TB" to "TorBox", "PM" to "Premiumize",
        "DL" to "Debrid-Link", "ED" to "EasyDebrid", "OC" to "Offcloud", "PKP" to "PikPak", "PP" to "PikPak",
        "DR" to "Debrider", "EN" to "Easynews", "NZB" to "Usenet", "ST" to "StremThru", "SB" to "Seedbox",
    )

    fun parse(vararg parts: String?, sizeHint: Long? = null): ParsedTags {
        val text = parts.filterNotNull().joinToString("\n")
        val norm = text.replace('.', ' ').replace('_', ' ')

        val resolution = RES.firstOrNull { it.first.containsMatchIn(norm) }?.second ?: 0

        val hdr = buildList {
            if (Regex("(?i)\\b(DV|DoVi|dolby\\s*vision)\\b").containsMatchIn(norm)) add("DV")
            if (Regex("(?i)HDR10\\+|HDR10plus|HDR10P\\b").containsMatchIn(text)) add("HDR10+")
            else if (Regex("(?i)\\bHDR10\\b").containsMatchIn(norm)) add("HDR10")
            else if (Regex("(?i)\\bHDR\\b").containsMatchIn(norm)) add("HDR")
            if (Regex("(?i)\\bHLG\\b").containsMatchIn(norm)) add("HLG")
        }

        val codec = when {
            Regex("(?i)\\b(hevc|x265|h\\s?265)\\b").containsMatchIn(norm) -> "HEVC"
            Regex("(?i)\\bav1\\b").containsMatchIn(norm) -> "AV1"
            Regex("(?i)\\b(x264|h\\s?264|avc)\\b").containsMatchIn(norm) -> "H.264"
            Regex("(?i)\\b(xvid|divx)\\b").containsMatchIn(norm) -> "XviD"
            Regex("(?i)\\bvp9\\b").containsMatchIn(norm) -> "VP9"
            else -> null
        }

        val source = when {
            Regex("(?i)\\bremux\\b").containsMatchIn(norm) -> "REMUX"
            Regex("(?i)\\b(blu-?ray|bdrip|brrip|bdremux|bd)\\b").containsMatchIn(norm) -> "BluRay"
            Regex("(?i)\\bweb-?dl\\b").containsMatchIn(norm) -> "WEB-DL"
            Regex("(?i)\\bweb-?rip\\b").containsMatchIn(norm) -> "WEBRip"
            Regex("(?i)\\bweb\\b").containsMatchIn(norm) -> "WEB"
            Regex("(?i)\\bhdtv\\b").containsMatchIn(norm) -> "HDTV"
            Regex("(?i)\\b(dvdrip|dvd)\\b").containsMatchIn(norm) -> "DVD"
            Regex("(?i)\\b(hdcam|cam|camrip|telesync|hdts|ts)\\b").containsMatchIn(norm) -> "CAM"
            else -> null
        }

        val audio = buildList {
            if (Regex("(?i)\\batmos\\b").containsMatchIn(norm)) add("Atmos")
            if (Regex("(?i)\\btrue-?hd\\b").containsMatchIn(norm)) add("TrueHD")
            when {
                Regex("(?i)DTS[\\s-]?X\\b").containsMatchIn(text) -> add("DTS:X")
                Regex("(?i)DTS[\\s-]?HD").containsMatchIn(text) -> add("DTS-HD")
                Regex("(?i)\\bDTS\\b").containsMatchIn(norm) -> add("DTS")
            }
            when {
                Regex("(?i)(DD\\+|DDP|E-?AC-?3)").containsMatchIn(text) -> add("DD+")
                Regex("(?i)\\b(DD|AC-?3|Dolby\\s*Digital)\\b").containsMatchIn(norm) -> add("DD")
            }
            if (Regex("(?i)\\baac\\b").containsMatchIn(norm)) add("AAC")
            if (Regex("(?i)\\bflac\\b").containsMatchIn(norm)) add("FLAC")
            if (Regex("(?i)\\bopus\\b").containsMatchIn(norm)) add("Opus")
        }

        val channels = Regex("\\b([257])[ .]([01])\\b").find(text)?.let { "${it.groupValues[1]}.${it.groupValues[2]}" }

        val size = sizeHint?.takeIf { it > 0 } ?: SIZE.findAll(text).map { toBytes(it.groupValues[1], it.groupValues[2]) }.maxOrNull()

        val seeders = SEEDERS.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.toIntOrNull() }

        var cached: Boolean? = null
        var debrid: String? = null
        DEBRID_TAG.find(text)?.let { m ->
            val code = m.groupValues[1].uppercase(Locale.US)
            debrid = DEBRID_NAMES[code] ?: code
            val mark = m.groupValues[2]
            cached = when {
                mark == "+" || mark == "⚡" || mark == "✓" -> true
                mark.equals("download", true) || mark == "⏳" -> false
                else -> null
            }
        }
        if (cached == null) {
            if (text.contains("⚡")) cached = true
            else if (text.contains("⏳")) cached = false
        }
        if (debrid == null) {
            debrid = DEBRID_NAMES.values.firstOrNull { text.contains(it, ignoreCase = true) }
        }

        val langs = FLAG.findAll(text).map { it.value }.distinct().toMutableList()
        if (Regex("(?i)\\bmulti\\b").containsMatchIn(norm)) langs.add(0, "Multi")

        return ParsedTags(
            resolution = resolution,
            hdr = hdr,
            codec = codec,
            source = source,
            audio = audio,
            channels = channels,
            sizeBytes = size,
            seeders = seeders,
            cached = cached,
            debrid = debrid,
            languages = langs,
            is3D = Regex("(?i)\\b3D\\b").containsMatchIn(norm),
        )
    }

    private fun toBytes(num: String, unit: String): Long {
        val v = num.replace(',', '.').toDoubleOrNull() ?: return 0
        val mult = when (unit.uppercase(Locale.US)) {
            "TB", "TIB" -> 1_099_511_627_776.0
            "GB", "GIB" -> 1_073_741_824.0
            else -> 1_048_576.0
        }
        return (v * mult).toLong()
    }

    /** Nuvio scrapers usually return a quality string like "1080p" or "4K". */
    fun qualityToResolution(q: String?): Int {
        if (q.isNullOrBlank()) return 0
        return RES.firstOrNull { it.first.containsMatchIn(q) }?.second
            ?: Regex("(\\d{3,4})").find(q)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
