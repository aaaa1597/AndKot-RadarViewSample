package com.aaa.radarviewsample

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var D_SWEEP_RANGE: Float = 0f
    private var D_START_ANGLE = 0f

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.RadarView) {
                D_SWEEP_RANGE = getFloat(R.styleable.RadarView_sweepRange, 360f)
                D_START_ANGLE = -D_SWEEP_RANGE/2f
            }
        }
    }

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

    private val glowPaint = Paint().apply {
        color = Color.CYAN
        isAntiAlias = true
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)    /* 光のにじみ */
    }

    private val centerPaint = Paint().apply {
        color = Color.CYAN
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        color = Color.CYAN
        isAntiAlias = true
    }

    private var sweepAngle = 0f
    private val sweepTrail = mutableListOf<Float>() /* スイープの軌跡 */
    private var sweepDirection = 1 /* 1:時計回り -1:反時計回り */

    /* レーダー内の点(極座標形式) */
    private val radarPoints = listOf(
        Pair(100f, 0f),    /* 半径, 角度(°) */
        Pair(100f, 30f),
        Pair(150f, 120f),
        Pair( 80f, 270f),
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) - 16f

        /* 同心円弧を描画 */
        val ringCount = 5
        for (i in 1..ringCount) {
            canvas.drawCircle(centerX, centerY, radius * i / ringCount, radarPaint)
        }

        /* グリッド線(扇形の両端) */
        val radStart = Math.toRadians(D_START_ANGLE.toDouble())
        val radEnd = Math.toRadians((D_START_ANGLE+D_SWEEP_RANGE).toDouble())
        val x1 = centerX + radius * cos(radStart).toFloat()
        val y1 = centerY + radius * sin(radStart).toFloat()
        val x2 = centerX + radius * cos(radEnd).toFloat()
        val y2 = centerY + radius * sin(radEnd).toFloat()
        canvas.drawLine(centerX, centerY, x1, y1, radarPaint)
        canvas.drawLine(centerX, centerY, x2, y2, radarPaint)

        /* スイープライン */
        val sweepLength = radius
        val rad = Math.toRadians(D_START_ANGLE+sweepAngle.toDouble())
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
            val radTrail = Math.toRadians((D_START_ANGLE+angle).toDouble())
            val trailX= centerX + radius * cos(radTrail).toFloat()
            val trailY= centerY + radius * sin(radTrail).toFloat()
            canvas.drawLine(centerX, centerY, trailX, trailY, sweepPaint)
        }

        /* 中心点描画 */
        canvas.drawCircle(centerX, centerY, 20f, glowPaint)
        canvas.drawCircle(centerX, centerY, 12f, centerPaint)

        /* 点描画 */
        for((r, angle) in radarPoints) {
            val relativeAngle = angle-D_START_ANGLE
            if(relativeAngle in 0f .. D_SWEEP_RANGE) {
                val radPoint = Math.toRadians(angle.toDouble())
                val x = centerX + r * cos(radPoint).toFloat()
                val y = centerY + r * sin(radPoint).toFloat()
                val diff = abs((D_START_ANGLE+sweepAngle)-angle)
                val isHit = diff < 5f || diff > 355f    /* 角度が近い時にヒットしたと判定 */
                if(isHit) {
                    /*光らせる*/
                    canvas.drawCircle(x, y, 16f, glowPaint)
                } else {
                    canvas.drawCircle(x, y, 8f, pointPaint)
                }
            }
        }

        /* アニメーション更新 */
        sweepAngle += 2f * sweepDirection
        if (sweepAngle >= D_SWEEP_RANGE) sweepAngle = 0f
        postInvalidateDelayed(16L) // 約60fps
    }
}
