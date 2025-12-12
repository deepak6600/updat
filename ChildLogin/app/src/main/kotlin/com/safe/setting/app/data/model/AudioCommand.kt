package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

// COMMENT: Audio recording command bhejane ke liye class.
@IgnoreExtraProperties
class AudioCommand {

    var recordAudio: Boolean? = null

    constructor()

    constructor(recordAudio: Boolean?) {
        this.recordAudio = recordAudio
    }
}