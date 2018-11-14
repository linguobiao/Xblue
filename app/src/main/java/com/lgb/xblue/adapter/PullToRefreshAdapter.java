package com.lgb.xblue.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.lgb.xblue.R;
import com.xblue.sdk.manager.DeviceBean;
import com.xblue.sdk.manager.ByteUtils;
import com.inuker.bluetooth.library.search.SearchResult;

import java.util.List;

/**
 * Created by LGB on 2018/11/11.
 */
public class PullToRefreshAdapter extends BaseQuickAdapter<SearchResult, BaseViewHolder> {
    public PullToRefreshAdapter(List<SearchResult> list) {
        super( R.layout.item_device, list);
    }

    @Override
    protected void convert(BaseViewHolder viewHolder, SearchResult item) {
        DeviceBean deviceBean = ByteUtils.getDevice(item);
        viewHolder.setText(R.id.tv_device_name, deviceBean.getName())
                .setText(R.id.tv_device_mac, deviceBean.getMac())
                .setText(R.id.tv_device_rssi, item.rssi + "dbm");
    }
}
