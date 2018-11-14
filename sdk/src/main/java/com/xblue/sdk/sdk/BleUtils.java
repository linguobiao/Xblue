package com.xblue.sdk.sdk;

import android.os.CountDownTimer;
import android.text.TextUtils;

import com.xblue.sdk.manager.ByteUtils;
import com.xblue.sdk.manager.SdkManager;
import com.xblue.sdk.api.BleListener;
import com.xblue.sdk.api.SdkApi;
import com.xblue.sdk.api.SemaphoreListener;
import com.xblue.sdk.api.SyncListener;
import com.xblue.sdk.util.LogHelper;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;

/**
 * Created by LGB on 2017/7/21.
 */

public class BleUtils implements SdkApi{

    public UUID uuid_service = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
//    public UUID uuid_service = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public UUID uuid_sync = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
//    public UUID uuid_sync = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    private static final BleConnectOptions options = new BleConnectOptions.Builder()
            .setConnectRetry(0)
            .setConnectTimeout(8000)
            .setServiceDiscoverRetry(0)
            .setServiceDiscoverTimeout(5000)
            .build();

    //倒计时
    private CountDownTimer countDownTimer;
    private int syncTimeOut = 10;//同步超时时间，默认10秒

    private int connectTimes = 5;//搜索次数，默认5次，每次10秒

    private SyncListener syncListener;

    private String boundMac;

    private BleListener connectListener;
    private Semaphore semaphore;

    @Override
    public void initUuid(UUID uuidService, UUID uuidSync) {
        this.uuid_service = uuidService;
        this.uuid_sync = uuidSync;
    }

    @Override
    public boolean isConnect(String mac) {
        return SdkManager.getInstance().getClient().getConnectStatus(mac) == Constants.STATUS_DEVICE_CONNECTED;
    }

    @Override
    public void connectDevice(String mac, BleListener bleListener) {
        if (uuid_service == null || uuid_sync == null) {
            LogHelper.log("uuid can not be null");
            bleListener.onResult(false);
            return;
        }
        SdkManager.getInstance().getClient().stopSearch();
        boundMac = mac;
        connectListener = bleListener;
        SearchRequest request = buildSearchRequest(connectTimes);
        SdkManager.getInstance().getClient().search(request, mSearchResponse);
    }

    private SearchRequest buildSearchRequest(int times){
        SearchRequest.Builder builder = new SearchRequest.Builder();
        for (int i = 0; i < times; i ++) {
            builder.searchBluetoothLeDevice(5000).searchBluetoothClassicDevice(5000);
        }
        return builder.build();
    }

    private final SearchResponse mSearchResponse = new SearchResponse() {

        boolean isConnectSuccess = false;

        @Override public void onSearchStarted() {
            isConnectSuccess = false;
            LogHelper.log("onSearchStarted:");}

        @Override
        public void onDeviceFounded(SearchResult device) {
            LogHelper.log("onDeviceFounded:" + device.getName() + ",  " + device.device.getAddress());
            String mac = ByteUtils.getDevice(device).getMac();
            if (!isConnectSuccess && boundMac.equals(mac)) {
                isConnectSuccess = true;
                SdkManager.getInstance().getClient().stopSearch();
                SdkManager.getInstance().getClient().refreshCache(boundMac);
                connect(boundMac);
            }
        }

        @Override public void onSearchStopped() {
            LogHelper.log("onSearchStopped:");
            if (connectListener != null && !isConnectSuccess) connectListener.onResult(false);
        }

        @Override public void onSearchCanceled() {
            LogHelper.log("onSearchCanceled:");
            if (connectListener != null && !isConnectSuccess) connectListener.onResult(false);
        }
    };

    private void connect(final String mac) {
        SdkManager.getInstance().getClient().connect(mac, BleUtils.options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                if (code == REQUEST_SUCCESS) {
                    LogHelper.log("connected success, open notify...");
                    openNotify(boundMac);
                } else {
                    LogHelper.log("connected fail");
                    if (connectListener != null) connectListener.onResult(false);
                    SdkManager.getInstance().getClient().disconnect(mac);
                }
            }
        });
    }

    // 写BLE设备
    private void write(String boundMac, byte[] bytes) {
        write(boundMac, bytes, false);
    }
    private void write(String boundMac, byte[] bytes, boolean isSemaphore) {
        LogHelper.log("write : " + Arrays.toString(bytes));
        SdkManager.getInstance().getClient().write(boundMac, uuid_service, uuid_sync, bytes, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {
                    LogHelper.log("write success");
                } else {
                    cancelTimer();
                    fail("write value fail");
                }
            }
        });
    }

    // 打开设备通知
    private void openNotify(final String boundMac) {
        SdkManager.getInstance().getClient().notify(boundMac, uuid_service, uuid_sync, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                LogHelper.log("notify : " + Arrays.toString(value));
                syncListener.onResult(true, value, "sync success");
            }

            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {
                    if (connectListener != null) connectListener.onResult(true);
                } else {
                    if (connectListener != null) connectListener.onResult(false);
                    SdkManager.getInstance().getClient().disconnect(boundMac);
                }
            }
        });
    }

    private void fail(String log) {
        if (syncListener != null) syncListener.onResult(false, null, LogHelper.buildLog(log));
    }

    @Override
    public void sync(String mac, byte[] value) {
        if (TextUtils.isEmpty(mac)) {
            fail("Bluetooth address is not available");
            return;
        }
        initTimer();
        write(mac, value);
    }

    @Override
    public void setSyncTimeOut(int seconds) {
        syncTimeOut = seconds;
    }

    @Override
    public void setConnectTimes(int times) {
        connectTimes = times;
    }

    private void runSemaphore() {
        semaphore = new Semaphore(0);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SemaphoreListener semaphoreListener = new SemaphoreListener() {
        @Override
        public void onSync(boolean isSuccess, byte[] value) {
            if (semaphore != null) semaphore.release();
            cancelTimer();
        }
    };

    @Override
    public void clean() {
        cancelTimer();
    }

    @Override
    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    private void initTimer() {
        if (countDownTimer == null) {
            countDownTimer = new CountDownTimer(syncTimeOut * 1000, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    cancelTimer();
                    fail("sync time out");
                    if (semaphore != null) semaphoreListener.onSync(false, null);
                }
            };
        }
        countDownTimer.start();
    }

    @Override
    public void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
