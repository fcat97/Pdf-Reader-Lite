package media.uqab.pdfreaderlite.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import media.uqab.pdfreaderlite.R

class PdfListAdapter(
    private val onClick: (FragmentPdfList.PdfData) -> Unit,
    private val onDelete: (FragmentPdfList.PdfData) -> Unit
): ListAdapter<FragmentPdfList.PdfData, PdfListAdapter.ItemHolder>(DIFF_CALLBACK) {

    class ItemHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.thumb)
        val title: TextView = itemView.findViewById(R.id.title)
        val location: TextView = itemView.findViewById(R.id.location)
        val delete: ImageView = itemView.findViewById(R.id.delete)
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.relative_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        val rootView = inflater.inflate(R.layout.item_pdf, parent, false)
        return ItemHolder(rootView)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val item = getItem(position)
        holder.thumb.setImageBitmap(item.thumb)
        holder.title.text = item.srcFile.nameWithoutExtension
        holder.location.text = item.srcFile.path
        holder.delete.setOnClickListener { onDelete(item) }
        holder.relativeLayout.setOnClickListener { onClick(item) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FragmentPdfList.PdfData>() {
            override fun areItemsTheSame(
                oldItem: FragmentPdfList.PdfData,
                newItem: FragmentPdfList.PdfData
            ): Boolean {
                return oldItem.srcFile.path == newItem.srcFile.path
            }

            override fun areContentsTheSame(
                oldItem: FragmentPdfList.PdfData,
                newItem: FragmentPdfList.PdfData
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}