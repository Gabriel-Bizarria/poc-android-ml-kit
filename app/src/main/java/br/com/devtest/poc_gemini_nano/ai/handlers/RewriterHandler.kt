package br.com.devtest.poc_gemini_nano.ai.handlers

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.rewriting.RewriterOptions
import com.google.mlkit.genai.rewriting.Rewriting
import com.google.mlkit.genai.rewriting.RewritingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class RewriterHandler(context: Context) {

    private val _inferenceResult = MutableSharedFlow<String>()
    val inferenceResult = _inferenceResult.asSharedFlow()

    val rewriterOptions = RewriterOptions.builder(context)
        .setOutputType(RewriterOptions.OutputType.ELABORATE)
        .setLanguage(RewriterOptions.Language.ENGLISH)
        .build()
    val rewriter = Rewriting.getClient(rewriterOptions)

    suspend fun prepareAndStartProcessing(textToRewrite: String) {
        val featureStatus = rewriter.checkFeatureStatus().await()

        when (featureStatus) {
            FeatureStatus.UNAVAILABLE -> {
                Log.e("RewriterHandler", "Feature is unavailable, cannot proceed with inference.")
                CoroutineScope(Dispatchers.Main).launch {
                    _inferenceResult.emit("Feature is unavailable.")
                }
            }

            FeatureStatus.DOWNLOADABLE -> {
                /**
                 * Download feature if necessary
                 * If downloadFeature is not called, the first inference request will
                 * also trigger the feature to be downloaded if it's not already
                 * downloaded.
                 **/
                rewriter.downloadFeature(object : DownloadCallback {
                    override fun onDownloadCompleted() {
                        Log.v("RewriterHandler", "Download completed successfully.")
                        startInferenceRequest(textToRewrite = textToRewrite, modelClient = rewriter)
                    }

                    override fun onDownloadFailed(p0: GenAiException) {
                        Log.e("RewriterHandler", "Download failed: ${p0.message}")
                    }

                    override fun onDownloadProgress(p0: Long) {
                        Log.v("RewriterHandler", "Download progress: $p0")
                    }

                    override fun onDownloadStarted(p0: Long) {
                        Log.v("RewriterHandler", "Download started: $p0")
                    }
                })
            }

            FeatureStatus.DOWNLOADING -> {
                Log.v(
                    "RewriterHandler",
                    "Feature is currently downloading, waiting for completion."
                )
                startInferenceRequest(textToRewrite = textToRewrite, modelClient = rewriter)
            }

            FeatureStatus.AVAILABLE -> {
                Log.v("RewriterHandler", "Feature is available, starting inference request.")
                startInferenceRequest(textToRewrite = textToRewrite, modelClient = rewriter)
            }
        }
    }

    private fun startInferenceRequest(
        textToRewrite: String,
        modelClient: Rewriter
    ) {
        val rewritingRequest = RewritingRequest.builder(textToRewrite).build()

        modelClient.runInference(rewritingRequest) { newText ->
            CoroutineScope(Dispatchers.Main).launch {
                Log.v("RewriterHandler", "Inference result: $newText")
                _inferenceResult.emit(newText)
            }
        }
    }

    fun closeRewriter() {
        Log.v("RewriterHandler", "Closing Rewriter client.")
        rewriter.close()
    }
}