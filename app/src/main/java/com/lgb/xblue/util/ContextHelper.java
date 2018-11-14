package com.lgb.xblue.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

/**
 * Created by LGB on 2018/11/11.
 */

public class ContextHelper {

    /**
     * 全局 ApplicationContext
     */
    private static Context context;

    private static class ContextInstance {
        private static final ContextHelper instance = new ContextHelper();
    }

    public static ContextHelper getInstance() {
        return ContextHelper.ContextInstance.instance;
    }

    /**
     * 初始化工具类,在Aplication进行初始化
     *
     * @param context 上下文
     */
    public void init(Context context) {
        if(null == ContextHelper.context) ContextHelper.context = context.getApplicationContext();
    }

    /**
     * 获取ApplicationContext
     *
     * @return ApplicationContext
     */
    public Context getApplicationContext() {
        if (context != null) return context;
        throw new NullPointerException("u should init first");
    }

    /**
     * View获取Activity的工具
     *
     * @param view view
     * @return Activity
     */
    public @NonNull
    Activity getActivity(View view) {
        Context context = view.getContext();

        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        throw new IllegalStateException("View " + view + " is not attached to an Activity");
    }

    /**
     * 全局获取String的方法
     *
     * @param id 资源Id
     * @return String
     */
    public String getString(@StringRes int id) {
        return context.getResources().getString(id);
    }

}
