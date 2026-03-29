package com.example.scanner.ui.fragments

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.scanner.R
import com.example.scanner.ui.activities.PagesEditor.PdfPagesEditorActivity
import com.example.scanner.ui.activities.PagesEditor.StateData
import java.io.File

class HalfScreenDialogFragment() : DialogFragment()
{
    //переменная для хранения ссылки на объект реализующий интерфейс
    private var pdfSaveListener: PdfSaveListener? = null
    private lateinit var emailSender: EmailSender
    private lateinit var sharedStateData: StateData
    private lateinit var previewHelper: ImagePreviewHelper
    private lateinit var imageShareHelper: ImageShareHelper


    //список путей к фотографиям

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //создание view из макета и помещание в родительский контейнер
        return inflater.inflate(R.layout.dialog_half_screen, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emailSender= EmailSender(this)
        val imagePreview=view.findViewById<ImageView>(R.id.image_photo)
        previewHelper= ImagePreviewHelper(imagePreview,android.R.drawable.ic_delete)
        val photoPaths=sharedStateData.getPhotoPaths()
        previewHelper.loadFirstPreview(photoPaths)
        imageShareHelper= ImageShareHelper(requireContext())

        //вызов функции для обработки всх кликов
        setupClickListeners(view)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
      if(context is PdfPagesEditorActivity){
          sharedStateData=context.stateData
      }
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
            val path = it.getStringArrayList("image_paths") ?: ArrayList()
            sharedStateData.setPhotoPaths(path)
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
            //if(photoPaths.isEmpty()){
              if(sharedStateData.isPhotoPathsEmpty()){
                val testFile= File(requireContext().filesDir,"text.jpg")
                if(!testFile.exists()){
                    testFile.createNewFile()
                }
                  val updatedPaths=sharedStateData.getPhotoPaths()
                  previewHelper.loadFirstPreview(updatedPaths)
                //photoPaths.add(testFile.absolutePath)
                  sharedStateData.photoPathsAddFile(testFile)
            }
            //emailSender.sendMultipleImages(photoPaths)
            emailSender.sendMultipleImages(sharedStateData.getPhotoPaths())

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
        val photoPath=sharedStateData.getPhotoPaths()
        imageShareHelper.shareImages(photoPath)
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

                //помещение списка строк по ключу image_paths
                putStringArrayList("image_paths",imagePaths)
            }
            //создание фрагмента и прикрепление bundle
            return HalfScreenDialogFragment().apply{
                arguments=arg
            }
            }
            }
    }
