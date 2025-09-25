package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log.e
import jp.co.cyberagent.android.gpuimage.GPUImage
import java.util.logging.Filter
import android.graphics.Color
import android.graphics.PointF

import jp.co.cyberagent.android.gpuimage.filter.*

object PhotoFilters {
    enum class FilterType {
        NONE,
        GRAYSCALE,
        SEPIA,
        VIGNETTE,
        INVERT,
        POSTERIZE,
        SATURATE,
        BRIGHTNESS,
        CONTRAST,
        SKETCH,
        BLUR,
        SHARPEN,
        EMBOSS,
        TOON,
        SWIRL
    }

    fun applyFilter(context: Context, bitmap: Bitmap, filterType: FilterType, intensity: Float = 1.0F): Bitmap {
        return try {
            val gpuImage = GPUImage(context).apply {
                setImage(bitmap)
                setFilter(getFilter(filterType, intensity))
            }
            gpuImage.bitmapWithFilterApplied
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap

        }
    }

    private fun getFilter(filterType: FilterType, intensity: Float): GPUImageFilter {
        return when (filterType) {
            FilterType.NONE -> GPUImageFilter()
            FilterType.GRAYSCALE -> GPUImageGrayscaleFilter()
            FilterType.SEPIA -> GPUImageSepiaToneFilter().apply { setIntensity(intensity) }
            FilterType.VIGNETTE -> GPUImageVignetteFilter().apply {
                setVignetteCenter(PointF(0.5f, 0.5f))
                setVignetteColor(floatArrayOf(0.0f, 0.0f, 0.0f))
                setVignetteStart(0.3f)
                setVignetteEnd(1.0f)
            }

            FilterType.INVERT -> GPUImageColorInvertFilter()
            FilterType.POSTERIZE -> GPUImagePosterizeFilter().apply { setColorLevels(intensity.toInt()) }
            FilterType.SATURATE -> GPUImageSaturationFilter().apply { setSaturation(intensity) }
            FilterType.BRIGHTNESS -> GPUImageBrightnessFilter().apply { setBrightness(intensity) }
            FilterType.CONTRAST -> GPUImageContrastFilter().apply { setContrast(intensity) }
            FilterType.SKETCH -> GPUImageSketchFilter()
            FilterType.BLUR -> GPUImageGaussianBlurFilter().apply { setBlurSize(intensity) }
            FilterType.SHARPEN -> GPUImageSharpenFilter().apply { setSharpness(intensity) }
            FilterType.EMBOSS -> GPUImageEmbossFilter().apply { setIntensity(intensity) }
            FilterType.TOON -> GPUImageToonFilter().apply {
                setThreshold(intensity)
                setQuantizationLevels(intensity * 10f)
            }

            FilterType.SWIRL -> GPUImageSwirlFilter().apply {
                setRadius(intensity)
                setAngle(intensity * 2f)
            }
        }
    }

    fun getAllFilters(): List<FilterItem> {
        return listOf(
            FilterItem(FilterType.NONE, "Оригинал", R.drawable.ic_filter_none),
            FilterItem(FilterType.GRAYSCALE, "Черно-белый", R.drawable.ic_filter_bw),
            FilterItem(FilterType.SEPIA, "Сепия", R.drawable.ic_filter_sepia),
            FilterItem(FilterType.VIGNETTE, "Виньетка", R.drawable.ic_filter_vignette),
            FilterItem(FilterType.INVERT, "Инверт", R.drawable.ic_filter_invert),
            FilterItem(FilterType.POSTERIZE, "Постеризация", R.drawable.ic_filter_posterize),
            FilterItem(FilterType.SATURATE, "Насыщенность", R.drawable.ic_filter_saturate),
            FilterItem(FilterType.BRIGHTNESS, "Яркость", R.drawable.ic_filter_brightness),
            FilterItem(FilterType.CONTRAST, "Контраст", R.drawable.ic_filter_contrast),
            FilterItem(FilterType.SKETCH, "Эскиз", R.drawable.ic_filter_sketch),
            FilterItem(FilterType.BLUR, "Размытие", R.drawable.ic_filter_blur),
            FilterItem(FilterType.SHARPEN, "Резкость", R.drawable.ic_filter_sharpen),
            FilterItem(FilterType.EMBOSS, "Тиснение", R.drawable.ic_filter_emboss),
            FilterItem(FilterType.TOON, "Мультяшный", R.drawable.ic_filter_toon),
            FilterItem(FilterType.SWIRL, "Вихрь", R.drawable.ic_filter_swirl)
        )
    }

    data class FilterItem(
        val type: FilterType,
        val name: String,
        val iconRes: Int
    )
}


