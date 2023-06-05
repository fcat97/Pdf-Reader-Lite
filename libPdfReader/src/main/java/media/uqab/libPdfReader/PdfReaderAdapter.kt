package media.uqab.libPdfReader

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView

class PdfReaderAdapter(
    val totalPages: () -> Int,
    val getPage: (index: Int) -> Bitmap?,
    val onItemClick: (index: Int) -> Unit
): RecyclerView.Adapter<PdfReaderAdapter.PageHolder>() {

    inner class PageHolder(page: View): RecyclerView.ViewHolder(page) {
        val imageView: PhotoView = page.findViewById<PhotoView?>(R.id.image).apply {
            setZoomTransitionDuration(500);
            isZoomable = true
            minimumScale = 1f
            maximumScale = 5f
            mediumScale = 2.5f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_page, parent, false)
        return PageHolder(view)
    }

    override fun getItemCount(): Int = totalPages()

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.imageView.setImageBitmap(getPage(position))
        holder.imageView.setOnClickListener { onItemClick(position) }
    }
}