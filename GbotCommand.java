package com.grimliterev.overlay;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.grimliterev.overlay.game.CoordinateMap;

import java.util.List;

public class DiagnosticsHelper {

    public static boolean isOverlayAllowed(Context ctx) {
        return Settings.canDrawOverlays(ctx);
    }

    public static boolean isAccessibilityEnabled(Context ctx) {
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        if (services == null) return false;
        String myId = ctx.getPackageName() + "/.BotAccessibilityService";
        for (AccessibilityServiceInfo info : services) {
            if (info.getId().contains(myId) || info.getId().contains("BotAccessibilityService")) {
                return true;
            }
        }
        return false;
    }

    public static String getDiagnostics(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("grimlite_overlay", Context.MODE_PRIVATE);
        CoordinateMap map = new CoordinateMap(prefs);
        StringBuilder sb = new StringBuilder();
        sb.append("Overlay Permission: ").append(isOverlayAllowed(ctx) ? "GRANTED" : "DENIED").append("\n");
        sb.append("Accessibility Service: ").append(isAccessibilityEnabled(ctx) ? "ENABLED" : "DISABLED").append("\n");
        sb.append("Script Loaded: ").append(prefs.getString("scriptName", "None")).append("\n");
        sb.append("Coordinates Set:\n");
        String[] keys = {"attack","skill1","skill2","skill3","skill4","rest","menu","join","confirm","quest","accept","turnin"};
        for (String k : keys) {
            int[] p = map.get(k);
            sb.append("  ").append(k).append(": ")
              .append(p[0] >= 0 ? "(" + p[0] + "," + p[1] + ")" : "NOT SET").append("\n");
        }
        return sb.toString();
    }
}
