package com.safe.setting.app.data.rxFirebase

import android.util.Log
import com.safe.setting.app.utils.Consts.TAG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
// import com.pawegio.kandroid.e // **** पुराना इम्पोर्ट हटा दिया गया ****
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RxFirebaseDatabase {

    fun Query.rxObserveValueEvent(auth : FirebaseAuth): Flow<DataSnapshot> {
        // Pure Kotlin Flow using callbackFlow to mirror Rx value events
        return callbackFlow<DataSnapshot> {
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    trySend(dataSnapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    try {
                        if (auth.currentUser != null) close(Throwable(error.message))
                    } catch (t: Throwable) {
                        Log.e(TAG, t.message.toString())
                    }
                }
            }
            addValueEventListener(valueEventListener)
            awaitClose { removeEventListener(valueEventListener) }
        }
    }

    suspend fun Query.rxObserveSingleValueEvent(): DataSnapshot =
        suspendCancellableCoroutine { cont ->
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    cont.resume(dataSnapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resumeWithException(Throwable(error.message))
                }
            }
            addListenerForSingleValueEvent(listener)
            cont.invokeOnCancellation {
                // No explicit remove for single event; cancellation is rare here.
            }
        }

}
