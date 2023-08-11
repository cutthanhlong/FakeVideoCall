package com.vocsy.fakecall.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class MyPreferences {

    private Activity activity;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public MyPreferences(Activity activity) {
        this.activity = activity;
        this.preferences = activity.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        this.editor = this.preferences.edit();
    }

    public boolean isUserHasPremium() {
        return preferences.getBoolean("premium", false);
    }

    public boolean isVibrate() {
        return preferences.getBoolean("vibrate", false);
    }

    public int getAutoCutSecond() {
        int second = preferences.getInt("second", 30);
        return second != 0 ? second : 30;
    }

    public void setPremium(boolean premium) {
        editor.putBoolean("premium", premium);
        editor.apply();
    }

    public void setVibrate(boolean vibrate) {
        editor.putBoolean("vibrate", vibrate);
        editor.apply();
    }

    public void setAutoCutSecond(int second) {
        editor.putInt("second", second);
        editor.apply();
    }
}
