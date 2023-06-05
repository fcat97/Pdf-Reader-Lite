# PDF Reader Android

---

This is a very minimal pdf reader library. If your need is just to open and view a pdf file without increasing the app size, this library may help you. 



How to Use? [![](https://jitpack.io/v/fcat97/pdf-reader-android.svg)](https://jitpack.io/#fcat97/pdf-reader-android)



 Just inport the project with 

```groovy
dependencies {
	implementation 'com.github.fcat97:pdf-reader-android:Tag'
}
```

Now instantiate the `PdfReader` with:

```kotlin
pdfReader = PdfReader.Builder(requireActivity())
                    .attachToRecyclerView(recyclerView)
                    .readFrom(documentUri)
                    .build()
```

All done. 



---

Happy coding 🚀
