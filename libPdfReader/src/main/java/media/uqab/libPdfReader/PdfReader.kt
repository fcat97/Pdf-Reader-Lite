package media.uqab.libPdfReader

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
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
    private val scale_1 = 1.5f
    private val scale_2 = 2.0f
    private var scale = 1f

    val currentPageIndex get() = RecyclerViewHelper.getMostVisiblePosition(layoutManager)
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

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    layoutManager.let {
                        if (currentPageIndex < 0) return
                        onScrollCallback.onScroll(currentPageIndex)

                        // save current position and offset
                        /*if (saveReadingHistory) {
                            val v = recyclerView.getChildAt(0)
                            val topOffset = if (v == null) 0 else v.top - recyclerView.paddingTop
                            Log.d("READER", "onScrollStateChanged: $currentPageIndex $topOffset")
                            analytics.setLastRead(fileName, currentPageIndex, topOffset)
                        }*/
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    layoutManager.let {
                        if (currentPageIndex < 0) return
                        onChangeCallback?.onChange(currentPageIndex)

                        // save current position and offset
                        if (saveReadingHistory) {
                            val v = recyclerView.getChildAt(0)
                            val topOffset = if (v == null) 0 else v.top - recyclerView.paddingTop
                            Log.d("READER", "onScrollStateChanged: $currentPageIndex $topOffset")
                            analytics.setLastRead(fileName, currentPageIndex, topOffset)
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
    fun jumpTo(index: Int, animate: Boolean = false) {
        val pos = index.coerceAtMost(totalPageCount - 1)
            .coerceAtLeast(0)

        if (!animate) {
            recyclerView.scrollToPosition(pos)
        } else {
            if (abs(currentPageIndex - pos) < 5) {
                recyclerView.smoothScrollToPosition(pos)
            } else {
                if (pos < currentPageIndex) { // moving backward
                    recyclerView.scrollToPosition((pos + 5).coerceAtMost(currentPageIndex))
                    recyclerView.smoothScrollToPosition(pos)
                } else if (pos > currentPageIndex) { // moving forward
                    recyclerView.scrollToPosition((pos - 5).coerceAtLeast(currentPageIndex))
                    recyclerView.smoothScrollToPosition(pos)
                } else {
                    recyclerView.scrollToPosition(pos)
                }
            }
        }
    }

    fun changeReadingMode(readingMode: ReadingMode) {

        this.readingMode = readingMode

        val currentPage = currentPageIndex

        this.layoutManager = if (readingMode == ReadingMode.SinglePageHorizontal) {
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        }

        recyclerView.layoutManager = layoutManager

        snapHelper.attachToRecyclerView(null)
        if (readingMode != ReadingMode.Continuous) {
            snapHelper.attachToRecyclerView(recyclerView)
        }

        recyclerView.scrollToPosition(currentPage)
    }

    private fun onDoubleClick() {
        scale = when(scale) {
            scale_1 -> scale_2
            scale_2 -> 1f
            else -> scale_1
        }

        scaleView(recyclerView, scale, scale)
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

    private fun scaleView(v: View, startScale: Float, endScale: Float) {
        val anim: Animation = ScaleAnimation(
            1f, 1f,  // Start and end values for the X axis scaling
            startScale, endScale,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0f,  // Pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 1f
        ) // Pivot point of Y scaling
        anim.fillAfter = true // Needed to keep the result of the animation
        anim.duration = 500
        v.startAnimation(anim)
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