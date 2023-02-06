package com.fetch.storage.alldata
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.fetch.storage.alldata.model.FolderItem
import com.fetch.storage.alldata.model.MediaItem
import java.util.concurrent.Executors

class MediaLoader : LifecycleOwner {
    /* load your media */
    fun loadDeviceMedia(context: Context, fetchData: String, fetchByOrder: String, mediaLoadListener: MediaLoadListener) {
        Executors.newSingleThreadExecutor().execute(MediaLoadRunnable(context, fetchData, fetchByOrder, mediaLoadListener))
    }

    inner class MediaLoadRunnable(private val mContext: Context, private val mFetchData: String, private val mFetchByOrder: String, private val mListener: MediaLoadListener) :
        Runnable {
        private val handler = Handler(Looper.getMainLooper())
        private val projection = arrayOf("_id", "_data", "title", "_size", "date_modified", "datetaken", "duration", "resolution", "bucket_display_name")
        private var mMediaList: ArrayList<MediaItem> = ArrayList()
        private var mFolderList: ArrayList<FolderItem> = ArrayList()
        override fun run() {
            mListener.onMediaLoadStart()

            if (!permissionAlreadyGranted(mContext)) {
                mListener.onFailed("Permission not granted!")
                return
            }

            try {
                scanFile(mContext, Environment.getExternalStorageDirectory().absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val uri: Uri = when (mFetchData) {
                "audio" -> {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                "image" -> {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                else -> {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            }

            var query: Cursor? = null
            when (mFetchByOrder) {
                "DateNewToOld" -> {
                    query = mContext.contentResolver.query(uri, projection, null, null, "date_modified ASC")!!
                }
                "DateOldToNew" -> {
                    query = mContext.contentResolver.query(uri, projection, null, null, "date_modified DESC")!!
                }
                "NameAToZ" -> {
                    query = mContext.contentResolver.query(uri, projection, null, null, "title DESC")!!
                }
                "NameZToA" -> {
                    query = mContext.contentResolver.query(uri, projection, null, null, "title ASC")!!
                }
                "SizeHighToLow" -> {
                    query = mContext.contentResolver.query(uri, projection, null, null, "_size ASC")!!
                }
                "SizeLowToHigh" -> {
                    query = mContext.contentResolver.query(uri, projection, null, null, "_size DESC")!!
                }
            }
            if (query == null) {
                handler.post {
                    mListener.onFailed("null")
                }
                return
            }
            mMediaList = ArrayList<MediaItem>()
            mFolderList = ArrayList<FolderItem>()
            val mTmpMediaList = ArrayList<MediaItem>()
            if (query.moveToLast()) {
                do {
                    val size = query.getLong(3)/*_size*/
                    if (size >= 1048576) {
                        val data = query.getString(1)/*_data*/
                        if (data != null) {
                            val id = query.getString(0)/*_id*/
                            val title = query.getString(2)/*title*/
                            val dataModified = if (query.getString(4).isNullOrEmpty()) "" else query.getString(4)/*date_modified*/
                            val resolution = if (query.getString(7).isNullOrEmpty()) "" else query.getString(7)/*resolution*/
                            val bucketName = if (query.getString(8).isNullOrEmpty()) "Unknown Folder" else query.getString(8)/*bucket_display_name*/
                            val duration = query.getLong(6)/*duration*/
                            val dateTaken = query.getLong(5)/*datetaken*/
                            val mMediaItem = MediaItem(id, title, data, duration, bucketName, resolution, size, dataModified, dateTaken)
                            mTmpMediaList.add(mMediaItem)
                        }
                    }
                } while (query.moveToPrevious())
            }
            query.close()
            if (mTmpMediaList.isNotEmpty()) {
                mMediaList.addAll(mTmpMediaList)
                mFolderList = getFolderList(mTmpMediaList)
                when (mFetchByOrder) {
                    "NameAToZ" -> {
                        mFolderList.sortWith { v1, v2 ->
                            (v1.folderName.lowercase()).compareTo(v2.folderName.lowercase())
                        }
                    }
                    "NameZToA" -> {
                        mFolderList.sortWith { v1, v2 ->
                            (v2.folderName.lowercase()).compareTo(v1.folderName.lowercase())
                        }
                    }
                }
            } else {
                mListener.onFailed("Empty")
            }
            handler.post {
                if (mMediaList.isNotEmpty() && mFolderList.isNotEmpty()) {
                    mListener.onMediaLoaded(mMediaList, mFolderList)
                } else {
                    mListener.onFailed("Empty")
                }
            }
        }
    }
    /* load your media */

    /* sort your media */
    fun sortLoadedMedia(
        context: Context, mediaList: ArrayList<MediaItem>, folderList: ArrayList<FolderItem>, fetchByOrder: String, mediaLoadListener: MediaLoadListener
    ) {
        Executors.newSingleThreadExecutor().execute(SortLoadedMeidaRunnable(context, mediaList, folderList, fetchByOrder, mediaLoadListener))
    }

    inner class SortLoadedMeidaRunnable(
        private val mContext: Context,
        private var mMediaList: ArrayList<MediaItem>,
        private var mFolderList: ArrayList<FolderItem>,
        private val mFetchByOrder: String,
        private val mListener: MediaLoadListener
    ) : Runnable {
        private val handler = Handler(Looper.getMainLooper())

        override fun run() {
            mListener.onMediaLoadStart()
            val mTempMediaList: ArrayList<MediaItem> = ArrayList()
            var mTempFolderList: ArrayList<FolderItem> = ArrayList()
            if (mMediaList.isNotEmpty() && mFolderList.isNotEmpty()) {
                mTempMediaList.addAll(mMediaList)
                mTempFolderList.addAll(mFolderList)
                when (mFetchByOrder) {
                    "DateNewToOld" -> {
                        mTempFolderList = ArrayList<FolderItem>()
                        mTempMediaList.sortWith { v1, v2 -> (v2.date_added).compareTo(v1.date_added) }
                        if (mTempMediaList.isNotEmpty()) {
                            mTempFolderList.addAll(getFolderList(mTempMediaList))
                        }
                    }
                    "DateOldToNew" -> {
                        mTempFolderList = ArrayList<FolderItem>()
                        mTempMediaList.sortWith { v1, v2 -> (v1.date_added).compareTo(v2.date_added) }
                        if (mTempMediaList.isNotEmpty()) {
                            mTempFolderList.addAll(getFolderList(mTempMediaList))
                        }
                    }
                    "NameAToZ" -> {
                        mTempMediaList.sortWith { v1, v2 -> (v1.mediaTitle.lowercase()).compareTo(v2.mediaTitle.lowercase()) }
                        mTempFolderList.sortWith { v1, v2 -> (v1.folderName.lowercase()).compareTo(v2.folderName.lowercase()) }
                    }
                    "NameZToA" -> {
                        mTempMediaList.sortWith { v1, v2 -> (v2.mediaTitle.lowercase()).compareTo(v1.mediaTitle.lowercase()) }
                        mTempFolderList.sortWith { v1, v2 -> (v2.folderName.lowercase()).compareTo(v1.folderName.lowercase()) }
                    }
                    "SizeHighToLow" -> {
                        mTempFolderList = ArrayList<FolderItem>()
                        mTempMediaList.sortWith { v1, v2 -> (v2.fileSizeAs).compareTo(v1.fileSizeAs) }
                        if (mTempMediaList.isNotEmpty()) {
                            mTempFolderList.addAll(getFolderList(mTempMediaList))
                        }
                    }
                    "SizeLowToHigh" -> {
                        mTempFolderList = ArrayList<FolderItem>()
                        mTempMediaList.sortWith { v1, v2 -> (v1.fileSizeAs).compareTo(v2.fileSizeAs) }
                        if (mTempMediaList.isNotEmpty()) {
                            mTempFolderList.addAll(getFolderList(mTempMediaList))
                        }
                    }
                }
            } else {
                mListener.onFailed("Empty")
            }
            handler.post {
                if (mTempMediaList.isNotEmpty() && mTempFolderList.isNotEmpty()) {
                    mListener.onMediaLoaded(mTempMediaList, mTempFolderList)
                } else {
                    mListener.onFailed("Empty")
                }
            }
        }
    }
    /* sort your media */


    /* local member */
    interface MediaLoadListener {
        fun onMediaLoadStart()
        fun onMediaLoadProgress()
        fun onMediaLoaded(MediaList: ArrayList<MediaItem>, folderList: ArrayList<FolderItem>)
        fun onFailed(exc: String)
        fun onMediaSorted()
    }

    private fun scanFile(context: Context, strFilePath: String) {
        try {
            MediaScannerConnection.scanFile(context, arrayOf(strFilePath), null) { _, _ -> }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(" jk ", "scanFile:Exception:  ${e.message} ")
        }
    }

    private fun getFolderList(arrayList: ArrayList<MediaItem>): ArrayList<FolderItem> {
        val arrayList2 = ArrayList<FolderItem>()
        val folderName = ArrayList<String>()
        if (arrayList.size > 0) {
            for (i in arrayList.indices) {
                val name = arrayList[i].folderName
                if (folderName.size > 0) {
                    if (!folderName.contains(name)) {
                        folderName.add(name)
                        arrayList2.add(FolderItem(name))
                    }
                } else {
                    folderName.add(name)
                    arrayList2.add(FolderItem(name))
                }
                val it: Iterator<FolderItem> = arrayList2.iterator()
                while (it.hasNext()) {
                    val next = it.next()
                    val checkIf: String = next.folderName
                    if (checkIf == name) {
                        next.mediaItems.add(arrayList[i])
                    }
                }
            }
        }
        return arrayList2
    }

    private fun permissionAlreadyGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun getLifecycle(): Lifecycle {
        Log.e("VideoLoader", "getLifecycle: Not yet implemented")
        TODO("Not yet implemented")
    }

    /* local member */

}