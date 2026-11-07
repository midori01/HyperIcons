package com.midori.hypericons;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

import java.util.Map;

public class IconConfigProvider extends ContentProvider {
    public static final String AUTHORITY = "com.midori.hypericons.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/config");

    public static final String METHOD_GET_CONFIG = "getConfig";
    public static final String METHOD_SET_CONFIG = "setConfig";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            MainActivity.getSafePrefs(context);
            MainActivity.fixPermissions(context);
        }
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null) return null;

        SharedPreferences prefs = MainActivity.getSafePrefs(context);
        if (prefs == null) return null;

        if (METHOD_GET_CONFIG.equals(method)) {
            Bundle bundle = new Bundle();
            final SharedPreferences stored = prefs;
            Map<String, String> config = ConfigKeys.resolve(new ConfigKeys.Source() {
                @Override
                public String get(String key, String def) {
                    return stored.getString(key, def);
                }
            });
            for (Map.Entry<String, String> entry : config.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }

            // Experimental: 5G+ Feature Center keep-alive
            bundle.putBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, prefs.getBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, false));

            return bundle;
        } else if (METHOD_REQUEST_HOOK_REPORTS.equals(method)) {
            if (!isTrustedWriter(Binder.getCallingUid())) return null;
            long now = System.currentTimeMillis();
            long token = Binder.clearCallingIdentity();
            try {
                context.sendBroadcast(new android.content.Intent(HookEnv.ACTION_REQUEST_REPORT));
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            Bundle bundle = new Bundle();
            bundle.putLong("time", now);
            return bundle;
        } else if (METHOD_GET_HOOK_REPORTS.equals(method)) {
            if (!isTrustedWriter(Binder.getCallingUid())) return null;
            // One process per call, flattened to a single line: fields joined by '|', lines by ';'.
            String process = arg != null ? arg : "systemui";
            String report = prefs.getString(KEY_HOOK_REPORT_PREFIX + process, "");
            Bundle bundle = new Bundle();
            bundle.putString("report", report.trim().replace('\t', '|').replace('\n', ';'));
            return bundle;
        } else if (HookEnv.METHOD_REPORT_HOOKS.equals(method) && extras != null) {
            String process = extras.getString(HookEnv.EXTRA_PROCESS);
            String report = extras.getString(HookEnv.EXTRA_REPORT);
            if (report == null || !isHookedProcess(Binder.getCallingUid(), process)) return null;
            prefs.edit().putString(KEY_HOOK_REPORT_PREFIX + process, report).apply();
            return Bundle.EMPTY;
        } else if (METHOD_SET_CONFIG.equals(method) && extras != null) {
            if (!isTrustedWriter(Binder.getCallingUid())) return null;
            SharedPreferences.Editor editor = prefs.edit();
            for (String key : ConfigKeys.allStringKeys()) {
                if (extras.containsKey(key)) {
                    editor.putString(key, extras.getString(key));
                }
            }
            if (extras.containsKey(MainActivity.KEY_ENABLE_5GA_KEEPALIVE)) {
                editor.putBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, extras.getBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, false));
            }
            editor.commit();
            MainActivity.fixPermissions(context);
            context.getContentResolver().notifyChange(CONTENT_URI, null);
            return Bundle.EMPTY;
        }

        return null;
    }

    private static final int ROOT_UID = 0;
    private static final int SHELL_UID = 2000;

    private static boolean isTrustedWriter(int uid) {
        return uid == Process.myUid() || uid == ROOT_UID || uid == SHELL_UID;
    }

    public static final String KEY_HOOK_REPORT_PREFIX = "hook_report_";
    /** Hook reports for tooling (adb shell, root and the app itself). */
    public static final String METHOD_GET_HOOK_REPORTS = "getHookReports";
    /** Asks SystemUI and the phone process to report again (tooling only). */
    public static final String METHOD_REQUEST_HOOK_REPORTS = "requestHookReports";

    /** Only the hooked SystemUI and phone processes may report their hook status. */
    private boolean isHookedProcess(int uid, String process) {
        String expected = "systemui".equals(process) ? "com.android.systemui"
                : "phone".equals(process) ? "com.android.phone" : null;
        if (expected == null) return false;
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages == null) return false;
        for (String pkg : packages) {
            if (expected.equals(pkg)) return true;
        }
        return false;
    }

    @Override
    public Bundle call(String authority, String method, String arg, Bundle extras) {
        return call(method, arg, extras);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.com.midori.hypericons.config";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}

