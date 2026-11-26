package io.heckel.ntfy.util

/**
 * Home Assistant companion-app notification options carried through ntfy as hidden tags (VAC fork).
 *
 * The ntfy server stores tags untouched, so HA's notify bridge sends options the stock ntfy message has
 * no field for as tags starting with "ha-". They are never shown as tags; this parses them.
 *   ha-sticky              tapping does not dismiss (HA sticky)
 *   ha-persistent          cannot be swiped away (HA persistent, ongoing)
 *   ha-alert_once          an update to a showing notification stays silent (HA alert_once)
 *   ha-chan:<name>         HA notification channel: its own Android channel, editable in system settings
 *   ha-imp:<min|low|default|high|max>   importance for that channel
 *   ha-vib:<a.b.c>         vibration pattern in ms (HA vibrationPattern), for that channel
 *   ha-led:<rrggbb>        LED color for that channel (HA ledColor)
 *   ha-timeout:<s>         dismiss after s seconds (HA timeout)
 *   ha-vis:<public|private|secret>  lock-screen visibility (HA visibility)
 *   ha-group:<name>        notification group (HA group)
 *   ha-when:<epoch s>      timestamp shown (HA when)
 *   ha-chrono / ha-countdown   show a running timer from/to "when" (HA chronometer)
 *   ha-progress:<n>/<max> or ha-progress:indeterminate   progress bar (HA progress)
 */
data class HaHints(
    val sticky: Boolean = false,
    val persistent: Boolean = false,
    val alertOnce: Boolean = false,
    val channel: String? = null,
    val importance: String? = null,
    val vibration: LongArray? = null,
    val led: String? = null,
    val timeoutSeconds: Long? = null,
    val visibility: String? = null,
    val group: String? = null,
    val whenSeconds: Long? = null,
    val chronometer: Boolean = false,
    val countdown: Boolean = false,
    val progress: Int? = null,
    val progressMax: Int? = null,
    val progressIndeterminate: Boolean = false,
) {
    /** Android channel id for the HA channel: stable per name, and per vibration/importance (channels are immutable). */
    fun channelId(): String? {
        val name = channel ?: return null
        val slug = name.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifEmpty { "default" }
        val vib = vibration?.joinToString(".") ?: ""
        return "ha_" + slug + "_" + Integer.toHexString((importance + "|" + vib + "|" + (led ?: "")).hashCode())
    }

    companion object {
        const val PREFIX = "ha-"
        private val COLOR = Regex("^[0-9a-fA-F]{6}$")
        private val IMPORTANCES = setOf("min", "low", "default", "high", "max")
        private val VISIBILITIES = setOf("public", "private", "secret")

        fun isHint(tag: String): Boolean = tag.startsWith(PREFIX)

        fun parse(tags: List<String>): HaHints {
            var h = HaHints()
            tags.map { it.trim() }.filter { isHint(it) }.forEach { tag ->
                val body = tag.removePrefix(PREFIX)
                val key = body.substringBefore(':')
                val value = if (body.contains(':')) body.substringAfter(':') else ""
                h = when (key) {
                    "sticky" -> h.copy(sticky = true)
                    "persistent" -> h.copy(persistent = true)
                    "alert_once" -> h.copy(alertOnce = true)
                    "chrono" -> h.copy(chronometer = true)
                    "countdown" -> h.copy(chronometer = true, countdown = true)
                    "chan" -> if (value.isNotBlank()) h.copy(channel = value.take(80)) else h
                    "imp" -> if (value.lowercase() in IMPORTANCES) h.copy(importance = value.lowercase()) else h
                    "vib" -> h.copy(vibration = parseVibration(value))
                    "led" -> if (COLOR.matches(value)) h.copy(led = "#" + value.lowercase()) else h
                    "timeout" -> value.toLongOrNull()?.takeIf { it > 0 }?.let { h.copy(timeoutSeconds = it) } ?: h
                    "vis" -> if (value.lowercase() in VISIBILITIES) h.copy(visibility = value.lowercase()) else h
                    "group" -> if (value.isNotBlank()) h.copy(group = value.take(80)) else h
                    "when" -> value.toLongOrNull()?.takeIf { it > 0 }?.let { h.copy(whenSeconds = it) } ?: h
                    "progress" -> parseProgress(h, value)
                    else -> h
                }
            }
            return h
        }

        fun parseVibration(value: String): LongArray? {
            val parts = value.split('.').map { it.trim().toLongOrNull() ?: return null }
            if (parts.isEmpty() || parts.size > 40 || parts.any { it < 0 || it > 60_000 }) return null
            return parts.toLongArray()
        }

        private fun parseProgress(h: HaHints, value: String): HaHints {
            if (value == "indeterminate") return h.copy(progressIndeterminate = true)
            val n = value.substringBefore('/').toIntOrNull() ?: return h
            val max = if (value.contains('/')) value.substringAfter('/').toIntOrNull() ?: return h else 100
            if (max <= 0 || n < 0) return h
            return h.copy(progress = minOf(n, max), progressMax = max)
        }
    }
}
