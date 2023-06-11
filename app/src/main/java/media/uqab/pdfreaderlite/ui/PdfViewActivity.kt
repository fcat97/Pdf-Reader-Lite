package media.uqab.pdfreaderlite.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import media.uqab.pdfreaderlite.databinding.ActivityPdfViewBinding
import java.io.File


class PdfViewActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewBinding

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result == null) return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        /*result.data?.data?.let {
            val f = FileHelper.getFile(this, it) ?: return@let

            val pdfModel = PdfModel(f.name, "subtitle", f.parent + "/", f.name, "test", "")
            openPDF(this, pdfModel)
        }*/
        result.data?.data?.let { openPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            binding.fileSelectorBtn.visibility = View.GONE
        }

        binding.fileSelectorBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/pdf"

            openDocumentLauncher.launch(intent)
        }

        openPdfListFragment()
    }

    private fun openPdfListFragment() {
        val fragment = FragmentPdfList()
        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainerView.id, fragment)
            .commit()
    }

    private fun openPdf(file: File) {
        val fragment = OpenDocumentFragment.newInstance(file)
        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainerView.id, fragment)
            .commit()

        binding.fileSelectorBtn.visibility = View.GONE
    }

    private fun openPdf(uri: Uri) {
        val fragment = OpenDocumentFragment.newInstance(uri)
        supportFragmentManager.beginTransaction()
            .add(binding.fragmentContainerView.id, fragment)
            .commit()

        binding.fileSelectorBtn.visibility = View.GONE
    }
}