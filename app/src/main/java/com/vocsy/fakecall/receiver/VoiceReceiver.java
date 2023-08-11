package com.vocsy.fakecall.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.vocsy.fakecall.AudioCallAlarmService;

public class VoiceReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {


        Intent playIntent = new Intent(context, AudioCallAlarmService.class);


        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(playIntent);
        } else {
            context.startService(playIntent);
        }


    }
}
