package com.kitware.medical.clariusstream

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.*
import android.util.Log
import android.util.Size
import android.widget.Toast
import me.clarius.mobileapi.MobileApi
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.stream.Stream

class StreamingService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val TAG = "ClariusStream"
    }

    private var mService: Messenger? = null
    private var mClient: Messenger? = null
    private var mBound: Boolean = false
    private var mRegistered: Boolean = false

    private val mConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // wraps the raw IBinder to the service with the Messenger interface
            mService = Messenger(service)
            mBound = true
            registerWithService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }

        override fun onNullBinding(name: ComponentName?) {
            Toast.makeText(
                applicationContext,
                "Clarius service refused to start; bad license?",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.START_SERVICE -> {
                    startStreamingService()
                }
                Constants.STOP_SERVICE -> {
                    stopStreamingService(startId)
                }
                else -> {
                    throw Exception("Invalid command")
                }
            }
        }
        return START_STICKY
    }

    /**
     * Callback parameters used to map outbound messages to return status messages from the service.
     *
     * How it works:
     *  1. When sending a message, set Message.arg1 to a unique value;
     *  2. Wait for a MSG_RETURN_STATUS from the service and check its Message.arg1 field:
     *      a. if it matches, it is the return status for our request;
     *      b. otherwise, it is the return status for another request;
     *  3. Use different codes to differentiate different requests.
     */
    class Requests {
        companion object {
            const val REGISTER = 1
            const val CONFIGURE_IMAGE = 2
        }
    }

    internal class IncomingHandler(
        service: StreamingService,
        private val mService: WeakReference<StreamingService> = WeakReference(service)
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MobileApi.MSG_RETURN_STATUS -> {
                    val param = msg.arg1
                    val status = msg.arg2
                    if (param == Requests.REGISTER) {
                        mService.get()?.onConnected(status == 0)
                    }
                }
                MobileApi.MSG_NEW_PROCESSED_IMAGE -> {
                    println("Got new image")
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startStreamingService() {
        Intent().also {
            it.component =
                ComponentName(BuildConfig.CLARIUS_PACKAGE_NAME, BuildConfig.CLARIUS_SERVICE_NAME)
            if (bindService(it, mConn, Context.BIND_AUTO_CREATE)) {
                startForegroundService()
                Log.d(TAG, "Started foreground service")
            } else {
                unbindService(mConn)
                Toast.makeText(
                    this,
                    "Could not connect. Is the probe connected?",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingMainIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val stopIntent = Intent(this, StreamingService::class.java).also {
            it.action = Constants.STOP_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, 0)
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                R.drawable.ic_launcher_background
            ), "Stop", pendingStopIntent
        ).build()

        val channelId =
            createNotificationChannel("clarius_stream_service", "Clarius Stream Service")
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Clarius IGTL Stream enabled")
            .setContentText("IP/Port:")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingMainIntent)
            .addAction(stopAction)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopStreamingService(startId: Int) {
        mClient?.let {
            unregisterFromService()
        }
        if (mBound) {
            unbindService(mConn)
            mBound = false
        }
        // will also invoke stopForeground(true)
        stopSelf(startId)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.GREEN
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun registerWithService() {
        if (mBound) {
            mClient = Messenger(IncomingHandler(this))
            val msg = Message.obtain(null, MobileApi.MSG_REGISTER_CLIENT)
            msg.replyTo = mClient
            // sets the callback param
            msg.arg1 = Requests.REGISTER
            try {
                mService?.send(msg)
                mRegistered = true
            } catch (e: RemoteException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to register with service", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unregisterFromService() {
        if (mBound && mRegistered) {
            val msg = Message.obtain(null, MobileApi.MSG_UNREGISTER_CLIENT)
            try {
                mService?.send(msg)
                mRegistered = false
            } catch (e: RemoteException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to unregister from service", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onConnected(connected: Boolean) {
        if (connected) {
            Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show()
            sendImageConfig()
        }
    }

    private fun makeImageConfig(): Bundle {
        return Bundle().also { bundle ->
            bundle.putSize(MobileApi.KEY_IMAGE_SIZE, Size(400, 400))
            bundle.putString(MobileApi.KEY_COMPRESSION_TYPE, MobileApi.COMPRESSION_TYPE_JPEG)
            bundle.putInt(MobileApi.KEY_COMPRESSION_QUALITY, 90)
        }
    }

    private fun sendImageConfig() {
        if (mBound) {
            val msg = Message.obtain(null, MobileApi.MSG_CONFIGURE_IMAGE)
            msg.replyTo = mClient
            msg.arg1 = Requests.CONFIGURE_IMAGE
            msg.data = makeImageConfig()
            try {
                mService?.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to send image config", Toast.LENGTH_SHORT).show()
            }
        }
    }
}