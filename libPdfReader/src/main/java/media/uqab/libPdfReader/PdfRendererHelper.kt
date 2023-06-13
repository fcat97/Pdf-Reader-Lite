package media.uqab.libPdfReader

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A helper class on top of android's [PdfRenderer]
 * to view pdf file into image view.
 *
 * __How To Use__
 *
 * - To instantiate use [getInstance] method. This will open the pdf file internally.
 * - To open pdf file on an existing instance, use [openRenderer] method.
 * - To close the renderer in decomposition e.g Activity's onStop(), onDestroy() etc.
 * use [closeRenderer] method.
 *
 * @author github/fCat97
 */
internal class PdfRendererHelper private constructor(private val context: Context) {
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var currentPage: PdfRenderer.Page
    private var isDarkMode: Boolean = false

    var currentPageIndex: Int = INITIAL_PAGE_INDEX

    /**
     * Total pages in the PDF
     */
    val pageCount get() = pdfRenderer.pageCount

    /**
     * Get [Bitmap] from the specified page of PDF scaled to screen resolution.
     *
     * The way [PdfRenderer] works is that it allows for "opening" a page with the method
     * [PdfRenderer.openPage], which takes a (0 based) page number to open. This returns
     * a [PdfRenderer.Page] object, which represents the content of this page.
     *
     * There are two ways to render the content of a [PdfRenderer.Page].
     * [PdfRenderer.Page.RENDER_MODE_FOR_PRINT] and [PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY].
     * Since we're displaying the data on the screen of the device, we'll use the later.
     *
     * @param index The page index.
     * @return [Bitmap] image of that page.
     *
     * @throws IndexOutOfBoundsException if [index] < 0 or [index] > [pageCount].
     */
    fun getBitmap(index: Int): Bitmap {
        if (index < 0 || index >= pageCount) throw IndexOutOfBoundsException()

        currentPage.close()
        currentPageIndex = index
        currentPage = pdfRenderer.openPage(index)

        // Important: the destination bitmap must be ARGB (not RGB).
        val bitmap = Bitmap.createBitmap(
            (currentPage.width * getScale(currentPage)).roundToInt(),
            (currentPage.height * getScale(currentPage)).roundToInt(),
            Bitmap.Config.ARGB_8888
        )

        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        return if (isDarkMode) {
            changeBitmap(bitmap, Color.BLACK, INVERT, -1)
        } else bitmap
    }

    /**
     * Sets up a [PdfRenderer] and related resources.
     */
    @Throws(IOException::class)
    fun openRenderer(context: Context?, documentUri: Uri) {
        if (context == null) return

        /**
         * It may be tempting to use `use` here, but [PdfRenderer] expects to take ownership
         * of the [FileDescriptor], and, if we did use `use`, it would be auto-closed at the
         * end of the block, preventing us from rendering additional pages.
         */
        val fileDescriptor = context.contentResolver.openFileDescriptor(documentUri, "r") ?: return

        // This is the PdfRenderer we use to render the PDF.
        pdfRenderer = PdfRenderer(fileDescriptor)
        currentPage = pdfRenderer.openPage(currentPageIndex)
    }

    @Throws(IOException::class, SecurityException::class)
    fun openRenderer(documentPath: String) {
        val file = File(documentPath)
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor)
        currentPage = pdfRenderer.openPage(currentPageIndex)
    }

    /**
     * Closes the [PdfRenderer] and related resources.
     *
     * @throws IOException When the PDF file cannot be closed.
     */
    @Throws(IOException::class)
    fun closeRenderer() {
        currentPage.close()
        pdfRenderer.close()
    }

    fun setDarkMode(darkMode: Boolean) {
        this.isDarkMode = darkMode
    }

    private fun getScale(p: PdfRenderer.Page): Float {
        val displayMetrics = DisplayMetrics()

        val ws = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ws.defaultDisplay.getMetrics(displayMetrics)

        val scale = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            1f * displayMetrics.widthPixels / p.width
        } else {
            1f * min(displayMetrics.widthPixels, displayMetrics.heightPixels) / min(p.width, p.height)
        }

        /*Log.i(
            TAG, "getScale: \n" + """
            orientation :   ${Configuration.ORIENTATION_LANDSCAPE} (2 == landscape)
            resolution  :   ${displayMetrics.widthPixels} x ${displayMetrics.heightPixels}
            density     :   ${displayMetrics.density} ${displayMetrics.densityDpi}dpi
            page dimen  :   ${p.width} x ${p.height}
            scale       :   $scale
        """.trimIndent())*/
        return scale
    }

    private fun changeBitmap(src: Bitmap, bgColor: Int, colorFilter: FloatArray, saturation: Int): Bitmap {
        val height = src.height
        val width = src.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)
        val paint = Paint()
        val matrixGrayscale = ColorMatrix()
        if (saturation != -1) {
            // saturation = 0 => means icon will be black and white
            matrixGrayscale.setSaturation(saturation.toFloat())
        }

        // Darker Gray Mapping
        val matrixInvert = ColorMatrix(colorFilter)
        matrixInvert.preConcat(matrixGrayscale)
        val filter = ColorMatrixColorFilter(matrixInvert)
        paint.colorFilter = filter
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bitmap
    }

    private val orientation: Int get() = context.resources.configuration.orientation

    companion object {
        private const val TAG = "PdfRendererHelper"
        private const val INITIAL_PAGE_INDEX = 0

        private val INVERT = floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )

        @JvmStatic
        @Throws(IOException::class)
        fun getInstance(context: Context, documentUri: Uri): PdfRendererHelper {
            val renderer = PdfRendererHelper(context)
            renderer.openRenderer(context, documentUri)

            return renderer
        }

        @JvmStatic
        @Throws(IOException::class, SecurityException::class)
        fun getInstance(context: Context, documentPath: String): PdfRendererHelper {
            val renderer = PdfRendererHelper(context)
            renderer.openRenderer(documentPath)
            return renderer
        }
    }
}