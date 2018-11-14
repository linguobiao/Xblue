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
        SdkManager.getInstance().init(this);
        ContextHelper.getInstance().init(this);
    }
}
