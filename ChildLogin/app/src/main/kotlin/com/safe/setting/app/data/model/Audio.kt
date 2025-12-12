package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

// COMMENT: Audio recording ki jankari store karne ke liye simple class.
@IgnoreExtraProperties
class Audio {

    var nameRandom: String? = null
    var dateTime: String? = null
    var audioUrl: String? = null
    var durationAudio: String? = null

    // COMMENT: Default constructor, jaisa aapke purane code mein hai.
    constructor()

    // COMMENT: Parameterized constructor.
    constructor(nameRandom: String?, dateTime: String?, audioUrl: String?, durationAudio: String?) {
        this.nameRandom = nameRandom
        this.dateTime = dateTime
        this.audioUrl = audioUrl
        this.durationAudio = durationAudio
    }
}