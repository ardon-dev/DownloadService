package com.github.vege19.backgroundservice

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.github.vege19.backgroundservice.service.DownloadService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val WRITE_STORAGE_REQUEST_PERMISSION_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activityFlow()

    }

    private fun activityFlow() {

        button.setOnClickListener {
            if (checkPermission()) {
                startImageDownload()
            } else {
                requestPermissions()
            }
        }

        //Start download observer
        downloadObserver()

    }

    private fun downloadObserver() {
        DownloadService.getObservableProgress().observe(this, Observer {
            if (it) {
                Toast.makeText(applicationContext, "File downloaded", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DownloadService::class.java)
                stopService(intent)

            }

        })

    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return  result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            WRITE_STORAGE_REQUEST_PERMISSION_CODE
        )
    }

    private fun startImageDownload() {
        val intent = Intent(this, DownloadService::class.java)
        startService(intent)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //Handle codes
        when (requestCode) {
            WRITE_STORAGE_REQUEST_PERMISSION_CODE -> {
                /*
                Write external storage
                 */
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /*
                    Permission granted
                     */
                    startImageDownload()
                } else {
                    /*
                    Permission denied
                     */
                    Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_SHORT).show()
                }

            }
            else -> {
                /*
                Ignore other cases
                 */
            }

        }

    }

}
