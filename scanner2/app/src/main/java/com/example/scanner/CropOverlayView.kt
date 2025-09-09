package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

class CropOverlayView @JvmOverloads
constructor(
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
    private val handleRadius = 20f
    private val minCropSize = 100f
    private val overlayPaint = Paint()

    init {
        overlayPaint.color = overlayColor
        overlayPaint.style = Paint.Style.FILL
    }

    private val borderPaint = Paint()

    init {
        borderPaint.color = overlayColor
        borderPaint.style = Paint.Style.FILL
        borderPaint.isAntiAlias = true
    }

    private val handlePaint = Paint()

    init {
        handlePaint.color = handleColor
        handlePaint.style = Paint.Style.FILL
        handlePaint.isAntiAlias = true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.restore()
        canvas.drawRect(cropRect, borderPaint)
        drawHandles(canvas)
    }

    private fun drawHandles(canvas: Canvas) {
        val handles = arrayOf(
            PointF(cropRect.left, cropRect.top),
            PointF(cropRect.right, cropRect.top),
            PointF(cropRect.left, cropRect.bottom),
            PointF(cropRect.right, cropRect.bottom),
            PointF(cropRect.left, cropRect.top)
        )
        handles.forEach { point -> canvas.drawCircle(point.x, point.y, handleRadius, handlePaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragHandle = getTouchedHandle(event.x, event.y)
                isDragging = dragHandle != -1
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateCropRect(event.x, event.y)
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                dragHandle = -1
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTouchedHandle(x: Float, y: Float): Int {
        val handles = arrayOf(
            RectF(
                cropRect.left - handleRadius, cropRect.top - handleRadius,
                cropRect.left + handleRadius, cropRect.top + handleRadius
            ),
            RectF(
                cropRect.right - handleRadius, cropRect.top - handleRadius,
                cropRect.right + handleRadius, cropRect.top + handleRadius
            ),
            RectF(
                cropRect.left - handleRadius, cropRect.bottom - handleRadius,
                cropRect.left + handleRadius, cropRect.bottom + handleRadius
            ),
            RectF(
                cropRect.right - handleRadius, cropRect.bottom - handleRadius,
                cropRect.right + handleRadius, cropRect.bottom + handleRadius
            )
        )
        handles.forEachIndexed { index, rect ->
            if (rect.contains(x, y)) {
                return index
            }
        }
        if (cropRect.contains(x, y)) {
            return 4
        }
        return -1
    }

    private fun updateCropRect(x: Float, y: Float) {
        when (dragHandle) {
            0 -> {
                cropRect.left = x.coerceIn(0f, cropRect.right - minCropSize)
                cropRect.top = y.coerceIn(0f, cropRect.bottom - minCropSize)
            }

            1 -> {
                cropRect.right = x.coerceIn(cropRect.left + minCropSize, width.toFloat())
                cropRect.top = y.coerceIn(0f, cropRect.bottom - minCropSize)
            }

            2 -> {
                cropRect.left = x.coerceIn(0f, cropRect.right - minCropSize)
                cropRect.bottom = y.coerceIn(0f, cropRect.top - minCropSize)
            }

            3 -> {
                cropRect.right = x.coerceIn(0f, cropRect.left + minCropSize)
                cropRect.bottom = y.coerceIn(0f, cropRect.top + minCropSize)
            }

            4 -> {
                val dx = x - (cropRect.left + cropRect.width() / 2)
                val dy = y - (cropRect.top + cropRect.height() / 2)
                val newLeft = cropRect.left + dx
                val newTop = cropRect.top + dy
                val newRight = cropRect.right + dx
                val newBottom = cropRect.bottom + dy
                if (newLeft >= 0 && newRight <= width && newTop >= 0 && newBottom <= height) {
                    cropRect.set(newLeft, newTop, newRight, newBottom)
                }
            }
        }
    }

    fun getCroppedArea(bitmap: Bitmap, imageView: ImageView): Rect {
        val scale = calculateScale(bitmap, imageView)
        val offsetX = calculateOffsetX(bitmap, imageView, scale)
        val offsetY = calculateOffsetY(bitmap, imageView, scale)
        return Rect(
            ((cropRect.left - offsetX) / scale).toInt(),
            ((cropRect.top - offsetY) / scale).toInt(),
            ((cropRect.right - offsetX / scale).toInt()),
            ((cropRect.bottom - offsetY / scale).toInt())
        )
    }

    private fun calculateScale(bitmap: Bitmap, imageView: ImageView): Float {
        val drawable = imageView.drawable ?: return 1f
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        val scaleX = imageView.width.toFloat() / bitmapWidth
        val scaleY = imageView.height.toFloat() / bitmapHeight
        val scale = minOf(scaleX, scaleY)
        return if (scale > 0) scale else 1f
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








