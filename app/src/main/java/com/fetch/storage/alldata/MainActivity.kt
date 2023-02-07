package com.fetch.storage.alldata

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.fetch.storage.alldata.model.FolderItem
import com.fetch.storage.alldata.model.MediaItem

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //
        MediaLoader().loadDeviceMedia(this@MainActivity, "video", "DateNewToOld", object : MediaLoader.MediaLoadListener {
            override fun onMediaLoadStart() {
                Log.e("MainActivity", "onMediaLoadStart: ")
            }

            override fun onMediaLoadProgress() {
                Log.e("MainActivity", "onMediaLoadProgress: ")

            }

            override fun onMediaLoaded(MediaList: ArrayList<MediaItem>, folderList: ArrayList<FolderItem>) {
                Log.e("MainActivity", "onMediaLoaded: ${MediaList.size}")

            }

            override fun onFailed(exc: String) {
                Log.e("MainActivity", "onFailed: $exc ")

            }

            override fun onMediaSorted() {
                Log.e("MainActivity", "onMediaSorted: ")

            }
        })
    }
}