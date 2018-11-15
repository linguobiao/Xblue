package com.lgb.xblue;

import android.app.Application;

import com.lgb.xblue.util.ContextHelper;
import com.xblue.sdk.manager.SdkManager;

/**
 * Created by LGB on 2018/11/11.
 */

public class GoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化蓝牙模块
        SdkManager.getInstance().init(this);
        //初始化Context
        ContextHelper.getInstance().init(this);
    }
}
