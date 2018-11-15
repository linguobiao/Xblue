package com.lgb.xblue.main;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.lgb.xblue.adapter.PullToRefreshAdapter;
import com.lgb.xblue.R;
import com.xblue.sdk.manager.SdkManager;
import com.xblue.sdk.manager.ByteUtils;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by LGB on 2018/11/11.
 */
public class DeviceActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private PullToRefreshAdapter pullToRefreshAdapter;
    private List<SearchResult> mDevices;
    private List<String> mMacs;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);
        findViewById(R.id.bt_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchDevice();
            }
        });
        initView();
    }

    public void initView() {
        mDevices = new ArrayList<>();
        mMacs = new ArrayList<>();
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setColorSchemeColors(Color.rgb(47, 223, 189));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pullToRefreshAdapter = new PullToRefreshAdapter(mDevices);
        mRecyclerView.setAdapter(pullToRefreshAdapter);
        mRecyclerView.addOnItemTouchListener(new OnItemClickListener() {
            @Override
            public void onSimpleItemClick(final BaseQuickAdapter adapter, final View view, final int position) {
                SdkManager.getInstance().getClient().stopSearch();
                Intent intent = new Intent();
                intent.putExtra("mac", ByteUtils.getDevice(mDevices.get(position)).getMac());
                intent.putExtra("name", ByteUtils.getDevice(mDevices.get(position)).getName());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        SdkManager.getInstance().getClient().registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                if (openOrClosed) searchDevice();
            }
        });
        if (!SdkManager.getInstance().getClient().isBluetoothOpened()) {
            SdkManager.getInstance().getClient().openBluetooth();
            return;
        }
        searchDevice();

    }

    private void searchDevice() {
        mDevices.clear();
        mMacs.clear();
        pullToRefreshAdapter.notifyDataSetChanged();
        search();
    }

    @Override public void onRefresh() {searchDevice();}
    private long lastTime = 0;

    public void onSearchResult(SearchResult device) {
        mSwipeRefreshLayout.setRefreshing(false);
//        if ("HMSoft".equals(device.getName()) || "Guoou".equals(device.getName()) || "Coating".equals(device.getName())) {
            String mac = ByteUtils.getDevice(device).getMac();
            if (!mDevices.contains(device) && !mMacs.contains(mac)) {
                mDevices.add(device);
                mMacs.add(mac);
                SdkManager.getInstance().setAvailableDevices(mDevices);
                Collections.sort(mDevices, new Comparator<SearchResult>() {
                    @Override
                    public int compare(SearchResult device1, SearchResult device2) {
                        return (int) (device2.rssi - device1.rssi);
                    }
                });
                pullToRefreshAdapter.notifyDataSetChanged();
            } else {
                for (SearchResult deviceSave : mDevices) {
                    if (deviceSave.getAddress().equals(device.getAddress())) {
                        deviceSave.rssi = device.rssi;
                        break;
                    }
                }
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTime >= 500) {
                    lastTime = currentTime;
                    Collections.sort(mDevices, new Comparator<SearchResult>() {
                        @Override
                        public int compare(SearchResult device1, SearchResult device2) {
                            return (int) (device2.rssi - device1.rssi);
                        }
                    });
                    pullToRefreshAdapter.notifyDataSetChanged();
                }
            }
//        }
    }

    public void search() {
        if (!SdkManager.getInstance().getClient().isBluetoothOpened()) {
            mSwipeRefreshLayout.setRefreshing(false);
            SdkManager.getInstance().getClient().openBluetooth();
            return;
        }
        if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {bluetoothAdapter.cancelDiscovery();}

        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 2)       // 先扫BLE设备2次，每次3s
                .searchBluetoothClassicDevice(5000, 2)  // 再扫经典蓝牙2次，每次5s
                .searchBluetoothLeDevice(3000, 2)       // 再扫BLE设备2次，每次3s
                .searchBluetoothClassicDevice(5000, 2)  // 再扫经典蓝牙2次，每次5s
                .build();
        SdkManager.getInstance().getClient().search(request, mSearchResponse);
    }

    private final SearchResponse mSearchResponse = new SearchResponse() {

        @Override public void onSearchStarted() {}

        @Override
        public void onDeviceFounded(SearchResult device) {
            Log.e("demo","onDeviceFounded:" + device.getName() + ",  " + device.device.getAddress() + ",  " + device.rssi);
            onSearchResult(device);
        }

        @Override public void onSearchStopped() {
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override public void onSearchCanceled() {}
    };

}
