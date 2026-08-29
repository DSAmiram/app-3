package com.example.pdfsearch

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * Renders each PDF page lazily to a bitmap using the platform's PdfRenderer.
 * Only one page may be open on a PdfRenderer at a time, so rendering is
 * synchronized on [renderer].
 */
class PdfPageAdapter(
    private val renderer: PdfRenderer,
    private val targetWidthPx: Int
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    class PageViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imgPage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val bitmap = renderPage(position)
        holder.imageView.setImageBitmap(bitmap)
    }

    private fun renderPage(index: Int): Bitmap {
        synchronized(renderer) {
            renderer.openPage(index).use { page ->
                val scale = targetWidthPx.toFloat() / page.width
                val width = targetWidthPx
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }

    override fun getItemCount(): Int = renderer.pageCount
}
