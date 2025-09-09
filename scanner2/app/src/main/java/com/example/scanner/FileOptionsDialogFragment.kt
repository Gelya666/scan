package com.example.scanner

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment

class FileOptionsDialogFragment: DialogFragment() {
    private lateinit var filename:String
    private var listener:FileOptionsListener?=null

    interface FileOptionsListener {
        fun onRename(filename: String)
        fun onHide(filename: String)
        fun onDownload(filename: String)
        fun onDelete(filename: String)
        fun onReject(filename: String)
    }
    companion object {
        fun newInstance(filename: String): FileOptionsDialogFragment {
            val fragment = FileOptionsDialogFragment()
            val args = Bundle()
            args.putString("filename", filename)
            fragment.arguments = args
            return fragment
        }
    }
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
        {
            filename=arguments?.getString("filename")?:""
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
                            .setPositiveButton("Удалить"){dialog,_->listener?.onDelete(filename)
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

