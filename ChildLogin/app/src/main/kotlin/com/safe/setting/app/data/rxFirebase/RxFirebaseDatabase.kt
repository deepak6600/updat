package com.safe.setting.app.data.rxFirebase

import android.util.Log
import com.safe.setting.app.utils.Consts.TAG
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
// import com.pawegio.kandroid.e // **** पुराना इम्पोर्ट हटा दिया गया ****
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.rx3.asFlowable

object RxFirebaseDatabase {

    fun Query.rxObserveValueEvent(auth : FirebaseAuth): Flowable<DataSnapshot> {
        // Internal coroutine-based stream; bridge to Rx Flowable while preserving DROP semantics
        val flow = callbackFlow<DataSnapshot> {
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    trySend(dataSnapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    try {
                        if (auth.currentUser != null) close(Throwable(error.message))
                    } catch (er: OnErrorNotImplementedException) {
                        Log.e(TAG, er.message.toString())
                    } catch (t: Throwable) {
                        Log.e(TAG, t.message.toString())
                    }
                }
            }
            addValueEventListener(valueEventListener)
            awaitClose { removeEventListener(valueEventListener) }
        }
        // Note: asFlowable() doesn't take BackpressureStrategy in rx3; we'll keep default buffering and
        // preserve external BackpressureStrategy.DROP at call sites if needed.
        return flow.asFlowable()
    }

    fun Query.rxObserveSingleValueEvent(): Maybe<DataSnapshot> {
        return Maybe.create { emitter ->
            addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) = emitter.onSuccess(dataSnapshot)
                override fun onCancelled(error: DatabaseError) { if (!emitter.isDisposed) emitter.onError(Throwable(error.message)) }
            })
        }
    }

}
