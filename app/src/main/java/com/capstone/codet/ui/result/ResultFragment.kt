package com.capstone.codet.ui.result

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.capstone.codet.R
import com.capstone.codet.data.ViewModelFactory
import com.capstone.codet.databinding.FragmentResultBinding
import java.io.File
import java.io.FileOutputStream
import com.capstone.codet.data.utils.Result

class ResultFragment : Fragment() {

    private val viewModel: ResultViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext())
    }

    private lateinit var binding: FragmentResultBinding
    private val navArgs by navArgs<ResultFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        observePredictionResult()
    }

    private fun setUpView() {
        val imgResult = navArgs.resultImage
        val imageFile = if (imgResult.isFromCamera) {
            imgResult.imageBitmap?.let { saveBitmapToFile(it) }
        } else {
            imgResult.imageUri?.let { getFileFromUri(it) }
        }

        if (imageFile != null) {
            // Load the image into the ImageView
            Glide.with(requireActivity())
                .load(imageFile)
                .into(binding.imgResult)

            // Upload the image for prediction
            viewModel.uploadPrediction(imageFile)
        } else {
            binding.textView4.text = "Prediction Error"
        }
    }

    private fun observePredictionResult() {
        viewModel.uploadPredictionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.textView4.text = getString(R.string.predicting)
                }
                is Result.Success -> {
                    val prediction = result.data.prediction
                    if (prediction != null) {
                        when (prediction) {
                            1L -> {
                                // Healthy
                                binding.textView4.text = "Healthy"
                                binding.cardView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
                            }
                            2L -> {
                                // Sick
                                binding.textView4.text = "Sick"
                                binding.cardView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
                            }
                            else -> {
                                // Unknown
                                binding.textView4.text = "Unknown"
                                binding.cardView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey)
                            }
                        }
                    } else {
                        binding.textView4.text = "Error"
                        binding.cardView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey)
                    }
                }
                is Result.Error -> {
                    binding.textView4.text = getString(R.string.prediction_error)
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val path = getPathFromUri(uri)
        return if (path != null) File(path) else null
    }

    private fun getPathFromUri(uri: Uri): String? {
        val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
        return if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(idx).also { cursor.close() }
        } else {
            uri.path
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val filesDir = requireActivity().filesDir
        val imageFile = File(filesDir, "detected_image_${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(imageFile).use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                os.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageFile
    }
}
