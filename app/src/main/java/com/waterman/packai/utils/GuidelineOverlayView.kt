package com.waterman.packai.utils   // <-- MUST match your real package

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GuidelineOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }

    private var message = "Detecting..."

    fun updateState(state: DistanceState) {
        when (state) {
            DistanceState.TOO_FAR -> {
                paint.color = Color.RED
                message = "Move Closer"
            }
            DistanceState.PERFECT -> {
                paint.color = Color.GREEN
                message = "Perfect Distance"
            }
            DistanceState.TOO_CLOSE -> {
                paint.color = Color.YELLOW
                message = "Move Away"
            }
            DistanceState.NO_OBJECT -> {
                paint.color = Color.GRAY
                message = "No Object"
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val boxWidth = width * 0.7f
        val boxHeight = height * 0.4f

        val left = (width - boxWidth) / 2
        val top = (height - boxHeight) / 2

        canvas.drawRect(
            left,
            top,
            left + boxWidth,
            top + boxHeight,
            paint
        )

        canvas.drawText(
            message,
            width / 2f,
            top - 30f,
            textPaint
        )
    }
}
