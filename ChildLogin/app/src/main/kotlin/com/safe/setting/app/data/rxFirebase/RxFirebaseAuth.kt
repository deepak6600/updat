package com.safe.setting.app.data.rxFirebase

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.rxjava3.core.Maybe
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RxFirebaseAuth {
        // Internal suspend implementation to keep behavior while migrating
        private suspend fun FirebaseAuth.signInWithEmailAndPasswordSuspend(email: String, password: String): AuthResult =
                suspendCancellableCoroutine { cont ->
                        val task = signInWithEmailAndPassword(email, password)
                        task.addOnSuccessListener { result -> cont.resume(result) }
                                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
                        // Firebase Task has no cancel; rely on listener lifecycle
                }

        fun FirebaseAuth.rxSignInWithEmailAndPassword(email: String, password: String): Maybe<AuthResult> =
                Maybe.create { emitter ->
                        // Bridge suspend -> Rx Maybe, preserving public API
                        try {
                                // Run on current thread; actual threading controlled by callers via subscribeOn/observeOn
                                val result = kotlinx.coroutines.runBlocking { signInWithEmailAndPasswordSuspend(email, password) }
                                emitter.onSuccess(result)
                        } catch (t: Throwable) {
                                emitter.onError(t)
                        }
                }


        private suspend fun FirebaseAuth.createUserWithEmailAndPasswordSuspend(email: String, password: String): AuthResult =
                suspendCancellableCoroutine { cont ->
                        val task = createUserWithEmailAndPassword(email, password)
                        task.addOnSuccessListener { result -> cont.resume(result) }
                                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
                        // Firebase Task has no cancel; rely on listener lifecycle
                }

        fun FirebaseAuth.rxCreateUserWithEmailAndPassword(email: String, password: String): Maybe<AuthResult> =
                Maybe.create { emitter ->
                        try {
                                val result = kotlinx.coroutines.runBlocking { createUserWithEmailAndPasswordSuspend(email, password) }
                                emitter.onSuccess(result)
                        } catch (t: Throwable) {
                                emitter.onError(t)
                        }
                }


        private suspend fun FirebaseAuth.signInWithCredentialSuspend(credential: AuthCredential): AuthResult =
                suspendCancellableCoroutine { cont ->
                        val task = signInWithCredential(credential)
                        task.addOnSuccessListener { result -> cont.resume(result) }
                                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
                        // Firebase Task has no cancel; rely on listener lifecycle
                }

        fun FirebaseAuth.rxSignInWithCredential(credential: AuthCredential) : Maybe<AuthResult> =
                Maybe.create { emitter ->
                        try {
                                val result = kotlinx.coroutines.runBlocking { signInWithCredentialSuspend(credential) }
                                emitter.onSuccess(result)
                        } catch (t: Throwable) {
                                emitter.onError(t)
                        }
                }

}