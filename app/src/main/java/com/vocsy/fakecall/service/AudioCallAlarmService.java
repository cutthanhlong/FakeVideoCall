package com.vocsy.fakecall.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.vocsy.fakecall.ui.VoiceCallScreenActivity;

public class AudioCallAlarmService extends Service {
    //private Intent alarmIntent;

    public void onCreate() {

        Log.e("onCreate","onCreate");
        a();
        Intent alarmIntent = new Intent(this, VoiceCallScreenActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(alarmIntent);
        stopSelf();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

       // Log.e("onStartCommand","onStartCommand");


        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {

        Log.e("onDestroy","onDestroy");
        try {
            a(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public static void a(Context context) {
        ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE)).cancel(34648);
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void a() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("Lock_notification_start_service", "Lock notification service", NotificationManager.IMPORTANCE_NONE));
            startForeground(34648, new NotificationCompat.Builder(this, "Lock_notification_start_service").setCategory(NotificationCompat.CATEGORY_SERVICE).setPriority(0).build());
        }
    }
}
