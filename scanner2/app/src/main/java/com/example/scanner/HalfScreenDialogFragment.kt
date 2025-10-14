package com.example.scanner

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class HalfScreenDialogFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_half_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }
    private fun setupClickListeners(view:View){
        view.findViewById<View>(R.id.button_save)?.setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_share)?.setOnClickListener {
            shareImage()
            dismiss()
        }
        view.findViewById<View>(R.id.btn_addPage)?.setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_email)?.setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_undo)?.setOnClickListener{
            dismiss()
        }
        view.findViewById<View>(R.id.btn_on)?.setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_on)?.setOnClickListener {
            dismiss()
        }
    }
    private fun shareImage(){
        Toast.makeText(requireContext(),"Поделиться изображением",Toast.LENGTH_SHORT).show()
    }
    override fun onStart(){
        super.onStart()
        dialog?.window?.apply {
            // Размер - занимает 60% высоты экрана
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.6).toInt()
            )

            // Прозрачный фон
            setBackgroundDrawable(ColorDrawable(0x80000000.toInt()))

            // Выравнивание по низу экрана
            setGravity(android.view.Gravity.BOTTOM)

            // Анимация появления снизу
            setWindowAnimations(R.style.DialogAnimation)
        }
    }

    companion object {
        fun newInstance(): HalfScreenDialogFragment{
            return HalfScreenDialogFragment()
                }
            }
    }
