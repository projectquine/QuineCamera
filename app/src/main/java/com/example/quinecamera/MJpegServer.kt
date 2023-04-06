package com.example.quinecamera

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicReference

class MJpegServer(port: Int) : NanoHTTPD(port) {
    private val jpegDataAtomicReference = AtomicReference<ByteArray>()

    init {
        start()
    }

    fun stopServer() {
        stop()
    }

    private fun Map<String, List<String>>.getFirst(key: String): String? {
        return this[key]?.firstOrNull()
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val queryParams = session.parameters.mapKeys { it.key.toLowerCase() }
        val uri = session.uri
        Log.d("MJpegServer", queryParams.toString())
        Log.d("MJpegServer", uri.toString())
        return when {
            uri == "/webcam/" && queryParams.getFirst("action") == "stream" -> {
                Log.d("MJpegServer", "Stream called......")
                serveMjpegStream()
            }
            uri == "/webcam/" && queryParams.getFirst("action") == "snapshot" -> {
                serveSnapshot()
            }
            else -> {
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
            }
        }
    }

    private fun serveMjpegStream(): NanoHTTPD.Response {
        val inputStream = MJpegInputStream(jpegDataAtomicReference)
        return newChunkedResponse(NanoHTTPD.Response.Status.OK, "multipart/x-mixed-replace;boundary=$BOUNDARY", inputStream)
    }

    private fun serveSnapshot(): NanoHTTPD.Response {
        val jpegData = jpegDataAtomicReference.get()
        return if (jpegData != null) {
            val inputStream = ByteArrayInputStream(jpegData)
            newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "image/jpeg", inputStream, jpegData.size.toLong())
        } else {
            newFixedLengthResponse("No data available").apply {
                status = NanoHTTPD.Response.Status.NO_CONTENT
            }
        }
    }

    fun updateJpeg(jpegData: ByteArray) {
        jpegDataAtomicReference.set(jpegData)
    }

    private class MJpegInputStream(private val jpegDataAtomicReference: AtomicReference<ByteArray>) : ByteArrayInputStream(ByteArray(0)) {
        override fun read(buf: ByteArray, off: Int, len: Int): Int {
            val jpegData = jpegDataAtomicReference.get()
            return if (jpegData != null) {
                ByteArrayInputStream(jpegData).use { inputStream ->
                    inputStream.read(buf, off, len)
                }
            } else {
                -1
            }
        }
    }

    companion object {
        private const val BOUNDARY = "MJPEGBOUNDARY"
    }
}
