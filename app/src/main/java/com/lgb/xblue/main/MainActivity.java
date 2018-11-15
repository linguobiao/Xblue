package com.lgb.xblue.main;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.lgb.xblue.R;
import com.lgb.xblue.util.ByteUtil;
import com.lgb.xblue.util.PermissionHelper;
import com.lgb.xblue.util.StringHelper;
import com.xblue.sdk.api.BleListener;
import com.xblue.sdk.api.SyncListener;
import com.xblue.sdk.manager.SdkManager;

/**
 * Created by LGB on 2018/11/11.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SyncListener{

    private TextView tv_state, tv_show, tv_byte_show, tv_byte_input;
    private EditText et_input;
    private Button bt_connect;
    private Switch switch_show, switch_input;
    private ScrollView scrollView;

    private StringBuilder showHexBuilder = new StringBuilder();
    private StringBuilder showStrBuilder = new StringBuilder();
    private String mac;
    private String name;

    private int byte_show, byte_input;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scrollView = findViewById(R.id.scrollView);
        tv_state = findViewById(R.id.tv_state);
        tv_show = findViewById(R.id.tv_show);
        tv_byte_show = findViewById(R.id.tv_byte_show);
        tv_byte_input = findViewById(R.id.tv_byte_input);
        et_input = findViewById(R.id.et_input);
        bt_connect = findViewById(R.id.bt_connect);
        switch_show = findViewById(R.id.switch_show);
        switch_input = findViewById(R.id.switch_input);
        bt_connect.setOnClickListener(this);
        findViewById(R.id.bt_clear_input).setOnClickListener(this);
        findViewById(R.id.bt_clear_show).setOnClickListener(this);
        findViewById(R.id.bt_send).setOnClickListener(this);
        switch_show.setOnCheckedChangeListener(this);
        switch_input.setOnCheckedChangeListener(this);
        switch_show.setChecked(false);
        switch_input.setChecked(false);
        //设置同步数据监听器，监听同步结果
        SdkManager.getInstance().setSyncListener(this);

        String str = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\n";
        et_input.setKeyListener(DigitsKeyListener.getInstance(str));
        et_input.setRawInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);//默认显示英文键盘
//        byte[] bytes = new byte[127];
//        for (int i = 0; i < 127; i ++) {
//            bytes[i] = (byte) i;
//        }
//        LogHelper.log(ByteUtil.bytesToString(bytes));

//        byte[] value = new byte[20];
//        value[3] = 55;
//        value[4] = 02;
//        value[6] = (byte)0x0a;
//        value[9] = 88;
//        onResult(true, value, "");
    }

    @Override public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_connect:   //点击连接
                if (bt_connect.getText().toString().equals("连接")) {
                    // 拥有蓝牙权限 -> 设备连接页面
                    if (PermissionHelper.checkBluetoothPermission(this)) startActivityForResult(new Intent(MainActivity.this, DeviceActivity.class), 1001);
                } else {
                    SdkManager.getInstance().getClient().disconnect(mac);
                }
                break;
            case R.id.bt_clear_show:    //点击清空显示
                clear(true);
                break;
            case R.id.bt_clear_input:   //点击清空输入
                clear(false);
                break;
            case R.id.bt_send:      //点击发送
                if (!SdkManager.getInstance().isConnect(mac)) {
                    Toast.makeText(MainActivity.this, "请先连接设备", Toast.LENGTH_SHORT).show();
                    return;
                }
                //构建指令
                byte[] value = StringHelper.buildBytes(et_input.getText().toString().trim().replaceAll(" ", ""), switch_input.isChecked());
                //发送指令
                if (value != null) {
                    SdkManager.getInstance().sync(mac, value);
                    initByteNumber(byte_input + value.length, false);//刷新字节数
                }
                break;
            default:break;
        }
    }

    @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.switch_show:
                switch_show.setText(b ? "HEX显示" : "字符显示");
                setShow(b ? showHexBuilder.toString() : showStrBuilder.toString());
                break;
            case R.id.switch_input:
                switch_input.setText(b ? "HEX输入" : "字符输入");
                StringHelper.initInput(et_input, switch_input.isChecked());
                break;
            default:break;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            mac = data.getStringExtra("mac");
            name = data.getStringExtra("name");
            //设置设备连接状态监听器
            SdkManager.getInstance().getClient().registerConnectStatusListener(mac, bleConnectStatusListener);
            //连接设备
            connect();
        }
    }

    private ProgressDialog dialog;
    /**
     * 连接设备
     */
    private void connect() {
        tv_state.setText("连接中...");
        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setMessage("连接中...");
        }
        dialog.show();
        SdkManager.getInstance().connectDevice(mac, new BleListener() {
            @Override
            public void onResult(boolean isSuccess) {
                tv_state.setText(isSuccess ? "已连接：" + name : "未连接");
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "connect:" + isSuccess, Toast.LENGTH_SHORT).show();
                clear(true);
                clear(false);
            }
        });
    }

    /**
     * 设备连接状态监听器
     */
    private BleConnectStatusListener bleConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == Constants.STATUS_CONNECTED) {
                tv_state.setText("已连接：" + name);
                bt_connect.setText("断开连接");
            } else if (status == Constants.STATUS_DISCONNECTED) {
                tv_state.setText("未连接");
                bt_connect.setText("连接");
            }
        }
    };

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 同意蓝牙权限 -> 设备连接页面
        if (PermissionHelper.isPermision(requestCode, permissions, grantResults)) startActivityForResult(new Intent(MainActivity.this, DeviceActivity.class), 1001);
    }

    /**
     * 同步数据结果
     * @param result 是否同步成功
     * @param value 同步成功后收到设备返回的数据
     * @param log 错误信息
     */
    @Override public void onResult(boolean result, byte[] value, String log) {
        if (result) {//同步成功
            if (value == null) return;
            initByteNumber(byte_show + value.length, true);//刷新字节数
            //byte -> hex
            String hex = ByteUtil.byteToHexString(value);
            showHexBuilder.append(hex);
            //byte -> str
            String str = ByteUtil.bytesToString(value);
            showStrBuilder.append(str);
            //显示收到的数据
            setShow(switch_show.isChecked() ? showHexBuilder.toString() : showStrBuilder.toString());
        } else {//同步失败
            Toast.makeText(MainActivity.this, log, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示接收到的数据
     */
    private void setShow(String str) {
        tv_show.setText(str);
        scrollView.post(new Runnable() {
            @Override public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void initByteNumber(int number, boolean isShow) {
        if (isShow) {
            byte_show = number;
            tv_byte_show.setText("已接收：" + number + "字节");
        } else {
            byte_input = number;
            tv_byte_input.setText("已发送：" + number + "字节");
        }
    }

    private void clear(boolean isShow) {
        if (isShow) {
            showHexBuilder.setLength(0);
            showStrBuilder.setLength(0);
            tv_show.setText(null);
            initByteNumber(0, true);//刷新字节数
        } else {
            et_input.setText(null);
            initByteNumber(0, false);//刷新字节数
        }
    }

    @Override
    protected void onDestroy() {
        SdkManager.getInstance().getClient().unregisterConnectStatusListener(mac, bleConnectStatusListener);
        SdkManager.getInstance().getClient().disconnect(mac);
        super.onDestroy();
    }
}
