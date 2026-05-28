package com.grimliterev.overlay;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

public class OverlayService extends Service {
    private WindowManager wm;
    private LinearLayout root;
    private LinearLayout panel;
    private TextView status;
    private TextView scriptHeader;
    private TextView scriptPreview;
    private TextView currentCommand;
    private SharedPreferences prefs;
    private Handler handler;
    private final ArrayList<String> commands = new ArrayList<>();
    private boolean previewRunning = false;
    private int commandIndex = 0;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return;
        startForegroundCompat();
        reloadScript();
        createOverlay();
    }

    private void startForegroundCompat() {
        String id = "grimlite_overlay";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(id, "Grimlite Overlay", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, id) : new Notification.Builder(this);
        b.setContentTitle("Grimlite Overlay running")
            .setContentText("Floating .gbot script panel is active")
            .setSmallIcon(android.R.drawable.ic_menu_manage);
        startForeground(1337, b.build());
    }

    private void createOverlay() {
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = prefs.getInt("x", 24);
        lp.y = prefs.getInt("y", 80);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        Button menu = new Button(this);
        menu.setText("☰ Grimlite");
        menu.setAllCaps(false);
        menu.setTextColor(Color.WHITE);
        menu.setBackgroundColor(Color.rgb(242, 140, 40));
        root.addView(menu, new LinearLayout.LayoutParams(220, -2));

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(16, 16, 16, 16);
        panel.setBackgroundColor(Color.argb(235, 18, 18, 18));
        panel.setVisibility(View.GONE);
        root.addView(panel, new LinearLayout.LayoutParams(920, -2));

        TextView title = text("Grimlite Rev - .gbot Loader", 18, true);
        panel.addView(title);

        status = text("Status: " + prefs.getString("status", "Stopped"), 14, false);
        panel.addView(status);

        LinearLayout landscapeRow = new LinearLayout(this);
        landscapeRow.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(landscapeRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        landscapeRow.addView(controls, new LinearLayout.LayoutParams(260, -2));

        addButton(controls, "Load .gbot", v -> openLoader());
        addButton(controls, "Reload Script", v -> { reloadScript(); refreshScriptViews(); saveStatus("Reloaded script"); });
        addButton(controls, "Start Preview", v -> startPreview());
        addButton(controls, "Stop", v -> stopPreview("Stopped"));
        addButton(controls, "Next Command", v -> nextCommand());
        addButton(controls, "Save Config", v -> saveConfig());
        addButton(controls, "Hide", v -> panel.setVisibility(View.GONE));

        LinearLayout scriptPanel = new LinearLayout(this);
        scriptPanel.setOrientation(LinearLayout.VERTICAL);
        scriptPanel.setPadding(16, 0, 0, 0);
        landscapeRow.addView(scriptPanel, new LinearLayout.LayoutParams(620, -2));

        scriptHeader = text("No script loaded", 14, true);
        scriptPanel.addView(scriptHeader);

        currentCommand = text("Current: -", 13, false);
        currentCommand.setTextColor(Color.rgb(255, 220, 160));
        scriptPanel.addView(currentCommand);

        ScrollView previewScroll = new ScrollView(this);
        scriptPreview = text("Use Load .gbot to select a Grimlite .gbot file.", 12, false);
        scriptPreview.setTextIsSelectable(false);
        previewScroll.addView(scriptPreview);
        scriptPanel.addView(previewScroll, new LinearLayout.LayoutParams(600, 300));

        refreshScriptViews();
        menu.setOnClickListener(v -> panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        makeDraggable(menu, lp);
        wm.addView(root, lp);
    }

    private void makeDraggable(View drag, WindowManager.LayoutParams lp) {
        final int[] startX = new int[1], startY = new int[1];
        final float[] touchX = new float[1], touchY = new float[1];
        drag.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = lp.x; startY[0] = lp.y;
                    touchX[0] = e.getRawX(); touchY[0] = e.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    lp.x = startX[0] + (int)(e.getRawX() - touchX[0]);
                    lp.y = startY[0] + (int)(e.getRawY() - touchY[0]);
                    wm.updateViewLayout(root, lp);
                    prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply();
                    return true;
            }
            return false;
        });
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        t.setPadding(4, 5, 4, 5);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 4, 0, 4);
        parent.addView(b, lp);
    }

    private void openLoader() {
        Intent i = new Intent(this, MainActivity.class);
        i.setAction("load_gbot");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        saveStatus("Opening .gbot picker");
    }

    private void reloadScript() {
        commands.clear();
        String script = prefs.getString("script", "");
        String[] lines = script.split("\\r?\\n");
        for (String line : lines) {
            String t = cleanLine(line);
            if (t.length() > 0) commands.add(t);
        }
        if (commandIndex >= commands.size()) commandIndex = 0;
    }

    private String cleanLine(String line) {
        String t = line.trim();
        if (t.length() == 0) return "";
        if (t.startsWith("//") || t.startsWith("#") || t.startsWith(";") || t.startsWith("--")) return "";
        return t;
    }

    private void refreshScriptViews() {
        if (scriptHeader == null) return;
        String name = prefs.getString("scriptName", "No .gbot loaded");
        int lines = prefs.getInt("scriptLineCount", 0);
        int count = commands.size();
        scriptHeader.setText("Script: " + name + "  | Lines: " + lines + " | Commands: " + count);
        scriptPreview.setText(buildPreview());
        currentCommand.setText(commandIndex < commands.size() ? "Current: " + commands.get(commandIndex) : "Current: -");
    }

    private String buildPreview() {
        if (commands.size() == 0) return "No commands loaded. Tap Load .gbot.";
        StringBuilder sb = new StringBuilder();
        int max = Math.min(commands.size(), 80);
        for (int i = 0; i < max; i++) {
            sb.append(String.format(Locale.US, "%03d  %s", i + 1, commands.get(i)));
            if (i == commandIndex) sb.append("   < current");
            sb.append('\n');
        }
        if (commands.size() > max) sb.append("... ").append(commands.size() - max).append(" more commands");
        return sb.toString();
    }

    private void startPreview() {
        if (commands.size() == 0) { saveStatus("No .gbot loaded"); return; }
        previewRunning = true;
        saveStatus("Preview running");
        handler.post(previewTick);
    }

    private void stopPreview(String reason) {
        previewRunning = false;
        handler.removeCallbacks(previewTick);
        saveStatus(reason);
    }

    private final Runnable previewTick = new Runnable() {
        @Override public void run() {
            if (!previewRunning) return;
            nextCommand();
            handler.postDelayed(this, 900);
        }
    };

    private void nextCommand() {
        if (commands.size() == 0) { saveStatus("No .gbot loaded"); return; }
        currentCommand.setText("Current: " + commands.get(commandIndex));
        scriptPreview.setText(buildPreview());
        commandIndex++;
        if (commandIndex >= commands.size()) {
            commandIndex = 0;
            stopPreview("Preview complete");
        }
    }

    private void saveStatus(String s) {
        if (status != null) status.setText("Status: " + s);
        prefs.edit().putString("status", s).apply();
    }

    private void saveConfig() {
        prefs.edit()
            .putString("status", status.getText().toString().replace("Status: ", ""))
            .putInt("commandIndex", commandIndex)
            .apply();
        saveStatus("Saved");
    }

    @Override public void onDestroy() {
        previewRunning = false;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (wm != null && root != null) wm.removeView(root);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
