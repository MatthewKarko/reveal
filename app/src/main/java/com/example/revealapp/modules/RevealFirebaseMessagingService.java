package com.example.revealapp.modules;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class RevealFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Use the token as needed or send it to your server for testing.
        Log.d("FCM Token", token);
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Logic to handle incoming messages
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        Log.d("Received: ", remoteMessage.getFrom());
        // Send a broadcast to MainActivity
        Intent intent = new Intent("com.example.notificationapp.NOTIFICATION");
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        sendBroadcast(intent);
    }
}
