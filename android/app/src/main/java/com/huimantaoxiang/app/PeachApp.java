package com.huimantaoxiang.app;

import android.app.Application;

import cn.bmob.v3.Bmob;

public class PeachApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Bmob.initialize(this, BuildConfig.BMOB_APP_ID);
    }
}

