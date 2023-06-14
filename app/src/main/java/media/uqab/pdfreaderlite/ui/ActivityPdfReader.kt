package media.uqab.pdfreaderlite.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.iterator
import androidx.recyclerview.widget.RecyclerView
import media.uqab.libPdfReader.PdfReader
import media.uqab.pdfreaderlite.R
import java.io.File

class ActivityPdfReader : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var pdfReader: PdfReader

    private var darkModeMenu: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_reader)

        recyclerView = findViewById(R.id.recyclerView)

        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        savedInstanceState?.getInt(CURRENT_PAGE_INDEX_KEY, 0)?.let {
            try {
                pdfReader.jumpTo(it)
            } catch (ignore: UninitializedPropertyAccessException) { }
        }
    }

    override fun onStart() {
        super.onStart()
        val documentPath = intent.extras?.getString(DOCUMENT_PATH_ARGUMENT)
        val documentUri = intent.extras?.getString(DOCUMENT_URI_ARGUMENT)?.toUri()

        try {
            if (!this::pdfReader.isInitialized) {
                if (documentUri != null) {
                    pdfReader = PdfReader.Builder(this)
                        .attachToRecyclerView(recyclerView)
                        .readFrom(documentUri)
                        .setOnPageScrollListener {
                            getSharedPref().edit().putInt("last_read", it).apply()
                        }
                        .build()
                } else if (!documentPath.isNullOrBlank()) {
                    pdfReader = PdfReader.Builder(this)
                        .attachToRecyclerView(recyclerView)
                        .readFrom(documentPath)
                        .setOnPageScrollListener {
                            getSharedPref().edit().putInt("last_read", it).apply()
                        }
                        .build()
                }
            }

            Log.d(TAG, "onCreate: called")
            getSharedPref().getBoolean("dark_mode", false).let {
                Log.d(TAG, "onCreate: $it")
                pdfReader.setDarkMode(it)
                setDarkIconColor()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        darkModeMenu = menu?.findItem(R.id.dark_mode)
        setDarkIconColor()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.dark_mode -> {
                pdfReader.setDarkMode(!pdfReader.isDarkMode())
                getSharedPref().edit().putBoolean("dark_mode", pdfReader.isDarkMode()).apply()
                setDarkIconColor()
            }

            R.id.go_to -> {

            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setDarkIconColor() {
        if (pdfReader.isDarkMode()) {
            darkModeMenu?.icon?.setTint(Color.BLACK)
        } else {
            darkModeMenu?.icon?.setTint(Color.WHITE)
        }
    }

    private fun getSharedPref(): SharedPreferences {
        return getSharedPreferences("reading_pref", Context.MODE_PRIVATE)
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
        fun startThisActivity(context: Context, documentUri: Uri) {
            val i = Intent(context, ActivityPdfReader::class.java)
            i.putExtra(DOCUMENT_URI_ARGUMENT, documentUri.toString())
            context.startActivity(i)
        }

        @JvmStatic
        fun startThisActivity(context: Context, path: File) {
            val i = Intent(context, ActivityPdfReader::class.java)
            i.putExtra(DOCUMENT_PATH_ARGUMENT, path.absolutePath)
            context.startActivity(i)
        }
    }
}

