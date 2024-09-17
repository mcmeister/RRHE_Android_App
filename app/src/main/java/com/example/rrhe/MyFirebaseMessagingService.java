package com.example.rrhe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM Service";
    private static final String FCM_TOPIC = "new_apk_available";

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived: Message received from: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "onMessageReceived: Notification received: " + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), remoteMessage.getData());
        } else {
            Log.d(TAG, "onMessageReceived: No notification payload in the message.");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken: Refreshed token: " + token);

        // Subscribe to the FCM topic for APK updates
        FirebaseMessaging.getInstance().subscribeToTopic(FCM_TOPIC)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onNewToken: Subscribed to topic: " + FCM_TOPIC);
                    } else {
                        Log.e(TAG, "onNewToken: Failed to subscribe to topic: " + FCM_TOPIC);
                    }
                });

        String userName = getCurrentUserName();
        if (userName != null && !userName.isEmpty()) {
            Log.d(TAG, "onNewToken: User name found, sending registration to server.");
            sendRegistrationToServer(token, userName);
        } else {
            Log.d(TAG, "onNewToken: User name not found, storing token for later.");
            storeTokenForLater(token);
        }
    }

    private void sendRegistrationToServer(String token, String userName) {
        Log.d(TAG, "sendRegistrationToServer: Sending token to server for user: " + userName);
        ApiService apiService = ApiClient.getStaticClient(ApiConfig.getStaticBaseUrl(ApiClient.isStaticEmulator())).create(ApiService.class);

        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("user_name", userName);
        tokenData.put("fcm_token", token);

        apiService.updateFcmToken(tokenData).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "sendRegistrationToServer: Token sent to server successfully.");
                } else {
                    Log.e(TAG, "sendRegistrationToServer: Failed to send token to server. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "sendRegistrationToServer: Error sending token to server: " + t.getMessage());
            }
        });
    }

    private String getCurrentUserName() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", null);
        Log.d(TAG, "getCurrentUserName: Retrieved userName: " + userName);
        return userName;
    }

    private void storeTokenForLater(String token) {
        Log.d(TAG, "storeTokenForLater: Storing token locally for later use.");
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("fcmToken", token).apply();
    }

    public static void sendStoredToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("fcmToken", null);
        String userName = sharedPreferences.getString("userName", null);

        if (token != null && userName != null && !userName.isEmpty()) {
            Log.d(TAG, "sendStoredToken: Sending stored token to server for user: " + userName);
            new MyFirebaseMessagingService().sendRegistrationToServer(token, userName);
            sharedPreferences.edit().remove("fcmToken").apply();
        } else {
            Log.d(TAG, "sendStoredToken: No stored token or user name available.");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        Log.d(TAG, "sendNotification: Preparing to send notification with title: " + title);
        String downloadUrl = data.get("download_url");

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            Log.d(TAG, "sendNotification: App update detected. Preparing to handle app update notification.");
            handleAppUpdateNotification(title, messageBody, downloadUrl);  // Use the `title` parameter here
        } else {
            Log.d(TAG, "sendNotification: No app update URL found, handling as planted end notification.");
            handlePlantedEndNotification(title, messageBody, data);
        }
    }

    private void handleAppUpdateNotification(String title, String messageBody, String downloadUrl) {
        Log.d(TAG, "handleAppUpdateNotification: Handling app update notification.");
        AppNotificationManager notificationManager = new AppNotificationManager(this);
        notificationManager.showAppUpdateNotification(title, messageBody, downloadUrl);
    }

    private void handlePlantedEndNotification(String title, String messageBody, Map<String, String> data) {
        Log.d(TAG, "handlePlantedEndNotification: Handling planted end notification.");

        // If you receive a list, you need to handle this list here and pass it to showScheduledNotification
        List<Map<String, String>> plants = new ArrayList<>();
        Map<String, String> plantData = new HashMap<>();
        plantData.put("NameConcat", Objects.requireNonNull(data.get("NameConcat")));
        plantData.put("TableName", Objects.requireNonNull(data.get("TableName")));
        plants.add(plantData);

        AppNotificationManager notificationManager = new AppNotificationManager(this);
        notificationManager.showScheduledNotification(title, messageBody, plants);
    }
}