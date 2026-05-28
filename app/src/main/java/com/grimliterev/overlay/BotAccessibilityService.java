package com.grimliterev.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

import com.grimliterev.overlay.game.AccessibilityGameInteractor;
import com.grimliterev.overlay.game.CoordinateMap;

public class BotAccessibilityService extends AccessibilityService {
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
        SharedPreferences prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);
        CoordinateMap coords = new CoordinateMap(prefs);
        interactor = new AccessibilityGameInteractor(this, coords);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor window changes if needed
    }

    @Override
    public void onInterrupt() {
        instance = null;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
