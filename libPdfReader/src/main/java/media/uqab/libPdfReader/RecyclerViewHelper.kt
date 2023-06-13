package media.uqab.libPdfReader

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

object RecyclerViewHelper {
    private const val TAG = "RecyclerViewHelper"

    fun getMostVisiblePosition(layoutManager: LinearLayoutManager): Int {
        var detectMostVisibleItemPos = getCompleteVisiblePosition(layoutManager)

        Log.d(TAG, "detectMostVisibleItemPos1: $detectMostVisibleItemPos")

        if (detectMostVisibleItemPos == RecyclerView.NO_POSITION) {
            detectMostVisibleItemPos = detectMostVisibleItemPos(layoutManager)
            Log.d(TAG, "detectMostVisibleItemPos2: $detectMostVisibleItemPos")
        }

        if (detectMostVisibleItemPos == RecyclerView.NO_POSITION) {
            detectMostVisibleItemPos = getLastReadPosition(layoutManager)
            Log.d(TAG, "detectMostVisibleItemPos3: $detectMostVisibleItemPos")
        }

        return detectMostVisibleItemPos
    }

    private fun detectMostVisibleItemPos(layoutManager: LinearLayoutManager): Int {
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

    private fun getLastReadPosition(layoutManager: LinearLayoutManager): Int {
        return layoutManager.findFirstVisibleItemPosition()
    }

    private fun getCompleteVisiblePosition(layoutManager: LinearLayoutManager): Int {
        return layoutManager.findFirstCompletelyVisibleItemPosition()
    }
}