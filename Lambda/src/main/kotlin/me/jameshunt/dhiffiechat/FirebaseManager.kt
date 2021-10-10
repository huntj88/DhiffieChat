package me.jameshunt.dhiffiechat

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

class FirebaseManager(serverCredentials: ServerCredentials) {

    private val serviceAccount = serverCredentials.firebaseJson.byteInputStream()
    private val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    init {
        FirebaseApp.initializeApp(options)
    }

    fun sendMessage(token: String, title: String, body: String) {
        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()
        
        FirebaseMessaging.getInstance().send(
            Message.builder()
                .setNotification(notification)
                .setToken(token)
                .build()
        )
    }
}