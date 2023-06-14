package media.uqab.libPdfReader

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import kotlin.math.abs

object RecyclerViewHelper {

    fun LinearLayoutManager.getMostVisiblePosition(): Int {
        var detectMostVisibleItemPos = findFirstCompletelyVisibleItemPosition()

        if (detectMostVisibleItemPos == RecyclerView.NO_POSITION) {
            detectMostVisibleItemPos = detectMostVisibleItemPos()
        }

        if (detectMostVisibleItemPos == RecyclerView.NO_POSITION) {
            detectMostVisibleItemPos = findFirstVisibleItemPosition()
        }

        return detectMostVisibleItemPos
    }

    private fun LinearLayoutManager.detectMostVisibleItemPos(): Int {
        val firstItemPosition = findFirstVisibleItemPosition()
        val secondItemPosition = findLastVisibleItemPosition()

        val mostVisibleItemPosition = if (firstItemPosition == secondItemPosition) {
            firstItemPosition
        } else {
            val firstView = findViewByPosition(firstItemPosition)
            val secondView = findViewByPosition(secondItemPosition)
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
}