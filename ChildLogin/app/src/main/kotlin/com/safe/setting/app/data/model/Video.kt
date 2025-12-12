package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

// COMMENT: Video recording ki jankari store karne ke liye simple class.
@IgnoreExtraProperties
class Video {

    var nameRandom: String? = null
    var dateTime: String? = null
    var videoUrl: String? = null
    var durationVideo: String? = null

    // COMMENT: Default constructor, jaisa aapke purane code mein hai.
    constructor()

    // COMMENT: Parameterized constructor.
    constructor(nameRandom: String?, dateTime: String?, videoUrl: String?, durationVideo: String?) {
        this.nameRandom = nameRandom
        this.dateTime = dateTime
        this.videoUrl = videoUrl
        this.durationVideo = durationVideo
    }
}