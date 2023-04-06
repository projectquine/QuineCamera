package com.example.quinecamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import com.example.quinecamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                val portNumber = binding.portNumberEditText.text.toString().toIntOrNull() ?: 8080
                val cameraSelector = if (binding.cameraToggle.isChecked) "FRONT" else "BACK"
                startCameraService(portNumber, cameraSelector)
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val portNumberEditText = binding.portNumberEditText
        val cameraToggle = binding.cameraToggle

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val portNumber = portNumberEditText.text.toString().toIntOrNull() ?: 8080
            val cameraSelector = if (cameraToggle.isChecked) "FRONT" else "BACK"
            startCameraService(portNumber, cameraSelector)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraToggle.setOnCheckedChangeListener { _, isChecked ->
            val portNumber = portNumberEditText.text.toString().toIntOrNull() ?: 8080
            val cameraSelector = if (isChecked) "FRONT" else "BACK"
            startCameraService(portNumber, cameraSelector)
        }

        portNumberEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val portNumber = portNumberEditText.text.toString().toIntOrNull() ?: 8080
                val cameraSelector = if (cameraToggle.isChecked) "FRONT" else "BACK"
                startCameraService(portNumber, cameraSelector)
                true
            } else {
                false
            }
        }
    }


    private fun startCameraService(portNumber: Int, cameraSelector: String) {
        val serviceIntent = Intent(this, CameraService::class.java)
        serviceIntent.putExtra("portNumber", portNumber)
        serviceIntent.putExtra("cameraSelector", cameraSelector)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
