package com.waterman.packai.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object KSBMotorCropHelper {

    /**
     * Detects and crops the dark top cap (black cast iron part) from KSB motor pump images.
     *
     * Strategy:
     * 1. Scan rows top-to-bottom to find where a SIGNIFICANT dark band starts
     * 2. Track the dark band's end (where it transitions back to silver/bright body)
     * 3. Crop from dark band start → end (+ small padding)
     * 4. Validate: dark region must be wide enough and tall enough to be the real cap
     *
     * @return Cropped Bitmap of dark cap, or null if not a valid motor image
     */
    fun cropDarkTopCap(src: Bitmap): Bitmap? {
        val width = src.width
        val height = src.height

        if (width < 100 || height < 100) return null

        // How many pixels to sample per row (spread evenly)
        val sampleCount = 30
        val step = maxOf(1, width / sampleCount)

        // Darkness threshold: pixel is "dark" if all RGB channels below this
        val darkThreshold = 80

        // Minimum fraction of sampled pixels that must be dark to call a row "dark"
        val darkRowRatio = 0.35f

        // --- STEP 1: Find first dark row (cap starts) ---
        var capStartY = -1
        val searchLimit = (height * 0.6f).toInt() // Only look in top 60%

        for (y in 0 until searchLimit) {
            val darkCount = countDarkPixelsInRow(src, y, step, darkThreshold)
            val ratio = darkCount.toFloat() / sampleCount
            if (ratio >= darkRowRatio) {
                capStartY = y
                break
            }
        }

        if (capStartY == -1) return null // No dark cap found

        // --- STEP 2: Find where dark cap ENDS (transition to bright silver body) ---
        var capEndY = capStartY
        var brightRowCount = 0
        val brightThreshold = 130 // Silver body is much brighter
        val brightRowRatio = 0.55f
        val consecutiveBrightRowsNeeded = 6 // Need several bright rows to confirm end of cap

        for (y in (capStartY + 1) until searchLimit) {
            val darkCount = countDarkPixelsInRow(src, y, step, darkThreshold)
            val ratio = darkCount.toFloat() / sampleCount

            if (ratio < darkRowRatio) {
                // Check if this is genuinely bright (silver body)
                val brightCount = countBrightPixelsInRow(src, y, step, brightThreshold)
                val brightRatio = brightCount.toFloat() / sampleCount
                if (brightRatio >= brightRowRatio) {
                    brightRowCount++
                    if (brightRowCount >= consecutiveBrightRowsNeeded) {
                        capEndY = y - consecutiveBrightRowsNeeded
                        break
                    }
                }
            } else {
                brightRowCount = 0 // Reset — still in dark zone
                capEndY = y
            }
        }

        // --- STEP 3: Validate the dark cap dimensions ---
        val capHeight = capEndY - capStartY
        val minCapHeight = height * 0.05f  // Cap must be at least 5% of image height
        val maxCapHeight = height * 0.45f  // Cap shouldn't be more than 45% of image

        if (capHeight < minCapHeight || capHeight > maxCapHeight) return null

        // Also validate: the dark region must span most of the width (real cap is wide)
        val midY = (capStartY + capEndY) / 2
        val darkAtMid = countDarkPixelsInRow(src, midY, step, darkThreshold)
        val widthCoverage = darkAtMid.toFloat() / sampleCount
        if (widthCoverage < 0.30f) return null // Cap too narrow — probably not real cap

        // --- STEP 4: Crop with small vertical padding ---
        val paddingTop = (capHeight * 0.05f).toInt()
        val paddingBottom = (capHeight * 0.15f).toInt() // Extra padding below to include transition

        val cropTop = maxOf(0, capStartY - paddingTop)
        val cropBottom = minOf(height, capEndY + paddingBottom)
        val cropHeight = cropBottom - cropTop

        if (cropHeight <= 0) return null

        return Bitmap.createBitmap(src, 0, cropTop, width, cropHeight)
    }

    /**
     * Count how many sampled pixels in a row are "dark"
     */
    private fun countDarkPixelsInRow(
        bitmap: Bitmap,
        y: Int,
        step: Int,
        threshold: Int
    ): Int {
        var count = 0
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // All channels must be dark (avoids counting colored regions)
            if (r < threshold && g < threshold && b < threshold) {
                count++
            }
            x += step
        }
        return count
    }

    /**
     * Count how many sampled pixels in a row are "bright" (silver/metallic body)
     */
    private fun countBrightPixelsInRow(
        bitmap: Bitmap,
        y: Int,
        step: Int,
        threshold: Int
    ): Int {
        var count = 0
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val brightness = (r + g + b) / 3
            if (brightness > threshold) {
                count++
            }
            x += step
        }
        return count
    }

    /**
     * Quick pre-check: does this image look like it could be a motor pump photo?
     * Checks if image has both very dark AND bright/silver regions.
     */
    fun isLikelyMotorImage(src: Bitmap): Boolean {
        val width = src.width
        val height = src.height
        val sampleStep = maxOf(1, width / 20)

        var darkPixelCount = 0
        var brightPixelCount = 0
        val totalSampled = (height / 10) * (width / sampleStep)

        for (y in 0 until height step 10) {
            for (x in 0 until width step sampleStep) {
                val pixel = src.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (r + g + b) / 3
                if (brightness < 70) darkPixelCount++
                if (brightness > 150) brightPixelCount++
            }
        }

        val darkRatio = darkPixelCount.toFloat() / totalSampled
        val brightRatio = brightPixelCount.toFloat() / totalSampled

        // Must have some dark (cap) AND some bright (silver body)
        return darkRatio > 0.03f && brightRatio > 0.10f
    }
}