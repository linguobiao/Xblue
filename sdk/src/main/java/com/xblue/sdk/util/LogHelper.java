package com.xblue.sdk.util;

import android.util.Log;

/**
 * Created by LGB on 2018/4/24.
 */

public class LogHelper {
    private final static boolean SHOW_LOG = true;
    private final static String _SDK = "Xblue";
    public static void log(String msg) {
        if (!SHOW_LOG) return;
        Log.d(_SDK, msg);
    }

    public static String buildLog(String log) {
        return log;
    }

}
