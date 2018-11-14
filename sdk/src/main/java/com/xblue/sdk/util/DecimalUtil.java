package com.xblue.sdk.util;

import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * 小数工具类
 * Created by linguobiao on 16/11/16.
 */

public class DecimalUtil {

    /**
     * 保留一位小数
     */
    public static DecimalFormat df_0_0() {
        return new DecimalFormat("0.0", getDecimalFormat());
    }

    /**
     * 保留两位小数
     */
    public static DecimalFormat df_0_00() {
        return new DecimalFormat("0.00", getDecimalFormat());
    }

    /**
     * 保留三位小数
     */
    public static DecimalFormat df_0_000() {
        return new DecimalFormat("0.000", getDecimalFormat());
    }

    /**
     * 保留四位小数
     */
    public static DecimalFormat df_0_0000() {
        return new DecimalFormat("0.0000", getDecimalFormat());
    }

    /**
     * 保留五位小数
     */
    public static DecimalFormat df_0_00000() {
        return new DecimalFormat("0.00000", getDecimalFormat());
    }

    private static DecimalFormatSymbols getDecimalFormat() {
        Locale currentLocale = Locale.ENGLISH;
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(currentLocale);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator('.');
        return otherSymbols;
    }

    /**
     * 整数 000
     */
    public static final DecimalFormat df_000 = new DecimalFormat("000");

    /**
     * 整数 00
     */
    public static final DecimalFormat df_00 = new DecimalFormat("00");
    /**
     * 整数0
     */
    public static final DecimalFormat df_0 = new DecimalFormat("0");

    /**
     * 设置编辑框小数位数
     * @param editText  编辑框
     * @param point 小数位数
     */
    public static void setEditPoint(EditText editText, final int point) {
        editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_CLASS_NUMBER);
        editText.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if(source.equals(".") && dest.toString().length() == 0){
                    return "0.";
                }
                if(dest.toString().contains(".")){
                    int index = dest.toString().indexOf(".");
                    int length = dest.toString().substring(index).length();
                    if(length == (point + 1) && dstart > index){
                        return "";
                    }
                }
                return null;
            }
        }});
    }
}
