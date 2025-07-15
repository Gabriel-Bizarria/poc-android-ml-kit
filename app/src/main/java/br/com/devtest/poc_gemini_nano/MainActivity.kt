package br.com.devtest.poc_gemini_nano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.devtest.poc_gemini_nano.ai.handlers.RewriterHandler
import br.com.devtest.poc_gemini_nano.ui.theme.PocGeminiNanoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val rewriterHandler: RewriterHandler by lazy {
        RewriterHandler(context = this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel by viewModels()
            val textToRewrite = viewModel.textToRewrite.collectAsStateWithLifecycle().value
            PocGeminiNanoTheme {
                ContentContainer(
                    inputText = textToRewrite,
                    onInputTextChange = {
                        viewModel.setTextToRewrite(it)
                    },
                    rewriterHandler = rewriterHandler,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rewriterHandler.closeRewriter()
    }
}

@Composable
fun ContentContainer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    rewriterHandler: RewriterHandler? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val rewrittenText = rewriterHandler?.inferenceResult?.collectAsStateWithLifecycle(initialValue = "")?.value

    Column(
        modifier = modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    rewriterHandler?.prepareAndStartProcessing(textToRewrite = inputText)
                }
            }
        ) {
            Text("Rewrite")
        }

        Text(
            text = rewrittenText.orEmpty()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenPreview() {
    PocGeminiNanoTheme {
        ContentContainer(
            inputText = "An other day, .I was going to the store and I see her",
            onInputTextChange = {},
        )
    }
}
