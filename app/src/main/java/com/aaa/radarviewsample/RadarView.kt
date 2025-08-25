package com.aaa.radarviewsample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val radarPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val sweepPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        isAntiAlias = true
    }

    private var sweepAngle = 0f
    private val sweepTrail = mutableListOf<Float>() /* スイープの軌跡 */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) - 16f

        /* 同心円を描画 */
        val ringCount = 5
        for (i in 1..ringCount) {
            canvas.drawCircle(centerX, centerY, radius * i / ringCount, radarPaint)
        }

        /* グリッド線(縦横) */
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, radarPaint)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, radarPaint)

        /* スイープライン(回転) */
        val sweepLength = radius
        val rad = Math.toRadians(sweepAngle.toDouble())
        val endX = centerX + sweepLength * cos(rad).toFloat()
        val endY = centerY + sweepLength * sin(rad).toFloat()
        canvas.drawLine(centerX, centerY, endX, endY, sweepPaint)

        /* スイープラインの尾を描画 */
        val maxTrail = 30   /* 尾の長さ */
        sweepTrail.add(0, sweepAngle-1) /* 新しい角度を先頭に追加 */
        sweepTrail.add(0, sweepAngle)   /* 新しい角度を先頭に追加 */
        while (sweepTrail.size > maxTrail)
            sweepTrail.removeAt(sweepTrail.size-1)

        for((idx, angle) in sweepTrail.withIndex()) {
            val alpha = ((255f * (1f - idx/maxTrail.toFloat()))).toInt()
            sweepPaint.alpha = alpha
            val rad = Math.toRadians(angle.toDouble())
            val endx= centerX + radius * cos(rad).toFloat()
            val endy= centerY + radius * sin(rad).toFloat()
            canvas.drawLine(centerX, centerY, endx, endy, sweepPaint)
        }

        /* アニメーション更新 */
        sweepAngle += 2f
        if (sweepAngle >= 360f) sweepAngle = 0f
        postInvalidateDelayed(16L) // 約60fps
    }
}
