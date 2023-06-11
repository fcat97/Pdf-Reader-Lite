package media.uqab.libPdfReader

import android.content.Context
import android.content.SharedPreferences

internal class Analytics(private val context: Context) {
    companion object {
        private const val ANALYTICS_PREF = "pdf_reader_analytics"
        private const val KEY_LAST_READ_FILE = "last_read"
        private const val KEY_LAST_PAGE = "last_page"
        private const val KEY_PAGE_OFFSET = "offset"
    }

    private fun getPref(): SharedPreferences {
        return context.getSharedPreferences(ANALYTICS_PREF, Context.MODE_PRIVATE)
    }

    fun isLastRead(fileName: String): Boolean {
        val lastRead = getPref().getString(KEY_LAST_READ_FILE, null)
        return if (! lastRead.isNullOrBlank()) {
            lastRead == fileName
        } else false
    }

    fun getLastReadPage(fileName: String): Int {
        return if (isLastRead(fileName)) {
            getPref().getInt(KEY_LAST_PAGE, 0)
        } else 0
    }

    fun getLastReadOffset(fileName: String): Int {
        return if (isLastRead(fileName)) {
            getPref().getInt(KEY_PAGE_OFFSET, 0)
        } else 0
    }

    fun setLastRead(fileName: String, page: Int, offset: Int) {
        val sp = getPref().edit()
        sp.putString(KEY_LAST_READ_FILE, fileName)
        sp.putInt(KEY_LAST_PAGE, page)
        sp.putInt(KEY_PAGE_OFFSET, offset)
        sp.apply()
    }
}