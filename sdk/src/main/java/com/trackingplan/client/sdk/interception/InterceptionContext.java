// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.sdk.interception;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.util.ScreenViewTracker;
import com.trackingplan.client.sdk.util.ServiceLocator;

import java.util.List;
import java.util.Locale;

public class InterceptionContext {

    private static volatile boolean initialized = false;

    public static String appName = "";
    public static String appVersion = "";
    public static String device = "";
    public static String platform = "";
    public static String language = "";

    public String activityName = "";
    public String screenName = "";

    public String instrument = "";

    @NonNull
    public static InterceptionContext createInterceptionContext(@NonNull final Context context) {

        if (!initialized) {
            synchronized (InterceptionContext.class) {
                if (!initialized) {
                    initCommonContext(context);
                    initialized = true;
                }
            }
        }

        var interceptionContext = new InterceptionContext();

        final String activityName = getTopActivityName(context);
        if (!activityName.isEmpty()) {
            interceptionContext.activityName = activityName;
        }

        final var screenViewTracker = ServiceLocator.tryGetSharedInstance(ScreenViewTracker.class);
        if (screenViewTracker != null) {
            interceptionContext.screenName = screenViewTracker.getLastScreenName();
        }

        return interceptionContext;
    }

    private static void initCommonContext(@NonNull final Context context) {

        final PackageManager packageManager = context.getPackageManager();
        final ApplicationInfo applicationInfo = context.getApplicationInfo();

        device = Build.MANUFACTURER + " " + Build.MODEL;
        platform = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";

        appName = (String) packageManager.getApplicationLabel(applicationInfo);
        language = context.getResources().getConfiguration().getLocales().toLanguageTags();

        try {
            PackageInfo pInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            appVersion = "unknown";
        }
    }

    @NonNull
    private static String getTopActivityName(@NonNull Context context) {

        String currentActivityName = "";

        try {
            final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.AppTask> taskInfo = am.getAppTasks();

            if (!taskInfo.isEmpty()) {
                final ActivityManager.RecentTaskInfo task = taskInfo.get(0).getTaskInfo();
                final ComponentName componentName = task.topActivity;
                if (componentName != null) {
                    currentActivityName = componentName.getClassName();
                }
            }
        } catch (Exception ignored) {
            // Fail silently
        }

        return currentActivityName;
    }
}
