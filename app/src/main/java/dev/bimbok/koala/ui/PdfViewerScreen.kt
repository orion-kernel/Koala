package dev.bimbok.koala.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bimbok.koala.ui.components.PdfPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: PdfViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenFile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode = isSystemInDarkTheme()
    val currentPage by viewModel.currentPage.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    
    val scope = rememberCoroutineScope()
    var showOverlay by remember { mutableStateOf(true) }
    
    // Global Zoom State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF121212) else Color(0xFF424242))
    ) {
        when (val state = uiState) {
            is PdfUiState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onOpenFile) {
                        Text("Open PDF")
                    }
                }
            }
            is PdfUiState.Ready -> {
                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = currentPage
                )
                
                // Sync current page with scroll position
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    viewModel.updateCurrentPage(listState.firstVisibleItemIndex)
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxWidth = constraints.maxWidth.toFloat()
                    val maxHeight = constraints.maxHeight.toFloat()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showOverlay = !showOverlay }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val oldScale = scale
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    
                                    if (scale > 1f) {
                                        val factor = scale / oldScale
                                        
                                        // Handle horizontal panning with bounds
                                        val maxOffsetX = (maxWidth * (scale - 1f)) / 2f
                                        val newOffsetX = (offset.x - centroid.x) * factor + centroid.x + pan.x
                                        
                                        // Handle vertical panning: Local translation + list scrolling
                                        val maxOffsetY = (maxHeight * (scale - 1f)) / 2f
                                        val potentialOffsetY = (offset.y - centroid.y) * factor + centroid.y + pan.y
                                        val cappedOffsetY = potentialOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                        
                                        offset = Offset(
                                            x = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
                                            y = cappedOffsetY
                                        )

                                        // If panning exceeds local zoomed bounds, scroll the list
                                        val excessY = potentialOffsetY - cappedOffsetY
                                        if (kotlin.math.abs(excessY) > 0.5f) {
                                            scope.launch {
                                                listState.scrollBy(-excessY / scale)
                                            }
                                        }
                                    } else {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                }
                            }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp),
                            userScrollEnabled = scale == 1f
                        ) {
                            items(state.pageCount) { index ->
                                PdfPage(
                                    pageIndex = index,
                                    viewModel = viewModel,
                                    globalScale = scale
                                )
                                if (index < state.pageCount - 1) {
                                    Spacer(modifier = Modifier.height(12.dp).fillMaxWidth().background(Color.Transparent))
                                }
                            }
                        }
                    }

                    // UI Overlays
                    AnimatedVisibility(
                        visible = showOverlay,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text("Koala", fontWeight = FontWeight.Bold)
                                        if (fileName.isNotEmpty()) {
                                            Text(
                                                fileName,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onBack) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Black.copy(alpha = 0.85f),
                                    titleContentColor = Color.White,
                                    navigationIconContentColor = Color.White,
                                    actionIconContentColor = Color.White
                                )
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Page ${currentPage + 1} / ${state.pageCount}",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Slider(
                                        value = currentPage.toFloat(),
                                        onValueChange = { newValue ->
                                            scope.launch {
                                                listState.scrollToItem(newValue.toInt())
                                            }
                                        },
                                        valueRange = 0f..(state.pageCount - 1).coerceAtLeast(0).toFloat(),
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is PdfUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}
