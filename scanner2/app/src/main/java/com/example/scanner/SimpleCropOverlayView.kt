
package com.example.scanner
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class SimpleCropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cropRect = RectF(100f, 100f, 500f, 500f)
    private val handleRadius = 30f
    private var selectedHandle: HandleType? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    enum class HandleType {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE, TOP, BOTTOM, LEFT, RIGHT
    }

    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 4f
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val shadowPaint = Paint().apply {
        color = Color.argb(128, 0, 0, 0) // Полупрозрачный черный
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем затемнение вокруг области обрезки
        drawShadow(canvas)

        // Рисуем рамку обрезки
        canvas.drawRect(cropRect, borderPaint)

        // Рисуем ручки
        drawHandle(canvas, cropRect.left, cropRect.top, HandleType.TOP_LEFT)
        drawHandle(canvas, cropRect.right, cropRect.top, HandleType.TOP_RIGHT)
        drawHandle(canvas, cropRect.left, cropRect.bottom, HandleType.BOTTOM_LEFT)
        drawHandle(canvas, cropRect.right, cropRect.bottom, HandleType.BOTTOM_RIGHT)
    }

    private fun drawShadow(canvas: Canvas) {
        // Рисуем 4 прямоугольника вокруг области обрезки для эффекта затемнения
        val leftRect = RectF(0f, 0f, cropRect.left, height.toFloat())
        val topRect = RectF(cropRect.left, 0f, cropRect.right, cropRect.top)
        val rightRect = RectF(cropRect.right, 0f, width.toFloat(), height.toFloat())
        val bottomRect = RectF(cropRect.left, cropRect.bottom, cropRect.right, height.toFloat())

        canvas.drawRect(leftRect, shadowPaint)
        canvas.drawRect(topRect, shadowPaint)
        canvas.drawRect(rightRect, shadowPaint)
        canvas.drawRect(bottomRect, shadowPaint)
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float, handleType: HandleType) {
        // Рисуем внешний круг
        handlePaint.color = Color.WHITE
        canvas.drawCircle(x, y, handleRadius, handlePaint)

        // Рисуем внутренний круг
        handlePaint.color = Color.BLUE
        canvas.drawCircle(x, y, handleRadius - 5, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedHandle = getHandleAtPoint(event.x, event.y)
                lastTouchX = event.x
                lastTouchY = event.y
                return selectedHandle != null
            }
            MotionEvent.ACTION_MOVE -> {
                selectedHandle?.let { handle ->
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    when (handle) {
                        HandleType.TOP_LEFT -> {
                            cropRect.left += dx
                            cropRect.top += dy
                        }
                        HandleType.TOP_RIGHT -> {
                            cropRect.right += dx
                            cropRect.top += dy
                        }
                        HandleType.BOTTOM_LEFT -> {
                            cropRect.left += dx
                            cropRect.bottom += dy
                        }
                        HandleType.BOTTOM_RIGHT -> {
                            cropRect.right += dx
                            cropRect.bottom += dy
                        }
                        HandleType.MOVE -> {
                            cropRect.offset(dx, dy)
                        }
                        HandleType.TOP -> {
                            cropRect.top += dy
                        }
                        HandleType.BOTTOM -> {
                            cropRect.bottom += dy
                        }
                        HandleType.LEFT -> {
                            cropRect.left += dx
                        }
                        HandleType.RIGHT -> {
                            cropRect.right += dx
                        }
                    }

                    ensureValidRect()
                    invalidate()

                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                selectedHandle = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getHandleAtPoint(x: Float, y: Float): HandleType? {
        val handles = listOf(
            Triple(cropRect.left, cropRect.top, HandleType.TOP_LEFT),
            Triple(cropRect.right, cropRect.top, HandleType.TOP_RIGHT),
            Triple(cropRect.left, cropRect.bottom, HandleType.BOTTOM_LEFT),
            Triple(cropRect.right, cropRect.bottom, HandleType.BOTTOM_RIGHT)
        )

        // Проверяем угловые ручки
        handles.forEach { (handleX, handleY, type) ->
            if (hypot((x - handleX).toDouble(), (y - handleY).toDouble()) <= handleRadius) {
                return type
            }
        }

        // Проверяем, если касание внутри прямоугольника (для перемещения)
        if (cropRect.contains(x, y)) {
            return HandleType.MOVE
        }

        return null
    }

    private fun ensureValidRect() {
        // Обеспечиваем минимальный размер
        val minSize = 100f
        if (cropRect.width() < minSize) {
            when (selectedHandle) {
                HandleType.TOP_LEFT -> cropRect.left = cropRect.right - minSize
                HandleType.TOP_RIGHT -> cropRect.right = cropRect.left + minSize
                HandleType.BOTTOM_LEFT -> cropRect.left = cropRect.right - minSize
                HandleType.BOTTOM_RIGHT -> cropRect.right = cropRect.left + minSize
                else -> {
                    if (cropRect.width() < minSize) {
                        val centerX = cropRect.centerX()
                        cropRect.left = centerX - minSize / 2
                        cropRect.right = centerX + minSize / 2
                    }
                }
            }
        }

        if (cropRect.height() < minSize) {
            when (selectedHandle) {
                HandleType.TOP_LEFT -> cropRect.top = cropRect.bottom - minSize
                HandleType.TOP_RIGHT -> cropRect.top = cropRect.bottom - minSize
                HandleType.BOTTOM_LEFT -> cropRect.bottom = cropRect.top + minSize
                HandleType.BOTTOM_RIGHT -> cropRect.bottom = cropRect.top + minSize
                else -> {
                    if (cropRect.height() < minSize) {
                        val centerY = cropRect.centerY()
                        cropRect.top = centerY - minSize / 2
                        cropRect.bottom = centerY + minSize / 2
                    }
                }
            }
        }

        // Ограничиваем в пределах view
        cropRect.left = cropRect.left.coerceIn(0f, width.toFloat())
        cropRect.top = cropRect.top.coerceIn(0f, height.toFloat())
        cropRect.right = cropRect.right.coerceIn(0f, width.toFloat())
        cropRect.bottom = cropRect.bottom.coerceIn(0f, height.toFloat())

        // Обеспечиваем правильный порядок координат
        if (cropRect.left > cropRect.right) {
            val temp = cropRect.left
            cropRect.left = cropRect.right
            cropRect.right = temp
        }
        if (cropRect.top > cropRect.bottom) {
            val temp = cropRect.top
            cropRect.top = cropRect.bottom
            cropRect.bottom = temp
        }
    }

    fun getCropRect(): Rect {
        return Rect(
            cropRect.left.toInt().coerceAtLeast(0),
            cropRect.top.toInt().coerceAtLeast(0),
            cropRect.right.toInt().coerceAtMost(width),
            cropRect.bottom.toInt().coerceAtMost(height)
        )
    }

    // Метод для установки начального размера относительно изображения
    fun setupInitialRect(imageWidth: Int, imageHeight: Int) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Вычисляем размер области обрезки (80% от меньшей стороны)
        val size = minOf(viewWidth, viewHeight) * 0.8f

        // Центрируем область обрезки
        cropRect.set(
            (viewWidth - size) / 2,
            (viewHeight - size) / 2,
            (viewWidth + size) / 2,
            (viewHeight + size) / 2
        )

        invalidate()
    }

    // Метод для сброса области обрезки
    fun resetCropRect() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val size = minOf(viewWidth, viewHeight) * 0.8f

        cropRect.set(
            (viewWidth - size) / 2,
            (viewHeight - size) / 2,
            (viewWidth + size) / 2,
            (viewHeight + size) / 2
        )
        invalidate()
    }
}