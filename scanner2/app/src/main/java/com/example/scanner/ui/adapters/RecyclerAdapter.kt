package com.example.scanner.ui.adapters
import android.app.Activity
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.R
import com.example.scanner.ui.fragments.OnFileClickListener
import com.example.scanner.viewmodel.PdfFile

class RecyclerAdapter(private val activity: Activity, val pdfFiles: List<PdfFile>, private val listener: OnFileClickListener): RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
     parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.recycle_view_item, parent, false)
        return MyViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val pdfFile = pdfFiles[position]
        holder.pdfName.text = pdfFile.pdfName
        holder.pdfSize.text = pdfFile.pdfSize
        holder.pdfCreateDate.text = pdfFile.pdfDate
        // holder.imagePdfFile.setImageURI(pdfFile.PathPdfFile)
        loadPdfPreviewImage(pdfFile.PathPdfFile, holder.imagePdfFile)
        holder.itemView.setOnClickListener {
            listener.onItemClick(pdfFile)
        }
        holder.point.setOnClickListener {
            listener.onFileClick(position, pdfFile.pdfName!!, pdfFile)
        }

        holder.imagePdfFile.post {
            Log.d("VIEW", "imageView: ${holder.imagePdfFile.width} x ${holder.imagePdfFile.height}")
            loadPdfPreviewImage(pdfFile.PathPdfFile, holder.imagePdfFile)
        }
        holder.shareFile.setOnClickListener{
            listener.sharePdfFile(pdfFile)
        }
        }

    override fun getItemCount(): Int {
        return pdfFiles.size
    }
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pdfName: TextView = itemView.findViewById(R.id.textPdfName)
        val imagePdfFile: ImageView = itemView.findViewById(R.id.iconPdfFirstScreen)
        val pdfSize: TextView = itemView.findViewById(R.id.textSize)
        val pdfCreateDate: TextView = itemView.findViewById(R.id.textDate)
        val point: ImageButton = itemView.findViewById(R.id.points)
        val shareFile:ImageButton=itemView.findViewById(R.id.download)
    }
    private fun loadPdfPreviewImage(uri: Uri?, imageView: ImageView) {
        try {
            if (uri != null) {
                val parcelFileDescriptor = imageView.context.contentResolver
                    .openFileDescriptor(uri, "r")

                parcelFileDescriptor?.use { descriptor ->
                    val pdfRenderer = PdfRenderer(descriptor)

                    // Открываем первую страницу (индекс 0)
                    if (pdfRenderer.pageCount > 0) {
                        val page = pdfRenderer.openPage(0)

                        //create
                        val tempBitmap = createBitmap(page.width, page.height)

                        //write
                        val cropPercent = 0.00f  // 5%
                        val left = (page.width * cropPercent).toInt()
                        val top = (page.height * cropPercent).toInt()
                        val right = page.width - left
                        val bottom = page.height - top
                        val cropRect = Rect(left, top, right, bottom)

                        tempBitmap.eraseColor(0xFFFFFFFF.toInt())
                        page.render(tempBitmap, cropRect, null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        //read
                        imageView.setImageBitmap(tempBitmap)
                        imageView.scaleType = ImageView.ScaleType.FIT_XY
                        imageView.invalidate()
                       page.close()
                    }
                    pdfRenderer.close()
                }
            } else {
                imageView.setImageResource(R.drawable.divider)
            }
        }catch (e: Exception) {
            imageView.setImageResource(R.drawable.divider)
        }
    }
}