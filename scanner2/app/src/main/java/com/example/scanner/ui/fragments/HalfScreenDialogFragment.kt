package com.example.scanner.ui.fragments

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.scanner.R
import com.example.scanner.ui.activities.PagesEditor.PdfPagesEditorActivity

class HalfScreenDialogFragment : DialogFragment()
{
    //переменная для хранения ссылки на объект реализующий интерфейс
    private var pdfSaveListener: PdfSaveListener? = null

    //список путей к фотографиям
    private var photoPaths = arrayListOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //создание view из макета и помещание в родительский контейнер
        return inflater.inflate(R.layout.dialog_half_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //вызов функции для обработки всх кликов
        setupClickListeners(view)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)

        //является ли context  реализацией интерфейса PdfSaveListener
        if (context is PdfSaveListener) {
            pdfSaveListener = context
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

            //извлекаем спсиок строк по ключу "image_paths"
            //если список строк пуст создаётся пустой список
            photoPaths = it.getStringArrayList("image_paths") ?: ArrayList()
        }
    }
    private fun setupClickListeners(view:View){
        view.findViewById<View>(R.id.button_save)?.setOnClickListener {
            dismiss()
            saveAsPdf()
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
    //метод вызвается когда фрагмент видим для пользователя
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
            setGravity(Gravity.BOTTOM)

            // Анимация появления снизу
            setWindowAnimations(R.style.DialogAnimation)
        }
    }
    private fun saveAsPdf(){

        //приведи к типу PdfPagesEditorActivity и вызов функции saveImageToPdf()
        (activity as? PdfPagesEditorActivity)?.saveImageToPdf()
    }
    interface PdfSaveListener {
        fun onSavePdf(imagePaths: ArrayList<String>)
    }


    companion object {
        //фабричный метод для создания фрагментов в android
        //принадлежит классу а не объекту
        fun newInstance(imagePaths: ArrayList<String>): HalfScreenDialogFragment{

            //создание пустого  bundle
            val arg=Bundle().apply{

                Log.d("BUNDLE","путь до $imagePaths")

                //помещение списка строк по ключу image_paths
                putStringArrayList("image_paths",imagePaths)
                val getPath=getStringArrayList("image_paths")
                Log.d("BUNDLE","Получен путь $getPath")
            }
            //создание фрагмента и прикрепление bundle
            return HalfScreenDialogFragment().apply{
                arguments=arg
            }
            }
            }
    }
