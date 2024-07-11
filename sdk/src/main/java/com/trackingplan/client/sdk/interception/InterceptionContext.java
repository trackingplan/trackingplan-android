// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.sdk.interception;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;

public class InterceptionContext {
    public String activityName = "";
    public String instrument = "";


    @NonNull
    public static InterceptionContext createInterceptionContext(Context context) {

        var interceptionContext = new InterceptionContext();

        String activityName = getTopActivityName(context);
        if (activityName != null && !activityName.isEmpty()) {
            interceptionContext.activityName = activityName;
        }

        return interceptionContext;
    }

    private static String getTopActivityName(@NonNull Context context) {

        String name = null;

        try {
            // NOTE: Works without any extra permission in Android 5.0+
            // NOTE: This API is deprecated in newer versions of Android
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
            name = cn.getClassName();

        } catch (Exception ignored) {
            // Fail silently
        }

        return name;
    }
}
