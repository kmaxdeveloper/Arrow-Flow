package uz.kmax.arrowflow.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class AmbientBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dots = mutableListOf<MovingDot>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val dotColors = listOf(
        Color.parseColor("#E2E8F0"), // Slate 200
        Color.parseColor("#E0F2FE"), // Blue 100
        Color.parseColor("#F1F5F9"), // Slate 100
        Color.parseColor("#F5F3FF")  // Purple 100
    )

    private var animator: ValueAnimator? = null

    private data class MovingDot(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var size: Float,
        var baseAlpha: Int,
        val color: Int
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        dots.clear()
        repeat(25) { // Increased count
            dots.add(MovingDot(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h,
                vx = (Random.nextFloat() - 0.5f) * 1.2f,
                vy = (Random.nextFloat() - 0.5f) * 1.2f,
                size = Random.nextFloat() * 60f + 20f, // Larger dots
                baseAlpha = Random.nextInt(100, 180), // HIGHLY visible
                color = dotColors.random()
            ))
        }
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateDots()
                invalidate()
            }
            start()
        }
    }

    private fun updateDots() {
        if (width == 0 || height == 0) return
        dots.forEach { dot ->
            dot.x += dot.vx
            dot.y += dot.vy
            
            if (dot.x < -dot.size) dot.x = width + dot.size
            if (dot.x > width + dot.size) dot.x = -dot.size
            if (dot.y < -dot.size) dot.y = height + dot.size
            if (dot.y > height + dot.size) dot.y = -dot.size
        }
    }

    override fun onDraw(canvas: Canvas) {
        dots.forEach { dot ->
            paint.color = dot.color
            paint.alpha = dot.baseAlpha
            canvas.drawCircle(dot.x, dot.y, dot.size, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
