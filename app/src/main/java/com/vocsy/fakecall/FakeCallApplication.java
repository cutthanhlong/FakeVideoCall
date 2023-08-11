package com.vocsy.fakecall;


import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

public class FakeCallApplication extends Application {
    private static Context mContext;

    public void onCreate() {
        super.onCreate();
        mContext = this;

        MultiDex.install(this);
    }

    public static Context getAppContext() {
        return mContext;
    }
}
