package com.midori.hypericons;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

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
            prefs.edit().putLong("last_systemui_ping", System.currentTimeMillis()).apply();
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.KEY_SIM_MODE, prefs.getString(MainActivity.KEY_SIM_MODE, MainActivity.VAL_SIM_MODE_BOTH));
            bundle.putString(MainActivity.KEY_TARGET_TAB, prefs.getString(MainActivity.KEY_TARGET_TAB, MainActivity.TAB_BOTH));

            // Global / Both SIMs config
            bundle.putString(MainActivity.KEY_5G_CA, prefs.getString(MainActivity.KEY_5G_CA, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_5G_BASIC, prefs.getString(MainActivity.KEY_5G_BASIC, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_CAPSULE_STYLE, prefs.getString(MainActivity.KEY_CAPSULE_STYLE, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_4G_RAT, prefs.getString(MainActivity.KEY_4G_RAT, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_ALWAYS_SHOW_RAT, prefs.getString(MainActivity.KEY_ALWAYS_SHOW_RAT, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_VOLTE_STYLE, prefs.getString(MainActivity.KEY_VOLTE_STYLE, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_VOWIFI_STYLE, prefs.getString(MainActivity.KEY_VOWIFI_STYLE, MainActivity.VAL_DEFAULT));
            bundle.putString(MainActivity.KEY_ROAMING_STYLE, prefs.getString(MainActivity.KEY_ROAMING_STYLE, MainActivity.VAL_DEFAULT));

            // SIM 1 (Slot 0) config
            bundle.putString(MainActivity.KEY_SIM1_5G_CA, prefs.getString(MainActivity.KEY_SIM1_5G_CA, prefs.getString(MainActivity.KEY_5G_CA, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_5G_BASIC, prefs.getString(MainActivity.KEY_SIM1_5G_BASIC, prefs.getString(MainActivity.KEY_5G_BASIC, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_CAPSULE_STYLE, prefs.getString(MainActivity.KEY_SIM1_CAPSULE_STYLE, prefs.getString(MainActivity.KEY_CAPSULE_STYLE, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_4G_RAT, prefs.getString(MainActivity.KEY_SIM1_4G_RAT, prefs.getString(MainActivity.KEY_4G_RAT, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_ALWAYS_SHOW_RAT, prefs.getString(MainActivity.KEY_SIM1_ALWAYS_SHOW_RAT, prefs.getString(MainActivity.KEY_ALWAYS_SHOW_RAT, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_VOLTE_STYLE, prefs.getString(MainActivity.KEY_SIM1_VOLTE_STYLE, prefs.getString(MainActivity.KEY_VOLTE_STYLE, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_VOWIFI_STYLE, prefs.getString(MainActivity.KEY_SIM1_VOWIFI_STYLE, prefs.getString(MainActivity.KEY_VOWIFI_STYLE, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM1_ROAMING_STYLE, prefs.getString(MainActivity.KEY_SIM1_ROAMING_STYLE, prefs.getString(MainActivity.KEY_ROAMING_STYLE, MainActivity.VAL_DEFAULT)));

            // SIM 2 / eSIM (Slot 1) config
            bundle.putString(MainActivity.KEY_SIM2_5G_CA, prefs.getString(MainActivity.KEY_SIM2_5G_CA, prefs.getString(MainActivity.KEY_5G_CA, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_5G_BASIC, prefs.getString(MainActivity.KEY_SIM2_5G_BASIC, prefs.getString(MainActivity.KEY_5G_BASIC, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_CAPSULE_STYLE, prefs.getString(MainActivity.KEY_SIM2_CAPSULE_STYLE, prefs.getString(MainActivity.KEY_CAPSULE_STYLE, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_4G_RAT, prefs.getString(MainActivity.KEY_SIM2_4G_RAT, prefs.getString(MainActivity.KEY_4G_RAT, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_ALWAYS_SHOW_RAT, prefs.getString(MainActivity.KEY_SIM2_ALWAYS_SHOW_RAT, prefs.getString(MainActivity.KEY_ALWAYS_SHOW_RAT, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_VOLTE_STYLE, prefs.getString(MainActivity.KEY_SIM2_VOLTE_STYLE, prefs.getString(MainActivity.KEY_VOLTE_STYLE, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_VOWIFI_STYLE, prefs.getString(MainActivity.KEY_SIM2_VOWIFI_STYLE, prefs.getString(MainActivity.KEY_VOWIFI_STYLE, MainActivity.VAL_DEFAULT)));
            bundle.putString(MainActivity.KEY_SIM2_ROAMING_STYLE, prefs.getString(MainActivity.KEY_SIM2_ROAMING_STYLE, prefs.getString(MainActivity.KEY_ROAMING_STYLE, MainActivity.VAL_DEFAULT)));

            // Experimental: 5G+ Feature Center keep-alive
            bundle.putBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, prefs.getBoolean(MainActivity.KEY_ENABLE_5GA_KEEPALIVE, false));

            return bundle;
        } else if (METHOD_SET_CONFIG.equals(method) && extras != null) {
            SharedPreferences.Editor editor = prefs.edit();
            String[] keys = new String[]{
                MainActivity.KEY_SIM_MODE, MainActivity.KEY_TARGET_TAB,
                MainActivity.KEY_5G_CA, MainActivity.KEY_5G_BASIC, MainActivity.KEY_CAPSULE_STYLE,
                MainActivity.KEY_4G_RAT, MainActivity.KEY_ALWAYS_SHOW_RAT, MainActivity.KEY_VOLTE_STYLE,
                MainActivity.KEY_VOWIFI_STYLE, MainActivity.KEY_ROAMING_STYLE,
                MainActivity.KEY_SIM1_5G_CA, MainActivity.KEY_SIM1_5G_BASIC, MainActivity.KEY_SIM1_CAPSULE_STYLE,
                MainActivity.KEY_SIM1_4G_RAT, MainActivity.KEY_SIM1_ALWAYS_SHOW_RAT, MainActivity.KEY_SIM1_VOLTE_STYLE,
                MainActivity.KEY_SIM1_VOWIFI_STYLE, MainActivity.KEY_SIM1_ROAMING_STYLE,
                MainActivity.KEY_SIM2_5G_CA, MainActivity.KEY_SIM2_5G_BASIC, MainActivity.KEY_SIM2_CAPSULE_STYLE,
                MainActivity.KEY_SIM2_4G_RAT, MainActivity.KEY_SIM2_ALWAYS_SHOW_RAT, MainActivity.KEY_SIM2_VOLTE_STYLE,
                MainActivity.KEY_SIM2_VOWIFI_STYLE, MainActivity.KEY_SIM2_ROAMING_STYLE
            };
            for (String key : keys) {
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

