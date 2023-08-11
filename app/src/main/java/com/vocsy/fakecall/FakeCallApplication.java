package com.vocsy.fakecall;


import android.content.Context;

import androidx.multidex.MultiDex;

import vocsy.ads.AdsApplication;


public class FakeCallApplication extends AdsApplication {
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
