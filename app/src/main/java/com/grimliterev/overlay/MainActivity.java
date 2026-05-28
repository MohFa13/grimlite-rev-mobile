package com.grimliterev.overlay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_GBOT_FILE = 2001;
    private SharedPreferences prefs;
    private TextView scriptInfo;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);
        setContentView(buildLayout());
        refreshScriptInfo();
        if ("load_gbot".equals(getIntent().getAction())) openGbotPicker();
    }

    private ScrollView buildLayout() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 28, 40, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Grimlite Rev Mobile Overlay");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(30, 30, 30));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView desc = new TextView(this);
        desc.setText("Landscape-compatible Grimlite-style overlay for AQW Pocket. Load .gbot files locally, preview commands, save config, then use the floating panel over AQW Pocket.");
        desc.setTextSize(15);
        desc.setPadding(0, 18, 0, 18);
        root.addView(desc, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        root.addView(buttons, new LinearLayout.LayoutParams(-1, -2));

        buttons.addView(button("Grant Overlay Permission", v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }));
        buttons.addView(button("Load .gbot Script", v -> openGbotPicker()));
        buttons.addView(button("Start Overlay", v -> startService(new Intent(this, OverlayService.class))));
        buttons.addView(button("Stop Overlay", v -> stopService(new Intent(this, OverlayService.class))));
        buttons.addView(button("Open AQW Pocket", v -> openAqwPocket()));

        scriptInfo = new TextView(this);
        scriptInfo.setTextSize(14);
        scriptInfo.setTextColor(Color.rgb(45, 45, 45));
        scriptInfo.setBackgroundColor(Color.rgb(245, 245, 245));
        scriptInfo.setPadding(20, 20, 20, 20);
        root.addView(scriptInfo, new LinearLayout.LayoutParams(-1, -2));
        return scroll;
    }

    private Button button(String s, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(6, 8, 6, 8);
        b.setLayoutParams(lp);
        return b;
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
            ScriptSummary summary = parseGbot(text);
            prefs.edit()
                .putString("script", text)
                .putString("scriptName", name)
                .putInt("scriptLineCount", summary.totalLines)
                .putInt("scriptCommandCount", summary.commands)
                .putString("scriptSummary", summary.summary)
                .putString("status", "Loaded " + name)
                .apply();
            startService(new Intent(this, OverlayService.class));
            refreshScriptInfo();
        } catch (Exception ex) {
            scriptInfo.setText("Failed to load .gbot file:\n" + ex.getMessage());
        }
    }

    private String getDisplayName(Uri uri) {
        String name = "loaded-script.gbot";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) { }
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

    private ScriptSummary parseGbot(String text) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder preview = new StringBuilder();
        int commands = 0;
        for (int i = 0; i < lines.length; i++) {
            String t = cleanLine(lines[i]);
            if (t.length() == 0) continue;
            commands++;
            if (commands <= 40) {
                preview.append(String.format(Locale.US, "%03d  %s\n", i + 1, t));
            }
        }
        if (commands > 40) preview.append("... ").append(commands - 40).append(" more commands");
        return new ScriptSummary(lines.length, commands, preview.toString());
    }

    private String cleanLine(String line) {
        String t = line.trim();
        if (t.length() == 0) return "";
        if (t.startsWith("//") || t.startsWith("#") || t.startsWith(";") || t.startsWith("--")) return "";
        return t;
    }

    private void refreshScriptInfo() {
        String name = prefs.getString("scriptName", "No .gbot script loaded");
        int lines = prefs.getInt("scriptLineCount", 0);
        int commands = prefs.getInt("scriptCommandCount", 0);
        String summary = prefs.getString("scriptSummary", "");
        scriptInfo.setText("Loaded Script: " + name + "\nLines: " + lines + "\nCommands detected: " + commands + "\n\nCommand Preview:\n" + summary);
    }

    private void openAqwPocket() {
        String[] packages = new String[] {
            "air.AQWPocket", "air.com.aqwpocket", "com.aqwpocket", "com.anthonyhyo.aqwpocket"
        };
        for (String p : packages) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(p);
            if (launch != null) { startActivity(launch); return; }
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anthony-hyo/aqw-mobile/releases/latest"));
        startActivity(i);
    }

    private static class ScriptSummary {
        int totalLines;
        int commands;
        String summary;
        ScriptSummary(int totalLines, int commands, String summary) {
            this.totalLines = totalLines;
            this.commands = commands;
            this.summary = summary;
        }
    }
}
