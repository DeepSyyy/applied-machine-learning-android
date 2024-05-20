package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException


class ImageClassifierHelper(
    val treshold: Float = 0.5f,
    var maxResult: Int = 3,
    var modelName: String = "cancer_classification.tflite",
    val context: Context,
    val listener: ClassifierListener?,
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        val optionsBuilder =
            ImageClassifier.ImageClassifierOptions.builder().setScoreThreshold(treshold)
                .setMaxResults(maxResult)
        val baseOptionBuildet = BaseOptions.builder().setNumThreads(4)

        optionsBuilder.setBaseOptions(baseOptionBuildet.build())

        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {

            Log.e(TAG, e.message.toString())
        }
    }

    fun classifyStaticImage(imageUri: Uri){
        try {
            Log.d(TAG, "classifyStaticImage: $imageUri")
            if (imageClassifier == null) {
                setupImageClassifier()
            }

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(CastOp(DataType.UINT8))
                .build()
            val tensorImage =
                imageProcessor.process(TensorImage.fromBitmap(uriToBitmap(context, imageUri)))

            val imageProcessingOptions = ImageProcessingOptions.builder()
                .setOrientation(ImageProcessingOptions.Orientation.TOP_LEFT)
                .build()

            listener?.onResults(imageClassifier?.classify(tensorImage, imageProcessingOptions))

        } catch (e: Exception) {
            listener?.onError(e.message.toString())
            Log.e(TAG, e.message.toString())
        }
    }

    private fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            Log.e(TAG, e.message.toString())
            null
        } finally {
            inputStream?.close()
        }

    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,

        )
    }
    companion object {
        private const val TAG = "ImageClassifierHelper"
    }

}