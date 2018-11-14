package com.xblue.sdk.api;

public interface SyncListener {
    void onResult(boolean result, byte value[], String log);
}
