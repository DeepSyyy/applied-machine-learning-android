package com.dicoding.asclepius.view

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.CancerClassficiationHelper
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    private lateinit var imageClassifierHelper: CancerClassficiationHelper

    private var res: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageClassifierHelper = CancerClassficiationHelper(
            context = this,
            classifierListener = object : CancerClassficiationHelper.ClassifierListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        showToast(error)
                    }
                }

                override fun onResults(results: FloatArray) {
                    runOnUiThread {
                        if (results.isNotEmpty()) {
                            var highest = 0
                            val displayResult = results.mapIndexed { index, value ->
                                if (value > results[highest]) {
                                    highest = index
                                }
                                when (index) {
                                    0 -> "Benign: ${NumberFormat.getPercentInstance().format(value)}"
                                    1 -> "Malignant: ${NumberFormat.getPercentInstance().format(1 - value)}"
                                    else -> "Unknown"
                                }
                            }
                            res = displayResult.joinToString("\n")
                            moveToResult()
                        } else {
                            showToast("No result")
                        }

                    }
                }
            }
        )


        binding.galleryButton.setOnClickListener {
            startGallery()
        }

        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }
    }

    private fun startGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = ACTION_GET_CONTENT
        val chooser = Intent.createChooser(intent, "Pilih gambar")
        launcGalerry.launch(chooser)
    }

    private val launcGalerry = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            currentImageUri = result.data?.data as Uri
            showImage()
        } else {
            Log.d("MainActivity", "Image Uri is null")
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("ImageUri", "Image Uri: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun analyzeImage() {
        Log.d("ImageUri", "Image Uri: $currentImageUri")
        currentImageUri?.let {
            imageClassifierHelper.classifyStaticImage(
                toBitmap(
                    this,
                    it
                )
            )
        }
    }

    private fun toBitmap(context: Context, image: Uri): Bitmap {
        val contentResolver = context.contentResolver

        val inputStream = contentResolver.openInputStream(image)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        inputStream?.close()
        return bitmap
    }

    private fun moveToResult() {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_RESULT, res)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}