package com.dicoding.asclepius.view

import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    private lateinit var imageClassifierHelper: ImageClassifierHelper

    private var res: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            listener = object : ImageClassifierHelper.ClassifierListener {
                override fun onResults(results: List<Classifications>?) {
                    Log.d("OnResult", results.toString())
                    results?.let {
                        Log.d("Hasil", it[0].categories.toString())
                        if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {

                            val sortedCategories =
                                it[0].categories.sortedByDescending { it?.score }
                            val displayResult =
                                sortedCategories.joinToString("\n") {
                                    "${it.label} " + NumberFormat.getPercentInstance()
                                        .format(it.score).trim()
                                }

                            res = displayResult
                            moveToResult()

                        } else {
                            showToast("Tidak dapat mengenali gambar")
                        }
                    }
                }

                override fun onError(error: String) {
                    showToast(error)
                }
            })

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
            imageClassifierHelper.classifyStaticImage(it)
        }
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