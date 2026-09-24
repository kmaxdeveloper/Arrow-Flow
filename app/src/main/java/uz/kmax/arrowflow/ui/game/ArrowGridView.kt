package uz.kmax.arrowflow.ui.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import uz.kmax.arrowflow.R
import uz.kmax.arrowflow.logic.GameEngine
import uz.kmax.arrowflow.logic.GameState
import uz.kmax.arrowflow.model.Arrow
import uz.kmax.arrowflow.model.Board
import uz.kmax.arrowflow.model.Direction
import uz.kmax.arrowflow.model.Level
import kotlin.math.min
import kotlin.random.Random

class ArrowGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), GameEngine.GameEngineListener {

    private var board: Board? = null
    private var gameEngine: GameEngine? = null
    private var cellSize: Float = 0f
    private var boardPadding: Float = 0f
    private var pulseAlpha: Float = 0f
    private var pulseColor: Int = 0
    private var dotGridAlpha: Float = 1f

    private data class ArrowVisual(
        val spinePath: Path,
        val originalLength: Float,
        val pathMeasure: PathMeasure
    )
    private val visualCache = mutableMapOf<Int, ArrowVisual>()

    private data class ExitState(var offset: Float = 0f, var alpha: Float = 1f)
    private val animatingArrows = mutableMapOf<Arrow, ExitState>()
    private val blockedArrows = mutableMapOf<Arrow, Float>()
    private val activeAnimators = mutableListOf<Animator>()

    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var size: Float, var alpha: Float,
        val color: Int
    )
    private val particles = mutableListOf<Particle>()
    private var hintArrowId: Int? = null

    private val fastOutSlowIn = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val boardBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        style = Paint.Style.FILL
    }

    private val arrowBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val arrowHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 25
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val shadowHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 25
        style = Paint.Style.FILL
    }

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_gold)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Reuse objects
    private val bodySegmentPath = Path()
    private val headDrawPath = Path()
    private val headBaseShape = Path()
    private val posBuffer = FloatArray(2)
    private val tanBuffer = FloatArray(2)
    private val matrixBuffer = Matrix()

    fun setEngine(engine: GameEngine) {
        this.gameEngine = engine
    }

    fun setHintArrowId(id: Int?) {
        this.hintArrowId = id
        invalidate()
    }

    fun setUndoEnabled(enabled: Boolean) {}

    override fun onLevelLoaded(level: Level, board: Board) {
        this.board = board
        dotGridAlpha = 1f
        cancelAllAnimations()
        calculateDimensions()
        clearVisualCache()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllAnimations()
    }

    override fun onLivesChanged(lives: Int) {}

    override fun onGameStateChanged(state: GameState, stars: Int) {
        if (state == GameState.LEVEL_COMPLETE) {
            startLevelCompleteAnimation()
        } else if (state == GameState.GAME_OVER) {
            startPulse(ContextCompat.getColor(context, R.color.brand_red))
        }
    }

    private fun startLevelCompleteAnimation() {
        spawnConfetti()
        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = 800
        animator.addUpdateListener { animation ->
            dotGridAlpha = animation.animatedValue as Float
            invalidate()
        }
        animator.start()
        activeAnimators.add(animator)
    }

    private fun spawnConfetti() {
        val colors = listOf(Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GREEN, Color.RED)
        repeat(50) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val speed = Random.nextFloat() * 20f + 10f
            particles.add(Particle(
                x = width / 2f, y = height / 2f,
                vx = Math.cos(angle).toFloat() * speed,
                vy = Math.sin(angle).toFloat() * speed,
                size = Random.nextFloat() * 10f + 5f,
                alpha = 1f,
                color = colors.random()
            ))
        }
        startParticleAnimation()
    }

    private fun startPulse(color: Int) {
        pulseColor = color
        val animator = ValueAnimator.ofFloat(0f, 0.4f, 0f)
        animator.duration = 600
        animator.addUpdateListener { animation ->
            pulseAlpha = animation.animatedValue as Float
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                pulseAlpha = 0f
                invalidate()
            }
        })
        animator.start()
    }

    private fun cancelAllAnimations() {
        activeAnimators.forEach { it.cancel() }
        activeAnimators.clear()
        animatingArrows.clear()
        blockedArrows.clear()
        particles.clear()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
        clearVisualCache()
    }

    private fun calculateDimensions() {
        val fullSize = min(width, height).toFloat()
        boardPadding = fullSize * 0.05f
        val boardSize = fullSize - (boardPadding * 2)
        val currentGridSize = board?.size ?: 5
        cellSize = boardSize / currentGridSize

        val strokeWidth = cellSize * 0.10f
        arrowBodyPaint.strokeWidth = strokeWidth
        shadowPaint.strokeWidth = strokeWidth
        hintPaint.strokeWidth = strokeWidth + 4f

        val cornerFactor = if (currentGridSize <= 5) 0.25f else if (currentGridSize <= 8) 0.20f else 0.15f
        val cornerRadius = cellSize * cornerFactor
        
        arrowBodyPaint.pathEffect = CornerPathEffect(cornerRadius)
        shadowPaint.pathEffect = CornerPathEffect(cornerRadius)
        hintPaint.pathEffect = CornerPathEffect(cornerRadius)

        val hLength = cellSize * 0.52f
        val hWidth = cellSize * 0.44f
        val neckNotch = cellSize * 0.08f
        
        headBaseShape.reset()
        headBaseShape.moveTo(hLength, 0f)
        headBaseShape.lineTo(-neckNotch, -hWidth / 2f)
        headBaseShape.lineTo(0f, 0f)
        headBaseShape.lineTo(-neckNotch, hWidth / 2f)
        headBaseShape.close()
    }

    private fun clearVisualCache() {
        visualCache.clear()
    }

    private fun getOrBuildVisual(arrow: Arrow): ArrowVisual {
        return visualCache.getOrPut(arrow.id) {
            val spine = Path()
            if (arrow.points.size >= 2) {
                val p0 = arrow.points[0]
                val p1 = arrow.points[1]
                val tailDr = p0.first - p1.first
                val tailDc = p0.second - p1.second
                val tailExtend = cellSize * 0.28f

                val startX = (p0.second * cellSize + cellSize / 2) + tailDc * tailExtend
                val startY = (p0.first * cellSize + cellSize / 2) + tailDr * tailExtend

                spine.moveTo(startX, startY)
                spine.lineTo(
                    p0.second * cellSize + cellSize / 2,
                    p0.first * cellSize + cellSize / 2
                )
                for (i in 1 until arrow.points.size) {
                    val p = arrow.points[i]
                    spine.lineTo(
                        p.second * cellSize + cellSize / 2,
                        p.first * cellSize + cellSize / 2
                    )
                }
            } else if (arrow.points.isNotEmpty()) {
                val first = arrow.points.first()
                spine.moveTo(
                    first.second * cellSize + cellSize / 2,
                    first.first * cellSize + cellSize / 2
                )
            }
            
            val tempPm = PathMeasure(spine, false)
            val originalLen = tempPm.length

            if (arrow.points.isNotEmpty()) {
                val last = arrow.points.last()
                val extendDist = width.toFloat() * 1.5f
                val dx = when (arrow.direction) {
                    Direction.LEFT -> -extendDist; Direction.RIGHT -> extendDist; else -> 0f
                }
                val dy = when (arrow.direction) {
                    Direction.UP -> -extendDist; Direction.DOWN -> extendDist; else -> 0f
                }
                spine.lineTo(
                    (last.second * cellSize + cellSize / 2) + dx,
                    (last.first * cellSize + cellSize / 2) + dy
                )
            }

            ArrowVisual(
                spinePath = spine,
                originalLength = originalLen,
                pathMeasure = PathMeasure(spine, false)
            )
        }
    }

    private data class Ripple(val x: Float, val y: Float, var radius: Float, var alpha: Float)
    private val ripples = mutableListOf<Ripple>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameEngine?.state != GameState.PLAYING) return false

        if (event.action == MotionEvent.ACTION_DOWN) {
            addRipple(event.x, event.y)
            val cell = getCellAt(event.x, event.y)
            if (cell != null) {
                gameEngine?.handleCellTap(cell.first, cell.second)
            }
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun addRipple(x: Float, y: Float) {
        val ripple = Ripple(x, y, 0f, 0.5f)
        ripples.add(ripple)
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400
            addUpdateListener {
                val p = it.animatedValue as Float
                ripple.radius = cellSize * 0.8f * p
                ripple.alpha = 0.5f * (1f - p)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { ripples.remove(ripple) }
            })
            start()
        }
        activeAnimators.add(animator)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onArrowExit(arrow: Arrow) {
        startExitAnimation(arrow)
        spawnParticles(arrow, ContextCompat.getColor(context, R.color.brand_blue), 6) // Reduced from 12
        shakeScreen(2f) // Reduced from 4f
    }

    override fun onArrowBlocked(arrow: Arrow) {
        startShakeAnimation(arrow)
        spawnParticles(arrow, ContextCompat.getColor(context, R.color.brand_red), 4) // Reduced from 8
        shakeScreen(5f) // Reduced from 10f
    }

    private fun spawnParticles(arrow: Arrow, color: Int, count: Int) {
        val head = arrow.points.last()
        val cx = head.second * cellSize + cellSize / 2
        val cy = head.first * cellSize + cellSize / 2
        
        repeat(count) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val speed = Random.nextFloat() * 6f + 1f // Reduced speed (was 10f + 2f)
            particles.add(Particle(
                x = cx, y = cy,
                vx = Math.cos(angle).toFloat() * speed,
                vy = Math.sin(angle).toFloat() * speed,
                size = Random.nextFloat() * 4f + 1f, // Reduced size (was 6f + 2f)
                alpha = 0.8f, // Start slightly transparent
                color = color
            ))
        }
        startParticleAnimation()
    }

    private fun startParticleAnimation() {
        if (activeAnimators.any { it is ValueAnimator && it.duration == 500L }) return
        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500
            addUpdateListener {
                val p = it.animatedValue as Float
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val part = iterator.next()
                    part.x += part.vx
                    part.y += part.vy
                    part.alpha = p
                    if (p <= 0) iterator.remove()
                }
                invalidate()
            }
            start()
        }
        activeAnimators.add(animator)
    }

    private fun shakeScreen(intensity: Float) {
        val animator = ValueAnimator.ofFloat(intensity, 0f)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            val v = animation.animatedValue as Float
            this.translationX = (Random.nextFloat() - 0.5f) * v * 2
            this.translationY = (Random.nextFloat() - 0.5f) * v * 2
        }
        animator.start()
        activeAnimators.add(animator)
    }

    private fun startExitAnimation(arrow: Arrow) {
        val boardSize = width.toFloat() * 1.5f
        val state = ExitState()
        animatingArrows[arrow] = state

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 750
        animator.interpolator = fastOutSlowIn
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            state.offset = progress * boardSize
            state.alpha = 1f - progress.coerceAtLeast(0.7f).let { (it - 0.6f) / 0.4f }
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animatingArrows.remove(arrow)
                invalidate()
                gameEngine?.onAnimationFinished()
            }
        })
        animator.start()
    }

    private fun startShakeAnimation(arrow: Arrow) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300
        animator.interpolator = OvershootInterpolator(2f)
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val shake = (Math.sin(progress * Math.PI * 4).toFloat() * 12f * (1f - progress))
            blockedArrows[arrow] = shake
            invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                blockedArrows.remove(arrow)
                invalidate()
            }
        })
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val board = board ?: return
        val size = board.size

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boardBackgroundPaint)

        canvas.save()
        canvas.translate(boardPadding, boardPadding)

        // 2. Dot Grid
        val dotRadius = cellSize * 0.04f
        val oldDotAlpha = dotPaint.alpha
        dotPaint.alpha = (dotGridAlpha * 255).toInt()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board.isCellOccupied(r, c)) continue
                val isOverlap = animatingArrows.keys.any { arrow ->
                    arrow.points.any { it.first == r && it.second == c }
                }
                if (isOverlap) continue
                canvas.drawCircle(c * cellSize + cellSize / 2, r * cellSize + cellSize / 2, dotRadius, dotPaint)
            }
        }
        dotPaint.alpha = oldDotAlpha

        // 3. Particles
        particles.forEach { p ->
            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.size, particlePaint)
        }
        
        // Ripples
        ripples.forEach { r ->
            particlePaint.color = Color.parseColor("#3B82F6")
            particlePaint.alpha = (r.alpha * 255).toInt()
            canvas.drawCircle(r.x, r.y, r.radius, particlePaint)
        }

        // 4. Static Arrows
        board.getArrows().forEach { arrow ->
            if (!blockedArrows.containsKey(arrow) && !animatingArrows.containsKey(arrow)) {
                drawSlitheringArrow(canvas, arrow, 0f, 1f, 0f)
            }
        }

        // 5. Blocked Arrows
        blockedArrows.forEach { (arrow, shake) ->
            drawSlitheringArrow(canvas, arrow, 0f, 1f, shake)
        }

        // 6. Animating Arrows
        animatingArrows.forEach { (arrow, state) ->
            drawSlitheringArrow(canvas, arrow, state.offset, state.alpha, 0f)
        }

        // 7. Hint Highlight
        hintArrowId?.let { hid ->
            board.getArrows().find { it.id == hid }?.let { arrow ->
                val time = (System.currentTimeMillis() % 1000) / 1000f
                val alpha = (150 + 100 * Math.sin(time * Math.PI * 2)).toInt().coerceIn(0, 255)
                drawArrowHighlight(canvas, arrow, alpha)
            }
            postInvalidateDelayed(30)
        }

        canvas.restore()

        // 8. Pulse
        if (pulseAlpha > 0) {
            val oldAlpha = boardBackgroundPaint.alpha
            boardBackgroundPaint.color = pulseColor
            boardBackgroundPaint.alpha = (pulseAlpha * 255).toInt()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boardBackgroundPaint)
            boardBackgroundPaint.alpha = oldAlpha
            boardBackgroundPaint.color = Color.WHITE
        }
    }

    private fun drawSlitheringArrow(canvas: Canvas, arrow: Arrow, offset: Float, alpha: Float, shake: Float) {
        val visual = getOrBuildVisual(arrow)
        val alphaInt = (alpha * 255).toInt()
        val dim = if (hintArrowId != null && hintArrowId != arrow.id) 0.5f else 1f

        bodySegmentPath.reset()
        val neckLength = cellSize * 0.12f
        val endDist = offset + visual.originalLength + neckLength
        val bodyEndDist = endDist - (cellSize * 0.05f)

        if (visual.originalLength < 0.1f) {
            val cx = arrow.points.first().second * cellSize + cellSize / 2
            val cy = arrow.points.first().first * cellSize + cellSize / 2
            val dx = when (arrow.direction) { Direction.LEFT -> -endDist; Direction.RIGHT -> endDist; else -> 0f }
            val dy = when (arrow.direction) { Direction.UP -> -endDist; Direction.DOWN -> endDist; else -> 0f }
            bodySegmentPath.moveTo(cx + dx, cy + dy)
            bodySegmentPath.lineTo(cx + dx + 0.1f, cy + dy)
        } else {
            visual.pathMeasure.getSegment(offset, bodyEndDist, bodySegmentPath, true)
        }

        visual.pathMeasure.getPosTan(endDist, posBuffer, tanBuffer)
        val hx = posBuffer[0]
        val hy = posBuffer[1]
        
        var angle = Math.toDegrees(Math.atan2(tanBuffer[1].toDouble(), tanBuffer[0].toDouble())).toFloat()
        if (Math.abs(tanBuffer[1]) < 0.05f) {
            angle = if (tanBuffer[0] >= 0) 0f else 180f
        } else if (Math.abs(tanBuffer[0]) < 0.05f) {
            angle = if (tanBuffer[1] >= 0) 90f else -90f
        }

        canvas.save()
        if (shake != 0f) canvas.translate(shake, 0f)

        val sdx = cellSize * 0.02f
        val sdy = cellSize * 0.02f
        shadowPaint.alpha = (alpha * 25).toInt()
        shadowHeadPaint.alpha = (alpha * 25).toInt()
        
        canvas.save()
        canvas.translate(sdx, sdy)
        canvas.drawPath(bodySegmentPath, shadowPaint)
        matrixBuffer.reset()
        matrixBuffer.postRotate(angle)
        matrixBuffer.postTranslate(hx, hy)
        headDrawPath.reset()
        headBaseShape.transform(matrixBuffer, headDrawPath)
        canvas.drawPath(headDrawPath, shadowHeadPaint)
        canvas.restore()

        val baseColor = Color.parseColor("#0F172A")
        val r = (Color.red(baseColor) * dim).toInt()
        val g = (Color.green(baseColor) * dim).toInt()
        val b = (Color.blue(baseColor) * dim).toInt()
        val finalColor = Color.rgb(r, g, b)
        
        arrowBodyPaint.color = finalColor
        arrowBodyPaint.alpha = alphaInt
        canvas.drawPath(bodySegmentPath, arrowBodyPaint)

        arrowHeadPaint.color = finalColor
        arrowHeadPaint.alpha = alphaInt
        canvas.drawPath(headDrawPath, arrowHeadPaint)

        canvas.restore()
    }

    private fun drawArrowHighlight(canvas: Canvas, arrow: Arrow, alpha: Int) {
        val visual = getOrBuildVisual(arrow)
        bodySegmentPath.reset()
        visual.pathMeasure.getSegment(0f, visual.originalLength, bodySegmentPath, true)
        hintPaint.alpha = alpha
        canvas.drawPath(bodySegmentPath, hintPaint)
    }

    private fun getCellAt(x: Float, y: Float): Pair<Int, Int>? {
        val board = board ?: return null
        val col = ((x - boardPadding) / cellSize).toInt()
        val row = ((y - boardPadding) / cellSize).toInt()
        return if (row in 0 until board.size && col in 0 until board.size) row to col else null
    }
}
