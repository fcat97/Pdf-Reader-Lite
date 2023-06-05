package media.uqab.libPdfReader

fun interface PageClickCallback { fun onClick(page: Int) }

fun interface PageChangeCallback { fun onChange(page: Int) }

fun interface PageScrollCallback { fun onScroll(page: Int) }