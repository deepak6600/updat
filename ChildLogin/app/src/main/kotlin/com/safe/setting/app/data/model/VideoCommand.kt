package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * ### पुराना कोड ###
 * class VideoCommand {
 * var recordVideo: Boolean? = null
 * ...
 * }
 *
 * ### नया/संशोधित कोड ###
 * हिंदी कमेंट: यह आपकी मौजूदा सिंपल क्लास है जिसे संशोधित किया गया है।
 * लॉजिक:
 * - recordVideo (बूलियन): यह बताता है कि रिकॉर्डिंग शुरू करनी है या नहीं।
 * - facing (इंटीजर): यह नया फ़ील्ड बताता है कि कौन सा कैमरा इस्तेमाल करना है (0 = बैक, 1 = फ्रंट)।
 * इसे जोड़ने से हम फ्रंट कैमरे के बग को ठीक कर सकते हैं।
 */
@IgnoreExtraProperties
class VideoCommand {

    var recordVideo: Boolean? = null
    var facing: Int? = null

    // फायरबेस के लिए खाली कंस्ट्रक्टर।
    constructor()

    // पैरामीटर के साथ कंस्ट्रक्टर।
    constructor(recordVideo: Boolean?, facing: Int?) {
        this.recordVideo = recordVideo
        this.facing = facing
    }
}
