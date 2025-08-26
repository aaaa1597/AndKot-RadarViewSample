package com.aaa.radarviewsample

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var D_SWEEP_RANGE: Float = 0f   /* Sweep angle of the radar arc */
    private var D_START_ANGLE = 0f  /* Starting angle of the radar arc */
    var D_IS_SWEEP_WRAPPED = false  /* Whether the radar line wraps around */

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.RadarView) {
                D_SWEEP_RANGE = getFloat(R.styleable.RadarView_sweepRange, 360f)
                D_START_ANGLE = -D_SWEEP_RANGE/2f
                D_IS_SWEEP_WRAPPED = getBoolean(R.styleable.RadarView_isSweepWrapped, false)
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
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)    /* Glow effect */
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
    private val sweepTrail = mutableListOf<Float>() /* - Sweep trail */
    private var sweepDirection = 1 /* 1: Clockwise, -1: Counterclockwise */

    /* Points inside the radar (in polar coordinates) */
    private val radarPoints = listOf(
        Pair(100f, 0f),    /* Radius, Angle(°) */
        Pair(100f, 30f),
        Pair(150f, 120f),
        Pair( 80f, 270f),
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) - 16f

        /* Draw concentric arcs */
        val ringCount = 5
        for (i in 1..ringCount) {
            canvas.drawCircle(centerX, centerY, radius * i / ringCount, radarPaint)
        }

        /* Grid lines (edges of the sector) */
        val radStart = Math.toRadians(D_START_ANGLE.toDouble())
        val radEnd = Math.toRadians((D_START_ANGLE+D_SWEEP_RANGE).toDouble())
        val x1 = centerX + radius * cos(radStart).toFloat()
        val y1 = centerY + radius * sin(radStart).toFloat()
        val x2 = centerX + radius * cos(radEnd).toFloat()
        val y2 = centerY + radius * sin(radEnd).toFloat()
        canvas.drawLine(centerX, centerY, x1, y1, radarPaint)
        canvas.drawLine(centerX, centerY, x2, y2, radarPaint)

        /* Sweep line */
        val sweepLength = radius
        val rad = Math.toRadians(D_START_ANGLE+sweepAngle.toDouble())
        val endX = centerX + sweepLength * cos(rad).toFloat()
        val endY = centerY + sweepLength * sin(rad).toFloat()
        canvas.drawLine(centerX, centerY, endX, endY, sweepPaint)

        /* Draw sweep trail */
        val maxTrail = 30   /* Trail length */
        sweepTrail.add(0, sweepAngle-1) /* Add new angle to the front */
        sweepTrail.add(0, sweepAngle)   /* Add new angle to the front */
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

        /* Draw center point */
        canvas.drawCircle(centerX, centerY, 20f, glowPaint)
        canvas.drawCircle(centerX, centerY, 12f, centerPaint)

        /* Draw radar points */
        for((r, angle) in radarPoints) {
            val relativeAngle = angle-D_START_ANGLE
            if(relativeAngle in 0f .. D_SWEEP_RANGE) {
                val radPoint = Math.toRadians(angle.toDouble())
                val x = centerX + r * cos(radPoint).toFloat()
                val y = centerY + r * sin(radPoint).toFloat()
                val diff = abs((D_START_ANGLE+sweepAngle)-angle)
                val isHit = diff < 5f || diff > 355f    /*  Considered a hit if angle is close */
                if(isHit) {
                    /* Glow effect */
                    canvas.drawCircle(x, y, 16f, glowPaint)
                } else {
                    canvas.drawCircle(x, y, 8f, pointPaint)
                }
            }
        }

        /* Update animation */
        sweepAngle += 2f * sweepDirection
        if(D_IS_SWEEP_WRAPPED) {
            if (sweepAngle >= D_SWEEP_RANGE) {
                sweepAngle = D_SWEEP_RANGE
                sweepDirection = -1
            }
            else if (sweepAngle <= 0f) {
                sweepAngle = 0f
                sweepDirection = 1
            }
        }
        else {
            if (sweepAngle >= D_SWEEP_RANGE) sweepAngle = 0f
        }
        postInvalidateDelayed(16L) // 約60fps
    }
}
