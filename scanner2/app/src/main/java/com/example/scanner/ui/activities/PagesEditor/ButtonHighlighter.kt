package com.example.scanner.ui.activities.PagesEditor
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import com.example.scanner.R
import com.google.android.material.button.MaterialButton

class ButtonHighlighter(private val context: Context){
    private val activeColor = R.color.purple_500
    private val inactiveColor = android.R.color.transparent
    private val activeTextColor=  android.R.color.white
    private val inactiveTextColor= R.color.purple_500
    @SuppressLint("SuspiciousIndentation")
    fun highlight(button: MaterialButton?){
     if(button==null) return
        //устанавливаем фон кнопки
        button.backgroundTintList=ContextCompat.getColorStateList(context,activeColor)
        button.setTextColor(ContextCompat.getColor(context,activeTextColor))
    }
    fun unhighlight(button: MaterialButton?){
        if(button==null) return
        button.backgroundTintList=ContextCompat.getColorStateList(context,inactiveColor)
        button.setTextColor(ContextCompat.getColor(context,inactiveTextColor))
    }
    fun setHighlighted(button:MaterialButton?,isActive:Boolean){
        if(isActive){
         highlight(button)
        }else{
            unhighlight(button)
        }
    }
    fun resetAll(vararg button:MaterialButton){
        button.forEach{button->unhighlight(button)}
    }
}