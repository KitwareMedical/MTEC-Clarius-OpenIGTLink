package com.kitware.medical.clariusstream

interface ClariusMessageObserver {
    fun onConnected(connected: Boolean)
    fun onConfiguredImage(configured: Boolean)
    fun onNewProcessedImage()
}