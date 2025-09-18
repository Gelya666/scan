package com.example.scanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.graphics.PorterDuffXfermode
import android.graphics.PorterDuff
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.Style
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var cropRect = RectF()
    private var isDragging = false
    private var dragHandle = -1

    private val overlayColor = Color.argb(150, 0, 0, 0)
    private val borderColor = Color.WHITE
    private val handleColor = Color.BLUE
    private val borderWidth = 4f
    private val handleRadius = 30f
    private val touchTargetExtra = 30f // расширяет область срабатывания тача
    private val minCropSize = 120f

    // Paints
    private val overlayPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = overlayColor
        style = Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val handlePaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = handleColor
        style = Style.FILL
    }

    // Для перемещения
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Для отрисовки с "вырубленным" прямоугольником
    private val clearPaint = Paint(ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Инициализируем cropRect по центру экрана (если ещё не задан)
        if (cropRect.isEmpty) {
            val margin = 40f
            val left = margin
            val top = margin
            val right = (w - margin).toFloat()
            val bottom = (h - margin).toFloat()
            // делаем квадрат по меньшей стороне
            val width = right - left
            val height = bottom - top
            val size = minOf(width, height) * 0.7f
            val cx = w / 2f
            val cy = h / 2f
            cropRect.set(
                cx - size / 2f,
                cy - size / 2f,
                cx + size / 2f,
                cy + size / 2f
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Сохраняем слой, чтобы можно было "вырезать"
        val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // Заливка полупрозрачным цветом
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // "Вырезаем" прямоугольник cropRect — он становится прозрачным
        val clearPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawRect(cropRect, clearPaint)

        // Восстанавливаем слой
        canvas.restoreToCount(sc)

        // Теперь поверх рисуем рамку и ручки
        canvas.drawRect(cropRect, borderPaint)
        drawHandles(canvas)
    }


    private fun drawHandles(canvas: Canvas) {
        val points = listOf(
            PointF(cropRect.left, cropRect.top),
            PointF(cropRect.right, cropRect.top),
            PointF(cropRect.left, cropRect.bottom),
            PointF(cropRect.right, cropRect.bottom)
        )
        for (p in points) {
            canvas.drawCircle(p.x, p.y, handleRadius, handlePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        Log.d("danil_logs", "onTouchEvent: $event")

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragHandle = getTouchedHandle(x, y)
                isDragging = dragHandle != -1
                lastTouchX = x
                lastTouchY = y
                // Возьмём событие — будем обрабатывать MOVE/UP
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return true

                // Если dragHandle == 4 — значит перетаскивание внутри области
                if (dragHandle == 4) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    moveCropBy(dx, dy)
                } else {
                    updateCropRectForHandle(dragHandle, x, y)
                }

                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                dragHandle = -1
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun getTouchedHandle(x: Float, y: Float): Int {
        // Индексы: 0 = TL, 1 = TR, 2 = BL, 3 = BR, 4 = inside (move)
        val handles = arrayOf(
            RectF(
                cropRect.left - handleRadius - touchTargetExtra,
                cropRect.top - handleRadius - touchTargetExtra,
                cropRect.left + handleRadius + touchTargetExtra,
                cropRect.top + handleRadius + touchTargetExtra
            ),
            RectF(
                cropRect.right - handleRadius - touchTargetExtra,
                cropRect.top - handleRadius - touchTargetExtra,
                cropRect.right + handleRadius + touchTargetExtra,
                cropRect.top + handleRadius + touchTargetExtra
            ),
            RectF(
                cropRect.left - handleRadius - touchTargetExtra,
                cropRect.bottom - handleRadius - touchTargetExtra,
                cropRect.left + handleRadius + touchTargetExtra,
                cropRect.bottom + handleRadius + touchTargetExtra
            ),
            RectF(
                cropRect.right - handleRadius - touchTargetExtra,
                cropRect.bottom - handleRadius - touchTargetExtra,
                cropRect.right + handleRadius + touchTargetExtra,
                cropRect.bottom + handleRadius + touchTargetExtra
            )
        )

        handles.forEachIndexed { index, rect ->
            if (rect.contains(x, y)) return index
        }

        // Внутри прямоугольника? тогда режим перемещения
        if (cropRect.contains(x, y)) return 4

        return -1
    }

    private fun updateCropRectForHandle(handle: Int, x: Float, y: Float) {
        when (handle) {
            0 -> { // TL
                val newLeft = x.coerceIn(0f, cropRect.right - minCropSize)
                val newTop = y.coerceIn(0f, cropRect.bottom - minCropSize)
                cropRect.left = newLeft
                cropRect.top = newTop
            }

            1 -> { // TR
                val newRight = x.coerceIn(cropRect.left + minCropSize, width.toFloat())
                val newTop = y.coerceIn(0f, cropRect.bottom - minCropSize)
                cropRect.right = newRight
                cropRect.top = newTop
            }

            2 -> { // BL
                val newLeft = x.coerceIn(0f, cropRect.right - minCropSize)
                val newBottom = y.coerceIn(cropRect.top + minCropSize, height.toFloat())
                cropRect.left = newLeft
                cropRect.bottom = newBottom
            }

            3 -> { // BR
                val newRight = x.coerceIn(cropRect.left + minCropSize, width.toFloat())
                val newBottom = y.coerceIn(cropRect.top + minCropSize, height.toFloat())
                cropRect.right = newRight
                cropRect.bottom = newBottom
            }
        }
        // На случай инвариантов: гарантируем минимальный размер и внутри вью
        normalizeCropRect()
    }

    private fun moveCropBy(dx: Float, dy: Float) {
        val newLeft = (cropRect.left + dx).coerceAtLeast(0f)
        val newTop = (cropRect.top + dy).coerceAtLeast(0f)
        val newRight = newLeft + cropRect.width()
        val newBottom = newTop + cropRect.height()

        // проверяем границы правой/нижней стороны
        if (newRight <= width && newBottom <= height) {
            cropRect.set(newLeft, newTop, newRight, newBottom)
        } else {
            // подгоняем по границам
            var adjLeft = newLeft
            var adjTop = newTop
            if (newRight > width) adjLeft = width - cropRect.width()
            if (newBottom > height) adjTop = height - cropRect.height()
            cropRect.set(adjLeft.coerceAtLeast(0f), adjTop.coerceAtLeast(0f),
                (adjLeft + cropRect.width()).coerceAtMost(width.toFloat()),
                (adjTop + cropRect.height()).coerceAtMost(height.toFloat()))
        }
    }

    private fun normalizeCropRect() {
        // сохраняем cropRect внутри вью и с минимальным размером
        if (cropRect.width() < minCropSize) {
            val cx = cropRect.centerX()
            cropRect.left = (cx - minCropSize / 2f).coerceAtLeast(0f)
            cropRect.right = (cropRect.left + minCropSize).coerceAtMost(width.toFloat())
        }
        if (cropRect.height() < minCropSize) {
            val cy = cropRect.centerY()
            cropRect.top = (cy - minCropSize / 2f).coerceAtLeast(0f)
            cropRect.bottom = (cropRect.top + minCropSize).coerceAtMost(height.toFloat())
        }

        // пересечение с границами вью
        if (cropRect.left < 0f) {
            val offset = -cropRect.left
            cropRect.offset(offset, 0f)
        }
        if (cropRect.top < 0f) {
            val offset = -cropRect.top
            cropRect.offset(0f, offset)
        }
        if (cropRect.right > width) {
            val offset = width - cropRect.right
            cropRect.offset(offset, 0f)
        }
        if (cropRect.bottom > height) {
            val offset = height - cropRect.bottom
            cropRect.offset(0f, height - cropRect.bottom)
        }
    }

    /**
     * Возвращает область (в пикселях исходного bitmap), соответствующую cropRect.
     * Результат корректируется по границам bitmap.
     */
    fun getCroppedArea(bitmap: Bitmap, imageView: ImageView): Rect {
        val scale = calculateScale(bitmap, imageView)
        val offsetX = calculateOffsetX(bitmap, imageView, scale)
        val offsetY = calculateOffsetY(bitmap, imageView, scale)

        // переводим координаты cropRect (view) -> bitmap
        val left = ((cropRect.left - offsetX) / scale).toInt()
        val top = ((cropRect.top - offsetY) / scale).toInt()
        val right = ((cropRect.right - offsetX) / scale).toInt()
        val bottom = ((cropRect.bottom - offsetY) / scale).toInt()

        // корректируем в пределах bitmap
        val clampedLeft = left.coerceIn(0, bitmap.width - 1)
        val clampedTop = top.coerceIn(0, bitmap.height - 1)
        val clampedRight = right.coerceIn(clampedLeft + 1, bitmap.width)
        val clampedBottom = bottom.coerceIn(clampedTop + 1, bitmap.height)

        return Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }

    private fun calculateScale(bitmap: Bitmap, imageView: ImageView): Float {
        // Учитываем fitCenter-поведение: масштаб = min(viewW/bitmapW, viewH/bitmapH)
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        val scaleX = imageView.width.toFloat() / bitmapWidth
        val scaleY = imageView.height.toFloat() / bitmapHeight
        val scale = minOf(scaleX, scaleY)
        return if (scale > 0f) scale else 1f
    }

    private fun calculateOffsetX(bitmap: Bitmap, imageView: ImageView, scale: Float): Float {
        val bitmapWidth = bitmap.width * scale
        return (imageView.width - bitmapWidth) / 2f
    }

    private fun calculateOffsetY(bitmap: Bitmap, imageView: ImageView, scale: Float): Float {
        val bitmapHeight = bitmap.height * scale
        return (imageView.height - bitmapHeight) / 2f
    }
}
