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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.grimliterev.overlay.game.CoordinateMap;
import com.grimliterev.overlay.gbot.GbotParser;
import com.grimliterev.overlay.gbot.GbotScript;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static final int PICK_GBOT_FILE = 2001;
    private SharedPreferences prefs;
    private TextView scriptInfo;
    private LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("grimlite_overlay", MODE_PRIVATE);
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(rootLayout);

        String action = getIntent().getAction();
        if ("coord_setup".equals(action)) {
            buildCoordSetup();
        } else {
            buildMainLayout();
            refreshScriptInfo();
            if ("load_gbot".equals(getIntent().getAction())) openGbotPicker();
        }
    }

    private void buildMainLayout() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 28, 40, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);
        rootLayout.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("Grimlite Rev Mobile Overlay");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(30, 30, 30));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView desc = new TextView(this);
        desc.setText("Landscape-compatible Grimlite-style overlay for AQW Pocket. Load .gbot files, configure tap coordinates, then execute scripts over AQW Pocket via AccessibilityService.");
        desc.setTextSize(15);
        desc.setPadding(0, 18, 0, 18);
        root.addView(desc, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        root.addView(buttons, new LinearLayout.LayoutParams(-1, -2));

        buttons.addView(button("Grant Overlay", v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }));
        buttons.addView(button("Enable Accessibility", v -> {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(i);
            Toast.makeText(this, "Enable Grimlite Bot Accessibility Service", Toast.LENGTH_LONG).show();
        }));
        buttons.addView(button("Load .gbot", v -> openGbotPicker()));
        buttons.addView(button("Start Overlay", v -> startService(new Intent(this, OverlayService.class))));
        buttons.addView(button("Stop Overlay", v -> stopService(new Intent(this, OverlayService.class))));
        buttons.addView(button("Set Coords", v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.setAction("coord_setup");
            startActivity(i);
        }));
        buttons.addView(button("Open AQW Pocket", v -> openAqwPocket()));

        scriptInfo = new TextView(this);
        scriptInfo.setTextSize(14);
        scriptInfo.setTextColor(Color.rgb(45, 45, 45));
        scriptInfo.setBackgroundColor(Color.rgb(245, 245, 245));
        scriptInfo.setPadding(20, 20, 20, 20);
        root.addView(scriptInfo, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildCoordSetup() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 28, 40, 28);
        scroll.addView(root);
        rootLayout.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        TextView title = new TextView(this);
        title.setText("Coordinate Setup");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(30, 30, 30));
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Set screen tap coordinates for AQW Pocket actions. Values are in screen pixels. Use Android Developer Options -> Pointer Location to find coordinates.");
        desc.setTextSize(14);
        desc.setPadding(0, 12, 0, 12);
        root.addView(desc);

        CoordinateMap map = new CoordinateMap(prefs);
        String[] actions = {
            "attack", "skill1", "skill2", "skill3", "skill4",
            "rest", "menu", "join", "confirm",
            "quest", "accept", "turnin",
            "shop", "buy", "sell",
            "inventory", "equip", "unequip"
        };

        for (String action : actions) {
            int[] p = map.get(action);
            root.addView(makeCoordRow(action, p[0], p[1]));
        }

        Button save = new Button(this);
        save.setText("Save All Coordinates");
        save.setOnClickListener(v -> {
            for (int i = 0; i < root.getChildCount(); i++) {
                android.view.View child = root.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout row = (LinearLayout) child;
                    TextView label = (TextView) row.getChildAt(0);
                    EditText ex = (EditText) row.getChildAt(1);
                    EditText ey = (EditText) row.getChildAt(2);
                    String act = label.getText().toString().replace(":", "").trim();
                    int x = parseInt(ex.getText().toString(), -1);
                    int y = parseInt(ey.getText().toString(), -1);
                    if (x >= 0 && y >= 0) map.set(act, x, y);
                }
            }
            Toast.makeText(this, "Coordinates saved", Toast.LENGTH_SHORT).show();
            finish();
        });
        root.addView(save);

        Button back = new Button(this);
        back.setText("Back");
        back.setOnClickListener(v -> finish());
        root.addView(back);
    }

    private LinearLayout makeCoordRow(String action, int x, int y) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView label = new TextView(this);
        label.setText(action + ":");
        label.setWidth(180);
        row.addView(label);

        EditText ex = new EditText(this);
        ex.setHint("X");
        ex.setText(x >= 0 ? String.valueOf(x) : "");
        ex.setWidth(200);
        row.addView(ex);

        EditText ey = new EditText(this);
        ey.setHint("Y");
        ey.setText(y >= 0 ? String.valueOf(y) : "");
        ey.setWidth(200);
        row.addView(ey);

        return row;
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
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
            GbotScript script = GbotParser.parse(name, text);

            prefs.edit()
                .putString("script", text)
                .putString("scriptName", name)
                .putInt("scriptLineCount", text.split("\r?\n").length)
                .putInt("scriptCommandCount", script.commands.size())
                .putString("scriptSummary", buildSummary(script))
                .putString("status", "Loaded " + name)
                .apply();

            startService(new Intent(this, OverlayService.class));
            refreshScriptInfo();
            Toast.makeText(this, "Loaded " + name + " (" + script.commands.size() + " commands)", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            scriptInfo.setText("Failed to load .gbot file:\n" + ex.getMessage());
        }
    }

    private String buildSummary(GbotScript script) {
        StringBuilder sb = new StringBuilder();
        int max = Math.min(script.commands.size(), 40);
        for (int i = 0; i < max; i++) {
            sb.append(String.format(java.util.Locale.US, "%03d %s\n", i + 1, script.commands.get(i).raw));
        }
        if (script.commands.size() > max) sb.append("... ").append(script.commands.size() - 40).append(" more commands");
        return sb.toString();
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

    private void refreshScriptInfo() {
        String name = prefs.getString("scriptName", "No .gbot script loaded");
        int lines = prefs.getInt("scriptLineCount", 0);
        int commands = prefs.getInt("scriptCommandCount", 0);
        String summary = prefs.getString("scriptSummary", "");
        scriptInfo.setText("Loaded Script: " + name + "\nLines: " + lines + "\nCommands: " + commands + "\n\nCommand Preview:\n" + summary);
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
}
