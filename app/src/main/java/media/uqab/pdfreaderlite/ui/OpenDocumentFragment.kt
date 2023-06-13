package media.uqab.pdfreaderlite.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import media.uqab.libPdfReader.PdfReader
import media.uqab.pdfreaderlite.R
import java.io.File

class OpenDocumentFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var pdfReader: PdfReader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pdf_renderer_basic, container, false).apply {
        recyclerView = findViewById(R.id.recyclerView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val documentPath = arguments?.getString(DOCUMENT_PATH_ARGUMENT)
        val documentUri = arguments?.getString(DOCUMENT_URI_ARGUMENT)?.toUri()

        try {
            if (!this::pdfReader.isInitialized) {
                if (documentUri != null) {
                    pdfReader = PdfReader.Builder(requireActivity())
                        .attachToRecyclerView(recyclerView)
                        .readFrom(documentUri)
                        .setOnPageScrollListener {
                            getSharedPref().edit().putInt("last_read", it).apply()
                        }
                        .build()
                } else if (!documentPath.isNullOrBlank()) {
                    pdfReader = PdfReader.Builder(requireActivity())
                        .attachToRecyclerView(recyclerView)
                        .readFrom(documentPath)
                        .setOnPageScrollListener {
                            getSharedPref().edit().putInt("last_read", it).apply()
                        }
                        .build()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        savedInstanceState?.getInt(CURRENT_PAGE_INDEX_KEY, 0)?.let {
            try {
                pdfReader.jumpTo(it)
            } catch (ignore: UninitializedPropertyAccessException) { }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::pdfReader.isInitialized) {
            // https://stackoverflow.com/questions/36426129/recyclerview-scroll-to-position-not-working-every-time
            CoroutineScope(Dispatchers.Main).launch {
                delay(200)
                val lastRead = getSharedPref().getInt("last_read", 0)
                pdfReader.jumpTo(lastRead)
            }
        }
    }

    private fun getSharedPref(): SharedPreferences {
        return requireContext().getSharedPreferences("reading_pref", Context.MODE_PRIVATE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE_INDEX_KEY, pdfReader.currentPageIndex)
        super.onSaveInstanceState(outState)
    }


    companion object {
        private const val CURRENT_PAGE_INDEX_KEY = "media.uqab.pdfjsandroid.state.CURRENT_PAGE_INDEX_KEY"
        private const val DOCUMENT_URI_ARGUMENT = "media.uqab.pdfjsandroid.args.DOCUMENT_URI_ARGUMENT"
        private const val DOCUMENT_PATH_ARGUMENT = "media.uqab.pdfjsandroid.args.DOCUMENT_PATH_ARGUMENT"
        private const val TAG = "OpenDocumentFragment"

        @JvmStatic
        fun newInstance(documentUri: Uri): OpenDocumentFragment {

            return OpenDocumentFragment().apply {
                arguments = Bundle().apply {
                    putString(DOCUMENT_URI_ARGUMENT, documentUri.toString())
                }
            }
        }

        @JvmStatic
        fun newInstance(path: File): OpenDocumentFragment {
            return OpenDocumentFragment().apply {
                arguments = Bundle().apply {
                    putString(DOCUMENT_PATH_ARGUMENT, path.absolutePath)
                }
            }
        }
    }
}

