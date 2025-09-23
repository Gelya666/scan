package com.example.scanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SimpleCropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val shadowPaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private var cropRect = RectF()
    private var isDragging = false
    private var lastX = 0f
    private var lastY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Инициализируем прямоугольник обрезки по центру
        val size = minOf(w, h) * 0.6f
        val left = (w - size) / 2
        val top = (h - size) / 2
        cropRect.set(left, top, left + size, top + size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем затемнение вокруг области обрезки
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, shadowPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, shadowPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, shadowPaint)

        // Рисуем прямоугольник обрезки
        canvas.drawRect(cropRect, paint)

        // Рисуем угловые маркеры
        drawCornerMarkers(canvas)
    }

    private fun drawCornerMarkers(canvas: Canvas) {
        val markerSize = 20f

        // Левый верхний
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + markerSize, cropRect.top, paint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + markerSize, paint)

        // Правый верхний
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right - markerSize, cropRect.top, paint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + markerSize, paint)

        // Левый нижний
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + markerSize, cropRect.bottom, paint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left, cropRect.bottom - markerSize, paint)

        // Правый нижний
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right - markerSize, cropRect.bottom, paint)
        canvas.drawLine(cropRect.right, cropRect.bottom, cropRect.right, cropRect.bottom - markerSize, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y

                // Проверяем, касаемся ли мы области обрезки
                if (cropRect.contains(lastX, lastY)) {
                    isDragging = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    // Перемещаем прямоугольник
                    cropRect.offset(dx, dy)

                    // Ограничиваем в пределах экрана
                    if (cropRect.left < 0) cropRect.offset(-cropRect.left, 0f)
                    if (cropRect.top < 0) cropRect.offset(0f, -cropRect.top)
                    if (cropRect.right > width) cropRect.offset(width - cropRect.right, 0f)
                    if (cropRect.bottom > height) cropRect.offset(0f, height - cropRect.bottom)

                    lastX = event.x
                    lastY = event.y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    fun getCropRect(): Rect {
        return Rect(
            cropRect.left.toInt(),
            cropRect.top.toInt(),
            cropRect.right.toInt(),
            cropRect.bottom.toInt()
        )
    }
}