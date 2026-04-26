package dev.bimbok.koala.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections

class PdfRendererWrapper(private val context: Context) {

    private val pdfiumCore = PdfiumCore(context)
    private var pdfDocument: PdfDocument? = null
    private val mutex = Mutex()

    // LruCache for rendered pages
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 4 
    private val pageCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted) {
                bitmapPool.add(oldValue)
            }
        }
    }

    private val bitmapPool = Collections.synchronizedSet(mutableSetOf<Bitmap>())

    suspend fun openDocument(uri: Uri): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeDocument()
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw Exception("Cannot open file")
            pdfDocument = pdfiumCore.newDocument(fd)
            pdfiumCore.getPageCount(pdfDocument)
        }
    }

    fun getPageSize(pageIndex: Int): Pair<Int, Int> {
        pdfDocument?.let { doc ->
            pdfiumCore.openPage(doc, pageIndex)
            val width = pdfiumCore.getPageWidthPoint(doc, pageIndex)
            val height = pdfiumCore.getPageHeightPoint(doc, pageIndex)
            return width to height
        }
        return 0 to 0
    }

    /**
     * Renders a PDF page to a Bitmap with a specific target width.
     * This ensures the resolution is absolute for the current screen width or zoom level.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        // Quantize targetWidth to reduce cache entries while maintaining visual quality
        val quantizedWidth = ((targetWidth + 99) / 100) * 100
        val cacheKey = "$pageIndex-$quantizedWidth"
        pageCache.get(cacheKey)?.let { return@withContext it }

        mutex.withLock {
            val doc = pdfDocument ?: return@withContext null
            pdfiumCore.openPage(doc, pageIndex)

            val originalWidth = pdfiumCore.getPageWidthPoint(doc, pageIndex)
            val originalHeight = pdfiumCore.getPageHeightPoint(doc, pageIndex)
            
            // Calculate height maintaining aspect ratio
            val scale = quantizedWidth.toFloat() / originalWidth
            val targetHeight = (originalHeight * scale).toInt()

            // Limit maximum resolution to prevent OOM (4096 is a safe high-res limit for most devices)
            if (quantizedWidth <= 0 || targetHeight <= 0 || quantizedWidth > 4096 || targetHeight > 4096) return@withLock null

            val bitmap = getBitmapFromPool(quantizedWidth, targetHeight)
            pdfiumCore.renderPageBitmap(doc, bitmap, pageIndex, 0, 0, quantizedWidth, targetHeight)

            pageCache.put(cacheKey, bitmap)
            bitmap
        }
    }

    private fun getBitmapFromPool(width: Int, height: Int): Bitmap {
        val iterator = bitmapPool.iterator()
        while (iterator.hasNext()) {
            val bitmap = iterator.next()
            if (bitmap.width == width && bitmap.height == height && bitmap.isMutable) {
                iterator.remove()
                bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
                return bitmap
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun closeDocument() {
        pdfDocument?.let {
            pdfiumCore.closeDocument(it)
            pdfDocument = null
        }
        pageCache.evictAll()
        bitmapPool.clear()
    }
}
