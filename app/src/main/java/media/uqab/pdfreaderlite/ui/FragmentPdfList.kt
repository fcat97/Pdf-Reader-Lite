package media.uqab.pdfreaderlite.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import media.uqab.pdfreaderlite.databinding.FragmentPdfListBinding
import java.io.File
import kotlin.math.roundToInt

class FragmentPdfList: Fragment() {
    private lateinit var binding: FragmentPdfListBinding
    private lateinit var adapter: PdfListAdapter
    private var fileJob: Job? = null
    private val pdfFilesLiveData = MutableLiveData<List<PdfData>>(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPdfListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfFilesLiveData.observe(viewLifecycleOwner, this::onFileChange)

        adapter = PdfListAdapter(
            onClick = {

            },
            onDelete = {
                it.srcFile.delete()
                getDownloadedPdf()
            }
        )
        binding.recyclerView.adapter = adapter

        getDownloadedPdf()
    }

    override fun onDestroy() {
        super.onDestroy()
        fileJob?.cancel()
    }

    private fun onFileChange(pdfList: List<PdfData>) {
        binding.noItemText.visibility = if (pdfList.isEmpty()) View.INVISIBLE else View.VISIBLE
        adapter.submitList(pdfList)
    }

    private fun getDownloadedPdf() {
        fileJob?.cancel()
        fileJob = CoroutineScope(Dispatchers.IO).launch {
            val root = requireContext().getExternalFilesDir(null)
            val pdfDir = File(root, DOWNLOAD_DIR)
            if (!pdfDir.exists()) pdfDir.mkdirs()
            val files = pdfDir.listFiles() ?: emptyArray()

            val pdfDataList =  files.toList()
                .filter { it.extension == "pdf" }
                .map {
                    PdfData(it, createThumb(it))
                }

            pdfFilesLiveData.postValue(pdfDataList)
        }
    }

    private fun createThumb(file: File): Bitmap? {
        Log.d(TAG, "createThumb: ${file.path}")
        val fileDesc = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fileDesc)

        if (renderer.pageCount <= 0) return null

        val bitmap = Bitmap.createBitmap(50.dpToPx(), 70.dpToPx(), Bitmap.Config.ARGB_8888)
        val page = renderer.openPage(0)
        page.render(
            bitmap,
            null,
            null,
            RENDER_MODE_FOR_DISPLAY
        )
        page.close()
        renderer.close()

        return bitmap
    }

    private fun Int.dpToPx(): Int {
        val displayMetrics = requireContext().resources.displayMetrics
        return (this * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    data class PdfData(
        val srcFile: File,
        val thumb: Bitmap?
    )

    companion object {
        private const val TAG = "FragmentPdfList"
        const val DOWNLOAD_DIR = "pdf"
    }
}