package com.grimliterev.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.grimliterev.overlay.game.AccessibilityGameInteractor;
import com.grimliterev.overlay.game.CoordinateMap;
import com.grimliterev.overlay.gbot.GbotParser;
import com.grimliterev.overlay.gbot.GbotScript;
import com.grimliterev.overlay.gbot.ScriptExecutor;

public class BotAccessibilityService extends AccessibilityService implements ScriptExecutor.Callback {
    private static final String TAG = "BotAccessibilityService";
    private static BotAccessibilityService instance;
    private AccessibilityGameInteractor interactor;
    private ScriptExecutor executor;
    private Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private boolean aqwForeground = false;
    private boolean popupShowing = false;
    private long lastPopupTime = 0;

    public static BotAccessibilityService getInstance() { return instance; }
    public AccessibilityGameInteractor getInteractor() { return interactor; }
    public ScriptExecutor getExecutor() { return executor; }
    public boolean isAqwForeground() { return aqwForeground; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Accessibility service connected");
        prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);
        CoordinateMap coords = new CoordinateMap(prefs);
        interactor = new AccessibilityGameInteractor(this, coords);
        executor = new ScriptExecutor(interactor);
        executor.setCallback(this);
        loadScript();

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        info.packageNames = new String[]{"air.AQWPocket", "air.com.aqwpocket", "com.aqwpocket", "com.anthonyhyo.aqwpocket"};
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            if (pkg == null) return;
            String pkgName = pkg.toString();
            boolean isAqw = pkgName.contains("AQWPocket") || pkgName.contains("aqwpocket");
            if (isAqw && !aqwForeground) {
                aqwForeground = true;
                Log.i(TAG, "AQW Pocket opened");
                showPopup();
            } else if (!isAqw && aqwForeground) {
                aqwForeground = false;
                Log.i(TAG, "AQW Pocket closed");
            }
        }
    }

    private void showPopup() {
        long now = System.currentTimeMillis();
        if (now - lastPopupTime < 2000) return; // debounce
        lastPopupTime = now;
        try {
            Intent intent = new Intent(this, PopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
            popupShowing = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch popup", e);
        }
    }

    public void loadScript() {
        String text = prefs.getString("script", "");
        String name = prefs.getString("scriptName", "unnamed");
        if (!text.isEmpty()) {
            GbotScript script = GbotParser.parse(name, text);
            if (executor != null) executor.load(script);
        }
    }

    public void startScript() {
        if (executor != null) executor.start();
    }

    public void stopScript() {
        if (executor != null) executor.stop("Stopped");
    }

    public void nextCommand() {
        if (executor != null) executor.next();
    }

    @Override public void onStatus(String s) {
        Log.d(TAG, "Status: " + s);
    }
    @Override public void onCommand(int idx, String raw) {
        Log.d(TAG, "Cmd [" + idx + "]: " + raw);
    }
    @Override public void onFinished(String reason) {
        Log.d(TAG, "Finished: " + reason);
    }

    @Override public void onInterrupt() {
        Log.w(TAG, "Interrupted");
    }

    @Override public void onDestroy() {
        Log.w(TAG, "Destroyed");
        if (executor != null) executor.stop("Service destroyed");
        instance = null;
        super.onDestroy();
    }
}
