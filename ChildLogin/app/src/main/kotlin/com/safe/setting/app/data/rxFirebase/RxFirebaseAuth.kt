package com.safe.setting.app.data.rxFirebase

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RxFirebaseAuth {
        // Suspend implementations returning Result for safe error handling
        private suspend fun FirebaseAuth.signInWithEmailAndPasswordSuspend(email: String, password: String): AuthResult =
                suspendCancellableCoroutine { cont ->
                        val task = signInWithEmailAndPassword(email, password)
                        task.addOnSuccessListener { result -> cont.resume(result) }
                                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
                        // Firebase Task has no cancel; rely on listener lifecycle
                }

        suspend fun FirebaseAuth.signInWithEmailAndPasswordResult(email: String, password: String): Result<AuthResult> =
                try { Result.success(signInWithEmailAndPasswordSuspend(email, password)) }
                catch (t: Throwable) { Result.failure(t) }


        private suspend fun FirebaseAuth.createUserWithEmailAndPasswordSuspend(email: String, password: String): AuthResult =
                suspendCancellableCoroutine { cont ->
                        val task = createUserWithEmailAndPassword(email, password)
                        task.addOnSuccessListener { result -> cont.resume(result) }
                                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
                        // Firebase Task has no cancel; rely on listener lifecycle
                }

        suspend fun FirebaseAuth.createUserWithEmailAndPasswordResult(email: String, password: String): Result<AuthResult> =
                try { Result.success(createUserWithEmailAndPasswordSuspend(email, password)) }
                catch (t: Throwable) { Result.failure(t) }


        private suspend fun FirebaseAuth.signInWithCredentialSuspend(credential: AuthCredential): AuthResult =
                suspendCancellableCoroutine { cont ->
                        val task = signInWithCredential(credential)
                        task.addOnSuccessListener { result -> cont.resume(result) }
                                .addOnFailureListener { ex -> cont.resumeWithException(ex) }
                        // Firebase Task has no cancel; rely on listener lifecycle
                }

        suspend fun FirebaseAuth.signInWithCredentialResult(credential: AuthCredential) : Result<AuthResult> =
                try { Result.success(signInWithCredentialSuspend(credential)) }
                catch (t: Throwable) { Result.failure(t) }

}