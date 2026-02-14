package com.example.scanner.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.scanner.R

class FileOptionsDialogFragment: DialogFragment() {
    private lateinit var filename:String
    private var listener:FileOptionsListener?=null
    private var position=-1

    interface FileOptionsListener {
        fun onRename(filename: String)
        fun onHide(filename: String)
        fun onDownload(filename: String)
        fun onDelete(filename: String,position:Int)
        fun onReject(filename: String)
    }
    companion object {
        fun newInstance(filename: String?, position: Int): FileOptionsDialogFragment {
            val fragment = FileOptionsDialogFragment()
            val args = Bundle()
            args.putString("filename", filename)
            args.putInt("position",position)
            fragment.arguments = args
            return fragment
        }
    }
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
        {
            filename=arguments?.getString("filename")?:""
            position = arguments?.getInt("position", -1) ?: -1
            val view=layoutInflater.inflate(R.layout.dialog_file_options,null)
            val builder= AlertDialog.Builder(requireActivity())
           .setTitle("Выберите опцию")
           .setView(view)
           .setNegativeButton("Закрыть"){dialog,_->dialog.dismiss()}
            return builder.create()
        }
            override fun onStart()
            {
                super.onStart()
                setupButtons()
            }
                private fun setupButtons()
                {
                    dialog?.findViewById<Button>(R.id.btn_rename)?.setOnClickListener{
                        listener?.onRename(filename)
                        dismiss()
                    }
                        dialog?.findViewById<Button>(R.id.btn_hide)?.setOnClickListener{
                            listener?.onHide(filename)
                            dismiss()
                        }
                            dialog?.findViewById<Button>(R.id.btn_download)?.setOnClickListener {
                                listener?.onDownload(filename)
                                dismiss()
                            }
                                dialog?.findViewById<Button>(R.id.btn_delete)?.setOnClickListener {
                                    showDeleteConfirmation()
                                }
                                    dialog?.findViewById<Button>(R.id.btn_reject)?.setOnClickListener {
                                        listener?.onReject(filename)
                                        dismiss()
                                    }
                }
                    private fun showDeleteConfirmation()
                    {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Подтверждение удаления")
                            .setMessage("Удалить файл $filename?")
                            .setPositiveButton("Удалить"){dialog,_->
                                //listener?.onDelete(filename,position)
                                listener?.let {
                                    Log.d("angel", "Listener вызывается!")
                                    Log.d("angel", "Filename: $filename")
                                    Log.d("angel", "Position: $position")
                                    Log.d("angel", "Listener class: ${it.javaClass.simpleName}")

                                    it.onDelete(filename, position)  // Сам вызов
                                }
                                dialog.dismiss()
                                dismiss()
                            }
                            .setNegativeButton("Отмена"){dialog,_->dismiss()}
                            .show()
                    }
                        override fun onAttach(context:Context)
                        {
                            super.onAttach(context)
                            if(context is FileOptionsListener){
                                listener=context
                            }
                        }
}

