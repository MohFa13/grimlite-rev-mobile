package com.grimliterev.overlay.game;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.*;

public class AccessibilityGameInteractor implements GameInteractor {
    private static final String TAG = "AGI";
    private static final String[] AQW_PACKAGES = {
        "air.AQWPocket", "air.com.aqwpocket", "com.aqwpocket", "com.anthonyhyo.aqwpocket"
    };

    private final AccessibilityService service;
    private final CoordinateMap coords;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Boolean> stateMap = new HashMap<>();

    public AccessibilityGameInteractor(AccessibilityService service, CoordinateMap coords) {
        this.service = service;
        this.coords = coords;
    }

    @Override
    public void tap(int x, int y) {
        performGesture(x, y, x, y, 50);
    }

    @Override
    public void swipe(int x1, int y1, int x2, int y2, int durationMs) {
        performGesture(x1, y1, x2, y2, durationMs);
    }

    @Override
    public void longPress(int x, int y, int durationMs) {
        performGesture(x, y, x, y, durationMs);
    }

    private void performGesture(int x1, int y1, int x2, int y2, int duration) {
        Path path = new Path();
        path.moveTo(x1, y1);
        if (x1 != x2 || y1 != y2) path.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, Math.max(duration, 50));
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);
        service.dispatchGesture(builder.build(), null, null);
    }

    @Override
    public void kill(String monster) {
        // Sequence: tap attack -> tap skill1 -> tap skill2 -> tap skill3 -> tap skill4
        int[] attack = coords.get("attack");
        if (attack[0] >= 0) tap(attack[0], attack[1]);
        handler.postDelayed(() -> {
            for (int i = 1; i <= 4; i++) {
                int[] s = coords.get("skill" + i);
                if (s[0] >= 0) {
                    final int fx = s[0], fy = s[1];
                    handler.postDelayed(() -> tap(fx, fy), i * 400L);
                }
            }
        }, 300);
    }

    @Override
    public void join(String map) {
        // Sequence: open menu -> join map -> type/confirm
        int[] menu = coords.get("menu");
        int[] join = coords.get("join");
        int[] confirm = coords.get("confirm");
        if (menu[0] >= 0) tap(menu[0], menu[1]);
        handler.postDelayed(() -> {
            if (join[0] >= 0) tap(join[0], join[1]);
            handler.postDelayed(() -> {
                if (confirm[0] >= 0) tap(confirm[0], confirm[1]);
            }, 1000);
        }, 800);
    }

    @Override
    public void acceptQuest(String quest) {
        int[] questBtn = coords.get("quest");
        int[] acceptBtn = coords.get("accept");
        if (questBtn[0] >= 0) tap(questBtn[0], questBtn[1]);
        handler.postDelayed(() -> {
            if (acceptBtn[0] >= 0) tap(acceptBtn[0], acceptBtn[1]);
        }, 800);
    }

    @Override
    public void turnInQuest(String quest) {
        int[] questBtn = coords.get("quest");
        int[] turninBtn = coords.get("turnin");
        if (questBtn[0] >= 0) tap(questBtn[0], questBtn[1]);
        handler.postDelayed(() -> {
            if (turninBtn[0] >= 0) tap(turninBtn[0], turninBtn[1]);
        }, 800);
    }

    @Override
    public void rest() {
        int[] rest = coords.get("rest");
        if (rest[0] >= 0) tap(rest[0], rest[1]);
    }

    @Override
    public void useSkill(int slot) {
        int[] s = coords.get("skill" + slot);
        if (s[0] >= 0) tap(s[0], s[1]);
    }

    @Override
    public void move(int x, int y) {
        tap(x, y);
    }

    @Override
    public void buy(String item) {
        int[] shop = coords.get("shop");
        int[] buy = coords.get("buy");
        if (shop[0] >= 0) tap(shop[0], shop[1]);
        handler.postDelayed(() -> {
            if (buy[0] >= 0) tap(buy[0], buy[1]);
        }, 800);
    }

    @Override
    public void sell(String item) {
        int[] shop = coords.get("shop");
        int[] sell = coords.get("sell");
        if (shop[0] >= 0) tap(shop[0], shop[1]);
        handler.postDelayed(() -> {
            if (sell[0] >= 0) tap(sell[0], sell[1]);
        }, 800);
    }

    @Override
    public void equip(String item) {
        int[] inv = coords.get("inventory");
        int[] equip = coords.get("equip");
        if (inv[0] >= 0) tap(inv[0], inv[1]);
        handler.postDelayed(() -> {
            if (equip[0] >= 0) tap(equip[0], equip[1]);
        }, 600);
    }

    @Override
    public void unequip(String item) {
        int[] inv = coords.get("inventory");
        int[] unequip = coords.get("unequip");
        if (inv[0] >= 0) tap(inv[0], inv[1]);
        handler.postDelayed(() -> {
            if (unequip[0] >= 0) tap(unequip[0], unequip[1]);
        }, 600);
    }

    @Override
    public boolean getState(String key) {
        return stateMap.getOrDefault(key.toLowerCase(), false);
    }

    public void setState(String key, boolean value) {
        stateMap.put(key.toLowerCase(), value);
    }
}
