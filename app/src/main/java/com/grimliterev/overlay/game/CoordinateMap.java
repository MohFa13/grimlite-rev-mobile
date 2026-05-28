package com.grimliterev.overlay.game;

import android.content.SharedPreferences;

import org.json.*;

import java.util.*;

public class CoordinateMap {
    private final SharedPreferences prefs;
    private static final String KEY = "coordinate_map";

    public CoordinateMap(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void set(String action, int x, int y) {
        try {
            JSONObject obj = loadJson();
            JSONObject point = new JSONObject();
            point.put("x", x);
            point.put("y", y);
            obj.put(action.toLowerCase(), point);
            prefs.edit().putString(KEY, obj.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int[] get(String action) {
        try {
            JSONObject obj = loadJson();
            JSONObject point = obj.optJSONObject(action.toLowerCase());
            if (point != null) {
                return new int[]{ point.optInt("x", -1), point.optInt("y", -1) };
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new int[]{ -1, -1 };
    }

    public boolean has(String action) {
        int[] p = get(action);
        return p[0] >= 0 && p[1] >= 0;
    }

    public Map<String, int[]> getAll() {
        Map<String, int[]> map = new HashMap<>();
        try {
            JSONObject obj = loadJson();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject p = obj.optJSONObject(k);
                if (p != null) map.put(k, new int[]{ p.optInt("x"), p.optInt("y") });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    private JSONObject loadJson() {
        try { return new JSONObject(prefs.getString(KEY, "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }
}
