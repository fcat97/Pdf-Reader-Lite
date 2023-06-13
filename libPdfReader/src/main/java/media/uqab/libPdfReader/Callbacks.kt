package media.uqab.libPdfReader

fun interface PageClickCallback { fun onClick(pos: Int) }

fun interface PageChangeCallback { fun onChange(pos: Int) }

fun interface PageScrollCallback { fun onScroll(pos: Int) }