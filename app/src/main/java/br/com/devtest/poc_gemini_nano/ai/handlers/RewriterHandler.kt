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

/**
 * RewriterHandler is responsible for managing the Rewriter client and processing text to rewrite.
 * It handles feature status checks, downloads, and inference requests.
 *
 * @param context The context in which the Rewriter client operates.
 */
class RewriterHandler(context: Context) {

    private val _inferenceResult = MutableSharedFlow<String>()
    val inferenceResult = _inferenceResult.asSharedFlow()

    /**
     * Initialize the Rewriter client with the provided context and options.
     * The RewriterOptions can be customized as needed.
     */
    val rewriterOptions = RewriterOptions.builder(context)
        .setOutputType(RewriterOptions.OutputType.ELABORATE)
        .setLanguage(RewriterOptions.Language.ENGLISH)
        .build()

    /**
     * Create a Rewriter client using the specified options.
     */
    val rewriter = Rewriting.getClient(rewriterOptions)


    /**
     * Prepare the Rewriter client and start processing the text to rewrite.
     * This method checks the feature status and handles downloading if necessary.
     *
     * @param textToRewrite The text that needs to be rewritten.
     */
    suspend fun prepareAndStartProcessing(textToRewrite: String) {
        val featureStatus = rewriter.checkFeatureStatus().await()

        when (featureStatus) {
            /**
             * FeatureStatus.UNSUPPORTED indicates that the feature is not supported by the device.
             */
            FeatureStatus.UNAVAILABLE -> {
                Log.e("RewriterHandler", "Feature is unavailable, cannot proceed with inference.")
                CoroutineScope(Dispatchers.Main).launch {
                    _inferenceResult.emit("Feature is unavailable.")
                }
            }

            /**
             * FeatureStatus.DOWNLOADABLE indicates that the feature is available for download.
             */
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

            /**
             * FeatureStatus.DOWNLOADING indicates that the feature is currently being downloaded.
             * In this case, we wait for the download to complete before proceeding with inference.
             */
            FeatureStatus.DOWNLOADING -> {
                Log.v(
                    "RewriterHandler",
                    "Feature is currently downloading, waiting for completion."
                )
                startInferenceRequest(textToRewrite = textToRewrite, modelClient = rewriter)
            }

            /**
             * FeatureStatus.AVAILABLE indicates that the feature is available and ready for use.
             */
            FeatureStatus.AVAILABLE -> {
                Log.v("RewriterHandler", "Feature is available, starting inference request.")
                startInferenceRequest(textToRewrite = textToRewrite, modelClient = rewriter)
            }
        }
    }

    /**
     * Start the inference request with the provided text to rewrite.
     * This method builds a RewritingRequest and runs the inference using the Rewriter client.
     *
     * @param textToRewrite The text that needs to be rewritten.
     * @param modelClient The Rewriter client used for inference.
     */
    private fun startInferenceRequest(
        textToRewrite: String,
        modelClient: Rewriter
    ) {
        val rewritingRequest = RewritingRequest.builder(textToRewrite).build()

        /**
         * Run the inference request using the Rewriter client, and on result emit its trough a
         * shared flow.
         */
        modelClient.runInference(rewritingRequest) { newText ->
            CoroutineScope(Dispatchers.Main).launch {
                Log.v("RewriterHandler", "Inference result: $newText")
                _inferenceResult.emit(newText)
            }
        }
    }

    /**
     * Function to close the Rewriter client - this should be called when the client is no longer needed.
     */
    fun closeRewriter() {
        Log.v("RewriterHandler", "Closing Rewriter client.")
        rewriter.close()
    }
}