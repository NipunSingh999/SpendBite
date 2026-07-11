package com.example.spendbitepro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: Map<String, Double> = emptyMap()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    // Color palette matching mockup colors
    private val categoryColors = mapOf(
        "meals" to Color.parseColor("#000666"),          // Brand primary blue
        "dining" to Color.parseColor("#000666"),
        "groceries" to Color.parseColor("#1B6D24"),      // Brand secondary green
        "rent" to Color.parseColor("#6366F1"),           // Indigo/Purple
        "utilities" to Color.parseColor("#F59E0B"),      // Amber/Orange
        "discretionary" to Color.parseColor("#EC4899"),  // Pink
        "others" to Color.parseColor("#9CA3AF")          // Grey
    )

    private val fallbackColors = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6", "#3B82F6")

    fun setData(newData: Map<String, Double>) {
        data = newData.filter { it.value > 0 }
        invalidate() // Redraw on update
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val size = Math.min(width, height)
        
        if (size <= 0 || data.isEmpty()) {
            // Draw empty placeholder ring
            paint.color = Color.parseColor("#F3F4F6")
            paint.strokeWidth = dpToPx(14f)
            val margin = dpToPx(10f)
            val rect = RectF(margin, margin, size - margin, size - margin)
            canvas.drawArc(rect, 0f, 360f, false, paint)
            return
        }

        val strokeWidth = dpToPx(14f)
        paint.strokeWidth = strokeWidth
        
        val margin = strokeWidth / 2f + dpToPx(4f)
        val rect = RectF(margin, margin, size - margin, size - margin)
        
        val total = data.values.sum()
        if (total <= 0.0) return

        var startAngle = -90f // Start from the top!
        
        data.entries.forEachIndexed { index, entry ->
            val category = entry.key.lowercase()
            val value = entry.value
            val sweepAngle = ((value / total) * 360f).toFloat()

            // Resolve color
            val colorStr = categoryColors[category] ?: Color.parseColor(fallbackColors[index % fallbackColors.size])
            paint.color = colorStr

            // Draw segment arc
            canvas.drawArc(rect, startAngle, sweepAngle, false, paint)

            // Draw thin white gap between segments
            if (data.size > 1) {
                paint.color = Color.WHITE
                paint.strokeWidth = dpToPx(2f)
                
                val rad = Math.toRadians(startAngle.toDouble())
                val cx = size / 2f
                val cy = size / 2f
                val r = (size - 2 * margin) / 2f
                
                val x1 = (cx + (r - strokeWidth / 2) * Math.cos(rad)).toFloat()
                val y1 = (cy + (r - strokeWidth / 2) * Math.sin(rad)).toFloat()
                val x2 = (cx + (r + strokeWidth / 2) * Math.cos(rad)).toFloat()
                val y2 = (cy + (r + strokeWidth / 2) * Math.sin(rad)).toFloat()
                
                canvas.drawLine(x1, y1, x2, y2, paint)
                
                paint.strokeWidth = strokeWidth
            }

            startAngle += sweepAngle
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
