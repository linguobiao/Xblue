package com.xblue.sdk.api;

/**
 * Created by LGB on 2018/4/24.
 */

public interface SemaphoreListener {
    void onSync(boolean isSuccess, byte[] value);
}
