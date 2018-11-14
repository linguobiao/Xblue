package com.lgb.xblue.util;

import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by LGB on 2018/11/11.
 */
public class StringHelper {

    public static byte[] buildBytes(String text, boolean isHex) {

        if (TextUtils.isEmpty(text)) {
            Toast.makeText(ContextHelper.getInstance().getApplicationContext(), "输入数据不合法", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (isHex) { //HEX
            String regex="^[A-Fa-f0-9]+$";
            if(!text.matches(regex)){
                Toast.makeText(ContextHelper.getInstance().getApplicationContext(), "输入数据不合法", Toast.LENGTH_SHORT).show();
                return null;
            }
            return ByteUtil.hexStringToByte(text);
        } else {    //字符串
            return ByteUtil.strToByte(text);
        }
    }


    public static void initInput(EditText editText, boolean isHex) {
        String input = editText.getText().toString().trim().replaceAll(" ", "");
        if (TextUtils.isEmpty(input)) {
            return;
        }
        if (isHex) { //字符串 -> HEX
            byte[] value = ByteUtil.strToByte(input);
            editText.setText(ByteUtil.byteToHexString(value));
            editText.setSelection(editText.getText().toString().length());
        } else {                        //HEX -> 字符串
            byte[] value = ByteUtil.hexStringToByte(input);
            editText.setText(ByteUtil.bytesToString(value));
            editText.setSelection(editText.getText().toString().length());
        }
    }
}
