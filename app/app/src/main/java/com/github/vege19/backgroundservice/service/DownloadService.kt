package com.github.vege19.backgroundservice.service

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.github.vege19.backgroundservice.R
import com.github.vege19.backgroundservice.network.RetrofitInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadService : IntentService("DownloadService") {

   companion object {
       /*
   Through this variable, we can send progress change to activity
    */
       private val mObservableProgress = MutableLiveData<Boolean>()
       fun getObservableProgress() = mObservableProgress

   }

    /*
    Notification handling
     */
    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private lateinit var mNotificationManager: NotificationManager

    //Notification channel builds
    private val NOTIFICATION_CHANNEL_ID = "DOWNLOAD_CHANNEL"
    private val NOTIFICATION_CHANNEL_NAME = "DOWNLOADS"


    override fun onHandleIntent(intent: Intent?) {
        //Step one: setup notification
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        declareNotificationChannel()
        buildNotification()

        //Step two: Make retrofit request for download the file
        initDownload()

    }

    /*
    Create notification channel for android O versions or above
     */
    private fun declareNotificationChannel() {
        //Check if version is O or above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "No sound"
            notificationChannel.setSound(null, null)
            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.enableVibration(false)
            mNotificationManager.createNotificationChannel(notificationChannel)

        }

    }

    /*
    Build custom notification
     */
    private fun buildNotification() {
        mNotificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_file_download_black_24dp)
            .setContentTitle("Download")
            .setContentText("Downloading image")
            .setDefaults(0)
            .setAutoCancel(true)

        mNotificationManager.notify(0, mNotificationBuilder.build())

    }

    /*
    Retrofit download request
     */
    private fun initDownload() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .build()

        val retrofitInterface = retrofit.create(RetrofitInterface::class.java)
        val request = retrofitInterface.downloadImage("Vege19/DownloadService/master/android.png")

        try {
            downloadImage(request.execute().body()!!)

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()

        }

    }

    /*
    Process to download and save image
     */
    private fun downloadImage(responseBody: ResponseBody) {
        var count: Int = 0
        val data = ByteArray(1204 * 4)
        val fileSize = responseBody.contentLength()
        val inputStream = BufferedInputStream(responseBody.byteStream(), 1024 * 8)
        val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "android.png")
        val outputStream = FileOutputStream(outputFile)
        var total: Long = 0
        var downloadComplete = false

        while (inputStream.read(data).also { count = it } != -1) {
            total += count.toLong()
            val progress = ((total * 100).toDouble() / fileSize.toDouble()).toInt()
            updateNotification(progress)
            outputStream.write(data, 0, count)
            downloadComplete = true
        }
        onDownloadComplete(downloadComplete)
        outputStream.flush()
        outputStream.close()
        inputStream.close()

    }

    private fun updateNotification(currentProgress: Int) {
        mNotificationBuilder.setProgress(100, currentProgress, false)
        mNotificationBuilder.setContentText("Downloading: $currentProgress%")
        mNotificationManager.notify(0, mNotificationBuilder.build())

    }

    private fun onDownloadComplete(downloadComplete: Boolean) {
        sendProgressUpdate(downloadComplete)
        mNotificationManager.cancel(0)
        mNotificationBuilder.setProgress(0, 0, false)
        mNotificationBuilder.setContentText("Download completed")
        mNotificationManager.notify(0, mNotificationBuilder.build())

    }

    private fun sendProgressUpdate(downloadComplete: Boolean) {
        mObservableProgress.postValue(downloadComplete)

    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mNotificationManager.cancel(0)

    }

}