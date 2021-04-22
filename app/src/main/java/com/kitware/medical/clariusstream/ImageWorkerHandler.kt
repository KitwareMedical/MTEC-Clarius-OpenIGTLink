package com.kitware.medical.clariusstream

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import me.clarius.mobileapi.MobileApi
import me.clarius.mobileapi.ProcessedImageInfo
import java.lang.AssertionError

class ImageWorkerHandler(looper: Looper) : Handler(looper) {
    companion object {
        const val TAG = "ImageWorkerHandler"
    }

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            Constants.ImageWorker.IMAGE_DATA -> {
                val data = msg.data
                data.classLoader = ProcessedImageInfo::class.java.classLoader
                val info = data.getParcelable(MobileApi.KEY_IMAGE_INFO) as ProcessedImageInfo?
                    ?: throw AssertionError("Image info missing")

                val imageData = data.getByteArray(MobileApi.KEY_IMAGE_DATA)
                    ?: throw AssertionError("image data missing")
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    ?: throw AssertionError("Bad image data")

                Log.d(TAG, "Received image ${info.width}x${info.height} @${info.tm}")
            }
        }
    }
}