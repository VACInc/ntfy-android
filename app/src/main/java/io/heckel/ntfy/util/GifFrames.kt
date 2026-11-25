package io.heckel.ntfy.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.net.Uri

/**
 * Animated GIF attachments in notifications (VAC fork).
 *
 * Android notifications cannot animate a picture, so like the Home Assistant app does for video,
 * the GIF is cut into still frames that a ViewFlipper in a custom notification view flips through.
 */
object GifFrames {
    /** Frame spacing in GIF time; must match android:flipInterval in notification_gif_flipper.xml. */
    const val STEP_MS = 200
    const val MAX_FRAMES = 30
    const val MAX_WIDTH = 480
    /** Total frame memory kept under the notification bitmap budget (HA uses the same 5 MB cap). */
    const val MAX_TOTAL_BYTES = 5_000_000

    /** GIF times (ms) to sample: every STEP_MS, spread wider when the GIF is longer than MAX_FRAMES steps. */
    fun frameTimes(durationMs: Int, maxFrames: Int = MAX_FRAMES, stepMs: Int = STEP_MS): List<Int> {
        if (durationMs <= 0 || maxFrames < 2 || stepMs <= 0) return emptyList()
        val count = minOf(maxFrames, (durationMs + stepMs - 1) / stepMs)
        if (count < 2) return emptyList()
        val step = maxOf(stepMs, durationMs / count)
        // Sample mid-frame so a GIF made at exactly stepMs per frame yields each frame once, never a boundary repeat.
        return List(count) { minOf(it * step + stepMs / 2, durationMs - 1) }
    }

    /** Scaled (width, height) for a frame, never wider than maxWidth, aspect ratio kept. */
    fun scaledSize(width: Int, height: Int, maxWidth: Int = MAX_WIDTH): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return Pair(0, 0)
        if (width <= maxWidth) return Pair(width, height)
        return Pair(maxWidth, maxOf(1, (height.toLong() * maxWidth / width).toInt()))
    }

    /** How many frames of this size fit in the budget (RGB_565 = 2 bytes per pixel). */
    fun framesWithinBudget(width: Int, height: Int, wanted: Int, budget: Int = MAX_TOTAL_BYTES): Int {
        val perFrame = width.toLong() * height * 2
        if (perFrame <= 0) return 0
        return minOf(wanted.toLong(), budget / perFrame).toInt()
    }

    /** Frames of an animated GIF, or null when it is not animated or cannot be decoded. */
    @Suppress("DEPRECATION") // Movie is the only platform API that exposes individual GIF frames.
    fun decode(context: Context, uri: Uri): List<Bitmap>? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val times = frameTimes(movie.duration())
            val (w, h) = scaledSize(movie.width(), movie.height())
            val n = framesWithinBudget(w, h, times.size)
            if (n < 2) return null
            val scale = w.toFloat() / movie.width()
            times.take(n).map { t ->
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                canvas.scale(scale, scale)
                movie.setTime(t)
                movie.draw(canvas, 0f, 0f)
                bitmap
            }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }
}
