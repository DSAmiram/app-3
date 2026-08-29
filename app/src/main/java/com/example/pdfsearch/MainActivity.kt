package com.example.pdfsearch

import android.content.res.Resources
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Name of the PDF bundled inside app/src/main/assets */
private const val ASSET_PDF_NAME = "gabrist.pdf"

class MainActivity : AppCompatActivity() {

    private lateinit var pdfRecycler: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var txtStatus: TextView
    private lateinit var recyclerResults: RecyclerView

    private var pdfRenderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null

    // cached page texts so repeated searches don't re-extract every time
    private var pageTexts: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Required once, before any PdfBox usage
        PDFBoxResourceLoader.init(applicationContext)

        pdfRecycler = findViewById(R.id.pdfRecycler)
        editSearch = findViewById(R.id.editSearch)
        btnSearch = findViewById(R.id.btnSearch)
        txtStatus = findViewById(R.id.txtStatus)
        recyclerResults = findViewById(R.id.recyclerResults)

        pdfRecycler.layoutManager = LinearLayoutManager(this)
        recyclerResults.layoutManager = LinearLayoutManager(this)

        loadBundledPdf()

        btnSearch.setOnClickListener {
            val query = editSearch.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "یه عبارت برای جست‌وجو وارد کن", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSearch(query)
        }
    }

    private fun loadBundledPdf() {
        try {
            // PdfRenderer needs a real file descriptor, so copy the asset into cache first.
            val outFile = File(cacheDir, ASSET_PDF_NAME)
            if (!outFile.exists()) {
                assets.open(ASSET_PDF_NAME).use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
            }

            pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd!!)
            pdfRenderer = renderer

            val screenWidthPx = Resources.getSystem().displayMetrics.widthPixels
            pdfRecycler.adapter = PdfPageAdapter(renderer, screenWidthPx)
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در باز کردن PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun performSearch(query: String) {
        txtStatus.visibility = View.VISIBLE
        txtStatus.text = "در حال جست‌وجو..."
        recyclerResults.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val texts = pageTexts ?: withContext(Dispatchers.IO) { extractPageTexts() }
                pageTexts = texts

                val results = withContext(Dispatchers.Default) { search(texts, query) }

                if (results.isEmpty()) {
                    txtStatus.text = "چیزی برای «$query» پیدا نشد"
                    recyclerResults.visibility = View.GONE
                } else {
                    txtStatus.text = "${results.size} نتیجه پیدا شد"
                    recyclerResults.visibility = View.VISIBLE
                    recyclerResults.adapter = ResultsAdapter(results) { hit ->
                        pdfRecycler.scrollToPosition(hit.pageIndex)
                        recyclerResults.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                txtStatus.text = "خطا در جست‌وجو: ${e.message}"
            }
        }
    }

    /** Extracts text page-by-page from the bundled asset PDF (via PdfBox, independent of PdfRenderer). */
    private fun extractPageTexts(): List<String> {
        assets.open(ASSET_PDF_NAME).use { stream ->
            PDDocument.load(stream).use { document ->
                val pageCount = document.numberOfPages
                val stripper = PDFTextStripper()
                val texts = ArrayList<String>(pageCount)
                for (i in 1..pageCount) {
                    stripper.startPage = i
                    stripper.endPage = i
                    texts.add(stripper.getText(document))
                }
                return texts
            }
        }
    }

    private fun search(pageTexts: List<String>, query: String): List<SearchResult> {
        val results = ArrayList<SearchResult>()
        val lowerQuery = query.lowercase()

        pageTexts.forEachIndexed { pageIndex, text ->
            val lowerText = text.lowercase()
            var fromIndex = 0
            while (true) {
                val matchIndex = lowerText.indexOf(lowerQuery, fromIndex)
                if (matchIndex == -1) break

                val start = maxOf(0, matchIndex - 40)
                val end = minOf(text.length, matchIndex + query.length + 40)
                val snippet = (if (start > 0) "…" else "") +
                        text.substring(start, end).replace("\n", " ").trim() +
                        (if (end < text.length) "…" else "")

                results.add(SearchResult(pageIndex, snippet))
                fromIndex = matchIndex + query.length

                // avoid flooding results with many hits on the same page
                if (results.count { it.pageIndex == pageIndex } >= 3) break
            }
        }
        return results
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        pfd?.close()
    }
}
