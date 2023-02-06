package com.fetch.storage.alldata.model
class MediaItem(
    var mediaId: String,
    var mediaTitle: String,
    var path: String,
    val duration: Long,
    var folderName: String,
    var resolution: String,
    var fileSizeAs: Long,
    var date_added: String,
    var date: Long
)