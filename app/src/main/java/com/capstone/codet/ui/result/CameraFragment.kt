package com.capstone.codet.ui.result

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capstone.codet.R
import com.capstone.codet.data.model.ImageResult
import com.capstone.codet.data.utils.createFile
import com.capstone.codet.data.utils.rotateBitmap
import com.capstone.codet.data.utils.toBitmap
import com.capstone.codet.data.utils.toFile
import com.capstone.codet.databinding.FragmentCameraBinding

class CameraFragment:Fragment() {

    private var _bindings: FragmentCameraBinding? = null
    private val bindings get() = _bindings
    private var camerasSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imgCapture: ImageCapture? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _bindings = FragmentCameraBinding.inflate(inflater, container, false)
        return bindings?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launchesPermission.launch(PERMISSIONS.first())

        setsUpView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }
    }

    private fun setsUpView() {
        bindings?.apply {
            btnCapture.setOnClickListener {

                takesPhoto()
            }
            btnAddGallery.setOnClickListener {
                val mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                launchesGallery.launch(PickVisualMediaRequest(mediaType))
            }
            btnFlipCamera.setOnClickListener {
                camerasSelector = if (camerasSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
                startsCamera()
            }

        }
    }
    override fun onResume() {
        super.onResume()
        startsCamera()
    }

    private fun startsCamera() {
        val camerasProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        camerasProviderFuture.addListener({
            val camerasProvider = camerasProviderFuture.get()
            val previews = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(bindings?.viewFinder?.surfaceProvider)
                }
            imgCapture = ImageCapture.Builder().build()
            try {
                camerasProvider.unbindAll()
                camerasProvider.bindToLifecycle(
                    this,
                    camerasSelector,
                    previews,
                    imgCapture
                )
            } catch (e: Exception) {
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.open_camera_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun takesPhoto() {
        val imgCapture = imgCapture ?: return
        val photoFiles = createFile(requireActivity().application)
        val outputsOptions = ImageCapture.OutputFileOptions.Builder(photoFiles).build()
        imgCapture.takePicture(
            outputsOptions,
            ContextCompat.getMainExecutor(requireActivity()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val imageBitmaps = photoFiles.toBitmap()
                    val rotatedBitmaps = imageBitmaps.rotateBitmap(
                        camerasSelector == CameraSelector.DEFAULT_BACK_CAMERA
                    )
                    movesToUpload(ImageResult(photoFiles, imageBitmap = rotatedBitmaps))
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.taking_photo_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun movesToUpload(imageResult: ImageResult) {
        val action =
            CameraFragmentDirections.actionCameraFragment2ToResultFragment(
                imageResult
            )
        findNavController().navigate(action)
    }

    private val launchesGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val imgFile = it.toFile(requireActivity())
            movesToUpload(ImageResult(imgFile, it, isFromCamera = false))
        }
    }

    private val launchesPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted and !isAllPermissionsGranted()) {
            Toast.makeText(requireActivity(), getString(R.string.access_denied), Toast.LENGTH_SHORT).show()
        }
    }
    private fun isAllPermissionsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireActivity(), it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onDestroyView() {
        super.onDestroyView()

        _bindings = null
    }
    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}