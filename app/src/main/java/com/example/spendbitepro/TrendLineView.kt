package com.example.spendbitepro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class TrendLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataset: List<Double> = listOf(200.0, 500.0, 350.0, 700.0, 400.0, 800.0, 600.0) // Seed defaults

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#000666") // Brand Blue
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        color = Color.parseColor("#E5E7EB") // light gray grid lines
    }

    fun setSpendingData(data: List<Double>) {
        if (data.size >= 2) {
            dataset = data
            invalidate() // Request redraw
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val paddingBottom = dpToPx(8f)
        val paddingTop = dpToPx(16f)
        val chartHeight = h - paddingTop - paddingBottom

        // Draw horizontal grid lines
        val gridRows = 3
        val rowHeight = chartHeight / gridRows
        for (i in 0..gridRows) {
            val y = paddingTop + i * rowHeight
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        val size = dataset.size
        val stepX = w / (size - 1)
        val maxVal = dataset.maxOrNull() ?: 1.0
        val minVal = dataset.minOrNull() ?: 0.0
        val range = if (maxVal - minVal > 0) maxVal - minVal else maxVal

        // Compute points coordinates
        val points = dataset.mapIndexed { index, value ->
            val x = index * stepX
            // Scaled Y coordinate (invert since canvas y increases downwards)
            val ratio = if (range > 0) (value - minVal) / range else 0.5
            val y = paddingTop + chartHeight - (ratio.toFloat() * chartHeight)
            x to y
        }

        // Draw curved line path
        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(points[0].first, points[0].second)
            fillPath.moveTo(points[0].first, points[0].second)

            for (i in 1 until points.size) {
                val (xPrev, yPrev) = points[i - 1]
                val (xCurr, yCurr) = points[i]

                // Cubic spline control points
                val conX1 = xPrev + (xCurr - xPrev) / 2f
                val conY1 = yPrev
                val conX2 = xPrev + (xCurr - xPrev) / 2f
                val conY2 = yCurr

                path.cubicTo(conX1, conY1, conX2, conY2, xCurr, yCurr)
                fillPath.cubicTo(conX1, conY1, conX2, conY2, xCurr, yCurr)
            }

            // Close the fill path to the bottom edges
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()

            // Setup LinearGradient Shader for area fill
            fillPaint.shader = LinearGradient(
                0f, paddingTop, 0f, h,
                Color.parseColor("#33000666"), // 20% alpha brand blue
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )

            // Draw paths
            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(path, linePaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
