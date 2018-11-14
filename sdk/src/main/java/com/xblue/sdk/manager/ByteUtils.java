package com.xblue.sdk.manager;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.search.SearchResult;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.xblue.sdk.util.LogHelper;

/**
 * Created by LGB on 2017/7/21.
 */

public class ByteUtils {

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序。
     *
     * @param ary
     *            byte数组
     * @return int数值
     */
    public static int byte2int(byte[] ary, int offset) {
        return ((ary[offset]&0xFF) | ((ary[offset+1]<<8) & 0xFF00));
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序。
     *
     * @param ary
     *            byte数组
     * @param offset
     *            从数组的第offset位开始
     * @return int数值
     */
    public static long byte2long(byte[] ary, int offset) {
        long value;
        value = (long) ((ary[offset]&0xFF)
                | ((ary[offset+1]<<8) & 0xFF00)
                | ((ary[offset+2]<<16)& 0xFF0000)
                | ((ary[offset+3]<<24) & 0xFF000000));
        return value;
    }

    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        float result = Float.intBitsToFloat(l);
        if (Float.isNaN(result)) return -1f;
        return result;
    }

    public static DeviceBean getDevice(SearchResult searchResult) {
        DeviceBean deviceBean = new DeviceBean();
        String mac = searchResult.getAddress();
        String name = searchResult.getName();
        BluetoothDevice device = searchResult.device;
        Beacon beacon = new Beacon(searchResult.scanRecord);
        LogHelper.log("----scanRecord:" + searchResult.scanRecord + ",  " + beacon.toString());
        BleAdvertisedData bleAdvertisedData = ByteUtils.parseAdertisedData(searchResult.scanRecord);
        LogHelper.log("----" + searchResult.getName() + ", " + searchResult.getAddress() + ", " + device.getType() + ", " + device.getName() + ", " + device.toString() + ", " + bleAdvertisedData.getName());
        if (device != null) {
            if (1 == device.getType() || 3 == device.getType()) {
                if (mac != null && mac.startsWith("00:0E:0E")) {
                    mac = mac.replace("00:0E:0E", "00:0E:0B");
                    if (TextUtils.isEmpty(name) || "null".equals(name) || "NULL".equals(name)) {
                        name = "HMSoft";
                    }
                }
            }
        }
        deviceBean.setMac(mac);
        deviceBean.setName(name);
        return deviceBean;
    }

    public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();
        String name = null;
        if( advertisedData == null ){
            return new BleAdvertisedData(uuids, name);
        }

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length-1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return new BleAdvertisedData(uuids, name);
    }

    public static class BleAdvertisedData {
        private List<UUID> mUuids;
        private String mName;
        public BleAdvertisedData(List<UUID> uuids, String name){
            mUuids = uuids;
            mName = name;
        }

        public List<UUID> getUuids(){
            return mUuids;
        }

        public String getName(){
            return mName;
        }
    }

}
