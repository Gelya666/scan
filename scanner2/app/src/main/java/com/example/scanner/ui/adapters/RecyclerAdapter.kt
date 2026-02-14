package com.example.scanner.ui.adapters
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.R
import com.example.scanner.ui.fragments.OnFileClickListener
import com.example.scanner.viewmodel.PdfFile

class RecyclerAdapter(private val activity: Activity, val pdfFiles: List<PdfFile>, private val listener: OnFileClickListener): RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val itemView=LayoutInflater.from(parent.context).inflate(R.layout.recycle_view_item,parent,false)
        return MyViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int){

        val pdfFile=pdfFiles[position]
        holder.pdfName.text =pdfFile.pdfName
        holder.pdfSize.text=pdfFile.pdfSize
        holder.pdfCreateDate.text=pdfFile.pdfDate
        holder.imagePdfFile.setImageURI(pdfFile.PathPdfFile)
        holder.point.setOnClickListener{
           listener.onFileClick(position, pdfFile.pdfName!!,pdfFile)
        }
    }
    override fun getItemCount(): Int {
        return pdfFiles.size
    }
    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val pdfName: TextView = itemView.findViewById(R.id.textPdfName)
        val imagePdfFile: ImageView =itemView.findViewById(R.id.iconPdfFirstScreen)
        val pdfSize:TextView=itemView.findViewById(R.id.textSize)
        val pdfCreateDate:TextView=itemView.findViewById(R.id.textDate)
        val point: ImageButton =itemView.findViewById(R.id.points)
    }
}