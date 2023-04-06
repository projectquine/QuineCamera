package com.example.quinecamera

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.IBinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class CameraService : Service() {

    private val channelId = "CameraServiceChannel"
    private val notificationId = 1

    private lateinit var mjpegServer: MJpegServer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val serviceLifecycleOwner = ServiceLifecycleOwner()

    override fun onCreate() {
        super.onCreate()
        serviceLifecycleOwner.start()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(notificationId, notification)

        mjpegServer = MJpegServer(8080)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor(), ImageAnalyzer(mjpegServer))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(serviceLifecycleOwner, cameraSelector, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceLifecycleOwner.destroy()
        mjpegServer.stopServer()
    }

    private class ImageAnalyzer(private val server: MJpegServer) : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            if (image.format != ImageFormat.YUV_420_888) {
                return
            }

            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val outputStream = ByteArrayOutputStream()

            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, outputStream)
            val jpegData = outputStream.toByteArray()

            server.updateJpeg(jpegData)

            outputStream.close()
            image.close()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Camera Service"
            val descriptionText = "Streams the phone camera to an HTTP endpoint."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
            .setContentTitle("Camera Service")
            .setContentText("Streaming phone camera to an HTTP endpoint.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
        return builder.build()
    }


}
