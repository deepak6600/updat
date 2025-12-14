package com.safe.setting.app.data.rxFirebase

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.flow.Flow

interface InterfaceFirebase {

    fun getUser(): FirebaseUser?

    suspend fun signIn(email: String, password: String): Result<AuthResult>

    suspend fun signUp(email: String, password: String): Result<AuthResult>

    fun signOut()

    fun valueEvent(child: String): Flow<DataSnapshot>

    fun valueEventAccount(): Flow<DataSnapshot>

    suspend fun queryValueEventSingle(child: String,value:String,id:String): DataSnapshot

    fun <T : Any> valueEventModel(child: String, clazz: Class<T>): Flow<T>

    fun getDatabaseReference(child: String): DatabaseReference

    fun getDatabaseAcount(): DatabaseReference

    // Storage से जुड़े सभी फंक्शन्स यहाँ से हटा दिए गए हैं
    // fun getStorageReference(...)
    // fun getFile(...)
    // fun putFile(...)

}
