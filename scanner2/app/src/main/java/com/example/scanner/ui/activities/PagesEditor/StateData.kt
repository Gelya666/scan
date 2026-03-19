package com.example.scanner.ui.activities.PagesEditor

import android.graphics.Bitmap
import com.example.scanner.PhotoFilters
class StateData(

    private var photoPaths: ArrayList<String> = arrayListOf<String>(),
    private val originalImages: MutableMap<Int, String> = mutableMapOf(),// ?????
    private val originalImagesBackup: MutableMap<Int, String> = mutableMapOf(),

    private val originalBitmaps: MutableMap<Int, Bitmap> = mutableMapOf(),
    private val bitmaps: MutableMap<Int, Bitmap> =mutableMapOf(),// ??????


    private val filtersMap: MutableMap<Int, PhotoFilters.FilterType> = mutableMapOf(),
    private val filterIntensityMap: MutableMap<Int,Float> = mutableMapOf(),
    private val rotationStates: MutableMap<Int, Float> = mutableMapOf(),

    private var currentPosition: Int = 0,
    private var currentIntensity: Float = 0.5f
) {
    fun SetFilterData(position: Int, filterType: PhotoFilters.FilterType, intensity: Float) {
        filtersMap[position] = filterType
        filterIntensityMap[position] = intensity;
    }
    fun GetFilterType(position:Int): PhotoFilters.FilterType{
        val nullable = filtersMap[position]
        if(nullable == null)
            return PhotoFilters.FilterType.NONE
        return nullable
    }
    fun setPhotoPaths(str:String): ArrayList<String>{
         photoPaths.add(str)
    }
    fun getRotationStates(position:Int): Float {
        var currentRotation=rotationStates[position]

        if(currentRotation==null)
            return 0f
        return currentRotation
    }
    fun setRotationStates(position: Int, newRotation: Float) {
        rotationStates[position] = newRotation
    }
    fun GetFilterIntensity(position:Int): Float {
        var nullable=filterIntensityMap[position]
        if(nullable==null)
            return 1.0f
        return nullable
    }
    fun getOriginalImagesContainsKey(position:Int): Boolean {
        val containsKey=originalImages.containsKey(position)
        return containsKey
    }
    fun SetOriginaImages(position: Int,backupFilePath:String) {
        originalImages[position]=backupFilePath
    }
    fun removeFilterAndIntensity(position:Int){
        filtersMap.remove(position)
        filterIntensityMap.remove(position)
    }
}