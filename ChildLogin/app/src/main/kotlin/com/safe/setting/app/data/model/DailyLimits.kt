package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * हिंदी कमेंट: यह एक सिंपल क्लास है, जैसा कि आप चाहते थे, न कि डेटा क्लास।
 * यह फायरबेस पर दैनिक रिकॉर्डिंग सीमाओं की जानकारी संग्रहीत करने के लिए है।
 *
 * लॉजिक:
 * - lastResetDate: यह उस तारीख (yyyy-MM-dd) को संग्रहीत करता है जब सीमा आखिरी बार रीसेट की गई थी।
 * - videoCount: यह उस दिन की गई वीडियो रिकॉर्डिंग की संख्या को ट्रैक करता है।
 * - audioCount: यह उस दिन की गई ऑडियो रिकॉर्डिंग की संख्या को ट्रैक करता है *
 * - maxVideoLimit: यह फायरबेस से वीडियो रिकॉर्डिंग की अधिकतम दैनिक सीमा को संग्रहीत करेगा। (डिफ़ॉल्ट 4)
 * - maxAudioLimit: यह फायरबेस से ऑडियो रिकॉर्डिंग की अधिकतम दैनिक सीमा को संग्रहीत करेगा। (डिफ़ॉल्ट 5)
 *
 * अब आप maxVideoLimit और maxAudioLimit को सीधे फायरबेस में बदलकर किसी भी उपयोगकर्ता के लिए सीमा को नियंत्रित कर सकते हैं।
 */
@IgnoreExtraProperties
class DailyLimits {
    var lastResetDate: String? = null
    var videoCount: Int? = null
    var audioCount: Int? = null
    var maxVideoLimit: Int? = null
    var maxAudioLimit: Int? = null

    // फायरबेस के लिए खाली कंस्ट्रक्टर आवश्यक है।
    constructor()

    // पैरामीटर के साथ कंस्ट्रक्टर।
    constructor(
        lastResetDate: String?,
        videoCount: Int?,
        audioCount: Int?,
        maxVideoLimit: Int?,
        maxAudioLimit: Int?
    ) {
        this.lastResetDate = lastResetDate
        this.videoCount = videoCount
        this.audioCount = audioCount
        this.maxVideoLimit = maxVideoLimit
        this.maxAudioLimit = maxAudioLimit
    }
}

