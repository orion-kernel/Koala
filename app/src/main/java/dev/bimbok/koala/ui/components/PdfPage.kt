package dev.bimbok.koala.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import dev.bimbok.koala.ui.PdfViewModel
import kotlinx.coroutines.delay

@Composable
fun PdfPage(
    pageIndex: Int,
    viewModel: PdfViewModel,
    globalScale: Float,
) {
    var lowResBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var highResBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val pageSize = remember(pageIndex) { viewModel.getPageSize(pageIndex) }
    val aspectRatio = if (pageSize.first > 0) pageSize.first.toFloat() / pageSize.second else 0.707f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(Color.White)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.toInt()

        // Load base resolution bitmap for current screen width (adapts to Landscape/Portrait)
        LaunchedEffect(pageIndex, widthPx) {
            lowResBitmap = viewModel.getPageBitmap(pageIndex, widthPx)
        }

        // Load high-resolution bitmap for zoomed states
        LaunchedEffect(globalScale, widthPx) {
            if (globalScale > 1.1f) {
                delay(300) // Debounce rendering during active zoom
                val targetZoomWidth = (widthPx * globalScale).toInt()
                val zoomedBtm = viewModel.getPageBitmap(pageIndex, targetZoomWidth)
                if (zoomedBtm != null) {
                    highResBitmap = zoomedBtm
                }
            } else {
                highResBitmap = null
            }
        }

        val displayBitmap = highResBitmap ?: lowResBitmap
        displayBitmap?.let { btm ->
            Image(
                bitmap = btm.asImageBitmap(),
                contentDescription = "Page $pageIndex",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
