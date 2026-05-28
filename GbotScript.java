package com.grimliterev.overlay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.grimliterev.overlay.game.CoordinateMap;
import com.grimliterev.overlay.gbot.GbotParser;
import com.grimliterev.overlay.gbot.GbotScript;
import com.grimliterev.overlay.gbot.ScriptExecutor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PopupActivity extends Activity implements ScriptExecutor.Callback {
    private static final String TAG = "PopupActivity";
    private static final int PICK_GBOT_FILE = 2001;

    private SharedPreferences prefs;
    private TextView scriptName;
    private TextView status;
    private TextView currentCmd;
    private TextView preview;
    private Button startBtn;
    private Button stopBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Log.i(TAG, "onCreate");
        prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);

        // Position popup in top-right corner
        Window window = getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP | Gravity.END;
        wlp.x = 20;
        wlp.y = 120;
        wlp.width = 520;
        wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wlp.dimAmount = 0.0f;
        wlp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        window.setAttributes(wlp);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(buildUI());
        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BotAccessibilityService svc = BotAccessibilityService.getInstance();
        if (svc != null && svc.getExecutor() != null) {
            svc.getExecutor().setCallback(this);
        }
        refreshUI();
    }

    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.argb(230, 22, 22, 22));

        TextView title = new TextView(this);
        title.setText("Grimlite Rev");
        title.setTextSize(18);
        title.setTextColor(Color.rgb(242, 140, 40));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        scriptName = new TextView(this);
        scriptName.setTextSize(13);
        scriptName.setTextColor(Color.WHITE);
        scriptName.setPadding(0, 8, 0, 8);
        root.addView(scriptName);

        status = new TextView(this);
        status.setTextSize(12);
        status.setTextColor(Color.rgb(200, 200, 200));
        status.setPadding(0, 4, 0, 4);
        root.addView(status);

        currentCmd = new TextView(this);
        currentCmd.setTextSize(12);
        currentCmd.setTextColor(Color.rgb(255, 220, 160));
        currentCmd.setPadding(0, 4, 0, 8);
        root.addView(currentCmd);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(btnRow, new LinearLayout.LayoutParams(-1, -2));

        startBtn = new Button(this);
        startBtn.setText("▶ Start");
        startBtn.setAllCaps(false);
        startBtn.setBackgroundColor(Color.rgb(34, 139, 34));
        startBtn.setTextColor(Color.WHITE);
        startBtn.setOnClickListener(v -> startScript());
        btnRow.addView(startBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        stopBtn = new Button(this);
        stopBtn.setText("⏹ Stop");
        stopBtn.setAllCaps(false);
        stopBtn.setBackgroundColor(Color.rgb(178, 34, 34));
        stopBtn.setTextColor(Color.WHITE);
        stopBtn.setOnClickListener(v -> stopScript());
        btnRow.addView(stopBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout btnRow2 = new LinearLayout(this);
        btnRow2.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(btnRow2, new LinearLayout.LayoutParams(-1, -2));

        Button loadBtn = new Button(this);
        loadBtn.setText("Load .gbot");
        loadBtn.setAllCaps(false);
        loadBtn.setOnClickListener(v -> openGbotPicker());
        btnRow2.addView(loadBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        Button nextBtn = new Button(this);
        nextBtn.setText("Next");
        nextBtn.setAllCaps(false);
        nextBtn.setOnClickListener(v -> nextScript());
        btnRow2.addView(nextBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout btnRow3 = new LinearLayout(this);
        btnRow3.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(btnRow3, new LinearLayout.LayoutParams(-1, -2));

        Button testBtn = new Button(this);
        testBtn.setText("Test Tap");
        testBtn.setAllCaps(false);
        testBtn.setOnClickListener(v -> testTap());
        btnRow3.addView(testBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        Button coordsBtn = new Button(this);
        coordsBtn.setText("Coords");
        coordsBtn.setAllCaps(false);
        coordsBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.setAction("coord_setup");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        btnRow3.addView(coordsBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        Button closeBtn = new Button(this);
        closeBtn.setText("✕ Close");
        closeBtn.setAllCaps(false);
        closeBtn.setOnClickListener(v -> finish());
        root.addView(closeBtn, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroller = new ScrollView(this);
        preview = new TextView(this);
        preview.setTextSize(11);
        preview.setTextColor(Color.rgb(180, 180, 180));
        preview.setPadding(0, 8, 0, 0);
        scroller.addView(preview);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 240));

        return root;
    }

    private void startScript() {
        BotAccessibilityService svc = BotAccessibilityService.getInstance();
        if (svc == null) {
            status.setText("ERROR: Accessibility not enabled");
            Toast.makeText(this, "Enable Grimlite Bot Accessibility Service", Toast.LENGTH_LONG).show();
            return;
        }
        svc.loadScript();
        svc.startScript();
        status.setText("Running...");
    }

    private void stopScript() {
        BotAccessibilityService svc = BotAccessibilityService.getInstance();
        if (svc != null) svc.stopScript();
        status.setText("Stopped");
        currentCmd.setText("Current: -");
    }

    private void nextScript() {
        BotAccessibilityService svc = BotAccessibilityService.getInstance();
        if (svc != null) svc.nextCommand();
    }

    private void testTap() {
        CoordinateMap map = new CoordinateMap(prefs);
        int[] p = map.get("attack");
        if (p[0] < 0) {
            Toast.makeText(this, "Set 'attack' coordinates first", Toast.LENGTH_SHORT).show();
            return;
        }
        BotAccessibilityService svc = BotAccessibilityService.getInstance();
        if (svc != null && svc.getInteractor() != null) {
            svc.getInteractor().tap(p[0], p[1]);
            status.setText("Test tap at (" + p[0] + "," + p[1] + ")");
        } else {
            status.setText("Accessibility not connected");
        }
    }

    private void openGbotPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, PICK_GBOT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_GBOT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) loadGbotFromUri(uri);
        }
    }

    private void loadGbotFromUri(Uri uri) {
        try {
            String name = getDisplayName(uri);
            String text = readText(uri);
            GbotScript script = GbotParser.parse(name, text);

            prefs.edit()
                .putString("script", text)
                .putString("scriptName", name)
                .putInt("scriptLineCount", text.split("\r?\n").length)
                .putInt("scriptCommandCount", script.commands.size())
                .apply();

            BotAccessibilityService svc = BotAccessibilityService.getInstance();
            if (svc != null) svc.loadScript();

            refreshUI();
            Toast.makeText(this, "Loaded " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to load: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getDisplayName(Uri uri) {
        String name = "script.gbot";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return name;
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void refreshUI() {
        String name = prefs.getString("scriptName", "No script loaded");
        int lines = prefs.getInt("scriptLineCount", 0);
        int cmds = prefs.getInt("scriptCommandCount", 0);
        scriptName.setText(name + " (" + cmds + " cmds, " + lines + " lines)");

        String text = prefs.getString("script", "");
        if (!text.isEmpty()) {
            GbotScript script = GbotParser.parse(name, text);
            StringBuilder sb = new StringBuilder();
            int max = Math.min(script.commands.size(), 30);
            for (int i = 0; i < max; i++) {
                sb.append(String.format(java.util.Locale.US, "%03d %s\n", i + 1, script.commands.get(i).raw));
            }
            if (script.commands.size() > max) sb.append("... " + (script.commands.size() - max) + " more");
            preview.setText(sb.toString());
        } else {
            preview.setText("No script loaded. Tap Load .gbot");
        }

        status.setText("Ready");
        currentCmd.setText("Current: -");
    }

    @Override public void onStatus(String s) {
        runOnUiThread(() -> status.setText(s));
    }
    @Override public void onCommand(int idx, String raw) {
        runOnUiThread(() -> currentCmd.setText("[" + idx + "] " + raw));
    }
    @Override public void onFinished(String reason) {
        runOnUiThread(() -> {
            status.setText("Done: " + reason);
            currentCmd.setText("Current: -");
        });
    }
}
