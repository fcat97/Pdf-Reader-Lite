package media.uqab.libPdfReader

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.Exception
import kotlin.math.abs

class PdfReader private constructor(
    private val activity: ComponentActivity,
    private val recyclerView: RecyclerView,
    private val filePath: String,
    private val saveReadingHistory: Boolean = true,
    private val onClickCallback: PageClickCallback?,
    private val onChangeCallback: PageChangeCallback?,
    private val onScrollCallback: PageScrollCallback?
) {
    private var readingMode: ReadingMode = ReadingMode.Continuous
    private var readerAdapter: PdfReaderAdapter
    private var layoutManager: LinearLayoutManager
    private var rendererHelper: PdfRendererHelper? = null
    private val snapHelper: PagerSnapHelper = PagerSnapHelper()
    private val analytics: Analytics = Analytics(activity)
    private val fileName: String get() = filePath.substringAfterLast("/")

    val currentPageNo get() = rendererHelper?.currentPageNumber ?: 0
    val totalPageCount get() = rendererHelper?.pageCount ?: 0

    init {
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.setHasFixedSize(true)

        readerAdapter = PdfReaderAdapter(
            totalPages = { rendererHelper?.pageCount ?: 0 },
            getPage = { p -> rendererHelper?.getBitmap(p) },
            onItemClick = { p -> onClickCallback?.onClick(p) }
        )
        recyclerView.adapter = readerAdapter

        // set onScroll listener
        onScrollCallback?.let {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    layoutManager.let {
                        val pos = detectMostVisibleItemPos()
                        if (pos < 0) return
                        onScrollCallback.onScroll(pos + 1)

                        // save current position and offset
                        if (saveReadingHistory) {
                            val v = recyclerView.getChildAt(0)
                            val topOffset = if (v == null) 0 else v.top - recyclerView.paddingTop
                            Log.d("READER", "onScrollStateChanged: $pos $topOffset")
                            analytics.setLastRead(fileName, pos, topOffset)
                        }
                    }
                }
            })
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // try with uri first
                if (rendererHelper == null) {
                    rendererHelper = try {
                        PdfRendererHelper.getInstance(activity, Uri.parse(filePath))
                    } catch (e: Exception) {
                        PdfRendererHelper.getInstance(activity, filePath)
                    }
                } else {
                    try {
                        rendererHelper?.openRenderer(activity, Uri.parse(filePath))
                    } catch (e: Exception) {
                        rendererHelper?.openRenderer(filePath)
                    }
                }

                // mode to last read position
                if (saveReadingHistory) {
                    try {
                        val pos = analytics.getLastReadPosition(fileName)
                            .coerceAtMost(totalPageCount - 1)
                            .coerceAtLeast(0)
                        val offset = analytics.getLastReadOffset(fileName)
                        layoutManager.scrollToPositionWithOffset(pos, offset)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            if (event == Lifecycle.Event.ON_STOP) {
                rendererHelper?.closeRenderer()
            }
        }
        activity.lifecycle.addObserver(observer)
    }

    @JvmOverloads
    fun jumpTo(page: Int, animate: Boolean = false) {
        val pos = (page - 1)
            .coerceAtMost(totalPageCount - 1)
            .coerceAtLeast(0)

        if (!animate) {
            recyclerView.scrollToPosition(pos)
        } else {
            if (abs(currentPageNo - page) < 5) {
                recyclerView.smoothScrollToPosition(pos)
            } else {
                if (page < currentPageNo) { // moving backward
                    recyclerView.scrollToPosition((pos + 5).coerceAtMost(currentPageNo))
                    recyclerView.smoothScrollToPosition(pos)
                } else if (page > currentPageNo) { // moving forward
                    recyclerView.scrollToPosition((pos - 5).coerceAtLeast(currentPageNo))
                    recyclerView.smoothScrollToPosition(pos)
                } else {
                    recyclerView.scrollToPosition(pos)
                }
            }
        }
    }

    fun changeReadingMode(readingMode: ReadingMode) {
        this.readingMode = readingMode

        val currentPage = currentPageNo

        this.layoutManager = if (readingMode == ReadingMode.SinglePageHorizontal) {
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        }

        recyclerView.layoutManager = layoutManager
        recyclerView.scrollToPosition(currentPage)

        snapHelper.attachToRecyclerView(null)
        if (readingMode != ReadingMode.Continuous) {
            snapHelper.attachToRecyclerView(recyclerView)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setDarkMode(dark: Boolean) {
        rendererHelper?.setDarkMode(dark)
        readerAdapter.notifyDataSetChanged()

        if (dark) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recyclerView.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
            } else {
                recyclerView.background = ColorDrawable(Color.BLACK)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recyclerView.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
                recyclerView.background = ColorDrawable(Color.WHITE)
            }
        }
    }

    private fun detectMostVisibleItemPos(): Int {
        val firstItemPosition = layoutManager.findFirstVisibleItemPosition()
        val secondItemPosition = layoutManager.findLastVisibleItemPosition()

        val mostVisibleItemPosition = if (firstItemPosition == secondItemPosition) {
            firstItemPosition
        } else {
            val firstView = layoutManager.findViewByPosition(firstItemPosition)
            val secondView = layoutManager.findViewByPosition(secondItemPosition)
            try {
                if (abs(firstView!!.top) <= abs(secondView!!.top)) {
                    firstItemPosition
                } else {
                    secondItemPosition
                }
            } catch (e: Exception) {
                firstItemPosition
            }
        }
        return mostVisibleItemPosition
    }

    class Builder(private val activity: ComponentActivity) {
        private var recyclerView: RecyclerView? = null
        private var filePath: String? = null
        private var fileUri: Uri? = null
        private var readingMode: ReadingMode = ReadingMode.Continuous
        private var pageChangeCallback: PageChangeCallback? = null
        private var onClickCallback: PageClickCallback? = null
        private var onScrollCallback: PageScrollCallback? = null
        private var saveReadingHistory: Boolean = true

        fun attachToRecyclerView(recyclerView: RecyclerView): Builder {
            this.recyclerView = recyclerView
            return this
        }

        fun readFrom(filePath: String): Builder {
            this.filePath = filePath
            return this
        }

        fun readFrom(uri: Uri): Builder {
            this.fileUri = uri
            return this
        }

        fun setReadingMode(readingMode: ReadingMode): Builder {
            this.readingMode = readingMode
            return this
        }

        fun saveReadingHistory(save: Boolean) {
            this.saveReadingHistory = save
        }

        fun setOnClickListener(onClickCallback: PageClickCallback): Builder {
            this.onClickCallback = onClickCallback
            return this
        }

        fun setOnPageChangeListener(onChangeCallback: PageChangeCallback): Builder {
            this.pageChangeCallback = onChangeCallback
            return this
        }

        fun setOnPageScrollListener(onScrollCallback: PageScrollCallback): Builder {
            this.onScrollCallback = onScrollCallback
            return this
        }

        fun build(): PdfReader {
            if (recyclerView == null) throw IllegalStateException("No recycler view attached")
            if (filePath == null && fileUri == null) throw IllegalArgumentException("No filepath set!")

            val srcPath = filePath ?: fileUri?.toString() ?: throw IllegalArgumentException("No file set!")

            val pdfReader = PdfReader(
                activity,
                recyclerView!!,
                srcPath,
                saveReadingHistory,
                onClickCallback,
                pageChangeCallback,
                onScrollCallback
            )
            pdfReader.changeReadingMode(readingMode)

            return pdfReader
        }
    }

    enum class ReadingMode {
        Continuous,
        SinglePageHorizontal,
        SinglePageVertical
    }
}