package com.example.spendbitepro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class MonthlyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Pair of Month Label to Total Spending amount
    private var dataset: List<Pair<String, Double>> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#767683") // Zinc 500
        textSize = dpToPx(10.5f)
        textAlign = Paint.Align.CENTER
    }

    private val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1C1C") // Zinc 900
        textSize = dpToPx(11f)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        isFakeBoldText = true
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        color = Color.parseColor("#E5E7EB") // light gray grid lines
    }

    fun setChartData(data: List<Pair<String, Double>>) {
        dataset = data
        invalidate() // Request redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0 || dataset.isEmpty()) return

        val paddingBottom = dpToPx(28f)
        val paddingTop = dpToPx(24f)
        val paddingLeftRight = dpToPx(16f)
        val chartHeight = h - paddingTop - paddingBottom
        val chartWidth = w - (paddingLeftRight * 2)

        // Draw horizontal grid lines
        val gridRows = 3
        val rowHeight = chartHeight / gridRows
        for (i in 0..gridRows) {
            val y = paddingTop + i * rowHeight
            canvas.drawLine(paddingLeftRight, y, w - paddingLeftRight, y, gridPaint)
        }

        val size = dataset.size
        val stepX = chartWidth / size
        val maxVal = dataset.map { it.second }.maxOrNull() ?: 1.0
        val baseMax = if (maxVal > 0.0) maxVal else 1.0

        val barWidth = dpToPx(18f)

        dataset.forEachIndexed { index, (label, amount) ->
            // Center each bar in its step column
            val centerX = paddingLeftRight + (index * stepX) + (stepX / 2f)
            val left = centerX - (barWidth / 2f)
            val right = centerX + (barWidth / 2f)

            // Scaled height ratio
            val ratio = amount / baseMax
            val top = paddingTop + chartHeight - (ratio.toFloat() * chartHeight)
            val bottom = paddingTop + chartHeight

            // Set dynamic gradient shading for each bar (emerald green to brand blue)
            barPaint.shader = LinearGradient(
                centerX, top, centerX, bottom,
                Color.parseColor("#1B6D24"), // Brand green
                Color.parseColor("#000666"), // Brand blue
                Shader.TileMode.CLAMP
            )

            // Draw rounded bar (top corners rounded, radius = 6dp)
            val rect = RectF(left, top, right, bottom)
            val radius = dpToPx(6f)
            canvas.drawRoundRect(rect, radius, radius, barPaint)

            // Draw Month label below the bar
            canvas.drawText(label, centerX, bottom + dpToPx(16f), textPaint)

            // Draw value label above the bar (e.g. ₹4.2K)
            val amountStr = if (amount >= 1000) {
                String.format("₹%.1fK", amount / 1000.0)
            } else {
                String.format("₹%.0f", amount)
            }
            canvas.drawText(amountStr, centerX, top - dpToPx(6f), boldTextPaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
