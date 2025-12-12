package com.safe.setting.app.data.model

import com.google.firebase.database.IgnoreExtraProperties

// [नया कोड]: यह पूरी क्लास नई है.
// लॉजिक: यह मॉडल ऐप की समग्र हेल्थ स्थिति, जैसे सेवाओं की स्थिति, अनुमतियों की स्थिति,
// और अन्य जानकारी को फायरबेस पर संग्रहीत करने के लिए एक संरचित तरीका प्रदान करता है.
@IgnoreExtraProperties
data class AppHealthStatus(
    // Service Status
    var isAccessibilityServiceEnabled: Boolean? = null,
    var isNotificationServiceEnabled: Boolean? = null,
    var lastHeartbeatTime: Long? = null, // आखिरी बार सर्विस कब सक्रिय थी

    // Permission Status
    var hasLocationPermission: Boolean? = null,
    var hasCameraPermission: Boolean? = null,
    var hasMicrophonePermission: Boolean? = null,
    var hasSmsPermission: Boolean? = null,
    var hasCallLogPermission: Boolean? = null,
    var hasDrawOverAppsPermission: Boolean? = null,
    var isIgnoringBatteryOptimizations: Boolean? = null,

    // Hardware/OS Interference
    var hardwareBlockEvents: Map<String, String>? = null, // उन घटनाओं को लॉग करें जहां हार्डवेयर ब्लॉक हो सकता है

    // General Info
    var lastCheckedTimestamp: Long? = null,
    var appVersion: String? = null
)