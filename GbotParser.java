package com.grimliterev.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.grimliterev.overlay.game.AccessibilityGameInteractor;
import com.grimliterev.overlay.game.CoordinateMap;

public class BotAccessibilityService extends AccessibilityService {
    private static final String TAG = "BotAccessibilityService";
    private static BotAccessibilityService instance;
    private AccessibilityGameInteractor interactor;

    public static BotAccessibilityService getInstance() {
        return instance;
    }

    public AccessibilityGameInteractor getInteractor() {
        return interactor;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "Accessibility service connected");
        SharedPreferences prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);
        CoordinateMap coords = new CoordinateMap(prefs);
        interactor = new AccessibilityGameInteractor(this, coords);

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        info.packageNames = new String[]{"air.AQWPocket", "air.com.aqwpocket", "com.aqwpocket", "com.anthonyhyo.aqwpocket"};
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor window focus changes if needed for future smart detection
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Accessibility service destroyed");
        instance = null;
        interactor = null;
        super.onDestroy();
    }
}
