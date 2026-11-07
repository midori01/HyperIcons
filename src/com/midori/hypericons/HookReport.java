package com.midori.hypericons;

import java.util.ArrayList;
import java.util.List;

/** Parsed hook status that a hooked process sent through {@link HookEnv#report}. */
public final class HookReport {

    public static final class Entry {
        public final String name;
        /** Null when the feature or hook point works. */
        public final String failure;

        Entry(String name, String failure) {
            this.name = name;
            this.failure = failure;
        }
    }

    public String framework = "";
    public String frameworkVersion = "";
    public String apiVersion = "";
    public String moduleVersion = "";
    public long time;
    public final List<Entry> groups = new ArrayList<Entry>();
    public final List<Entry> points = new ArrayList<Entry>();

    public static HookReport parse(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] lines = text.split("\n");
        String[] head = lines[0].split("\t", -1);
        if (head.length < 5) return null;
        HookReport r = new HookReport();
        r.framework = head[0];
        r.frameworkVersion = head[1];
        r.apiVersion = head[2];
        r.moduleVersion = head[3];
        try {
            r.time = Long.parseLong(head[4]);
        } catch (NumberFormatException ignored) {
        }
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split("\t", -1);
            if (cols.length < 3) continue;
            Entry e = new Entry(cols[1], cols[2].isEmpty() ? null : cols[2]);
            if ("G".equals(cols[0])) {
                r.groups.add(e);
            } else if ("P".equals(cols[0])) {
                r.points.add(e);
            }
        }
        return r;
    }

    public static int countOk(List<Entry> entries) {
        int ok = 0;
        for (Entry e : entries) {
            if (e.failure == null) ok++;
        }
        return ok;
    }
}
