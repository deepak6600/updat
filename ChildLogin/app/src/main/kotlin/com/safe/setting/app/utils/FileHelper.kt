package com.safe.setting.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.safe.setting.app.utils.Consts.ADDRESS_IMAGE
import com.safe.setting.app.utils.Consts.TAG
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import kotlin.jvm.Throws

object FileHelper {

    // ##################################################################
    // ### आपका पुराना कोड यहाँ से शुरू होता है (कोई बदलाव नहीं)      ###
    // ##################################################################

    fun Context.getFilePath(): String =
        if (externalCacheDir != null) externalCacheDir!!.absolutePath
        else cacheDir.absolutePath

    fun Context.deleteFileName(fileName: String?) {
        if (fileName == null)
            return
        try {
            val file = File(fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to delete file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteFile(fileName: String?) {
        if (fileName == null)
            return
        try {
            val file = File(fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.message.toString())
        }
    }

    fun Context.deleteAllFile(address:String) {
        try {
            val file = File(getFilePath(), address)
            if (file.isDirectory) {
                file.listFiles()?.forEach { it.delete() }
            } else {
                file.delete()
            }
        }catch (e:Exception){
            Toast.makeText(this, "Failed to delete files: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(Exception::class)
    fun Context.getContactName(phoneNum: String?): String {
        if (phoneNum==null) throw Exception("Phone number can't be empty")

        var res = phoneNum.replace("[*+-]".toRegex(), "")
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)

        val names = contentResolver.query(uri, projection, null, null, null)
        if (names!=null){
            val indexName = names.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val indexNumber = names.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            if (names.count > 0) {
                names.moveToFirst()
                do {
                    val name = names.getString(indexName)
                    val number = names.getString(indexNumber).replace("[*+-]".toRegex(), "")

                    if (number.compareTo(res) == 0) {
                        res = name
                        break
                    }
                } while (names.moveToNext())
            }
            names.close()
        }
        return res
    }

    fun Uri.getUriPath(context: Context): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(this, projection, null, null, null) ?: return null
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val s = cursor.getString(columnIndex)
        cursor.close()
        return s
    }

    fun getDurationFile(fileName:String) : String{
        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(fileName)
        val duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        val seconds = (duration % 60000 / 1000).toString()
        val minutes = (duration / 60000).toString()
        metaRetriever.release()
        return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
    }

    fun Bitmap.getFileNameBitmap(context: Context,nameImage:String) : String{
        val file = File(context.getFilePath(), ADDRESS_IMAGE)
        if (!file.exists()) file.mkdirs()

        val filePath = file.absolutePath + "/" + nameImage +  ".png"

        val bytes = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fos = FileOutputStream(File(filePath))
        fos.write(bitmapData)
        fos.flush()
        fos.close()
        return filePath
    }

    // ##################################################################
    // ### आपका पुराना कोड यहाँ समाप्त होता है                          ###
    // ##################################################################


    // ##################################################################
    // ### नया कोड यहाँ से जोड़ा गया है                                ###
    // ##################################################################

    /**
     * यह फंक्शन विशेष रूप से ऑडियो/वीडियो रिकॉर्डिंग के लिए एक 'media' फ़ोल्डर बनाता है।
     */
    fun getMediaFilePath(context: Context): String {
        val directory = File(context.filesDir, "media")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory.absolutePath
    }

    /**
     * यह ड्यूरेशन प्राप्त करने का एक सुरक्षित तरीका है, जो त्रुटियों को बेहतर ढंग से संभालता है।
     */
    @SuppressLint("DefaultLocale")
    fun getMediaDurationFormatted(filePath: String?): String {
        if (filePath == null) return "00:00"
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLongOrNull() ?: 0
            retriever.release()

            val seconds = (timeInMillis / 1000) % 60
            val minutes = (timeInMillis / (1000 * 60)) % 60
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }
}