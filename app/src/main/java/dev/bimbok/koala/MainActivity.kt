package dev.bimbok.koala

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bimbok.koala.ui.PdfViewModel
import dev.bimbok.koala.ui.PdfViewerScreen
import dev.bimbok.koala.ui.theme.KoalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            KoalaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PdfViewModel = viewModel()
                    val currentUri by viewModel.currentUri.collectAsState()
                    
                    val filePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            viewModel.loadDocument(it)
                        }
                    }

                    LaunchedEffect(Unit) {
                        // Only handle intent if we don't already have a URI in the ViewModel
                        // This prevents re-triggering the load on every rotation if it's already there
                        if (currentUri == null) {
                            val intentUri = handleIntent(intent)
                            if (intentUri != null) {
                                viewModel.loadDocument(intentUri)
                            } else {
                                // Automatically launch picker if no intent data and no saved URI
                                filePickerLauncher.launch("application/pdf")
                            }
                        }
                    }

                    PdfViewerScreen(
                        viewModel = viewModel,
                        onBack = { finish() },
                        onOpenFile = { filePickerLauncher.launch("application/pdf") }
                    )
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?): Uri? {
        return when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
    }
}
