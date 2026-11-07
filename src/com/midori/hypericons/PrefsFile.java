package com.midori.hypericons;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Read-only view of the app's SharedPreferences XML file, used by the hooks
 * as a fallback when the config provider cannot be reached (for example
 * early in boot). Replaces the legacy XSharedPreferences.
 */
public final class PrefsFile {
    private final File mFile;
    private volatile Map<String, Object> mValues = Collections.emptyMap();

    public PrefsFile(File file) {
        mFile = file;
    }

    /** The credential-encrypted prefs file of {@code packageName}. */
    public PrefsFile(String packageName, String name) {
        this(new File("/data/data/" + packageName + "/shared_prefs/" + name + ".xml"));
    }

    public void reload() {
        Map<String, Object> values = new HashMap<String, Object>();
        InputStream in = null;
        try {
            in = new FileInputStream(mFile);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, "UTF-8");
            for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                if (event != XmlPullParser.START_TAG) continue;
                String tag = parser.getName();
                String key = parser.getAttributeValue(null, "name");
                if (key == null) continue;
                String attr = parser.getAttributeValue(null, "value");
                if ("string".equals(tag)) {
                    values.put(key, parser.nextText());
                } else if ("boolean".equals(tag) && attr != null) {
                    values.put(key, Boolean.valueOf(attr));
                } else if ("int".equals(tag) && attr != null) {
                    values.put(key, Integer.valueOf(attr));
                } else if ("long".equals(tag) && attr != null) {
                    values.put(key, Long.valueOf(attr));
                }
            }
        } catch (Throwable t) {
            HookEnv.log("PrefsFile: cannot read " + mFile + ": " + t);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
        mValues = Collections.unmodifiableMap(values);
    }

    public Map<String, ?> getAll() {
        return mValues;
    }

    public String getString(String key, String def) {
        Object v = mValues.get(key);
        return v instanceof String ? (String) v : def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = mValues.get(key);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : def;
    }
}
