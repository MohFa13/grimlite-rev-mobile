package com.grimliterev.overlay.gbot;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.grimliterev.overlay.game.GameInteractor;

import java.util.*;

public class ScriptExecutor {
    private static final String TAG = "ScriptExecutor";

    public interface Callback {
        void onStatus(String status);
        void onCommand(int index, String raw);
        void onFinished(String reason);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GameInteractor interactor;
    private Callback callback;
    private GbotScript script;
    private int index = 0;
    private boolean running = false;
    private final Map<String, String> variables = new HashMap<>();
    private int loopCounter = 0;
    private static final int MAX_LOOPS = 50000; // safety break

    public ScriptExecutor(GameInteractor interactor) {
        this.interactor = interactor;
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public void load(GbotScript script) {
        stop("Reloaded");
        this.script = script;
        this.index = 0;
        this.loopCounter = 0;
        this.variables.clear();
    }

    public void start() {
        if (script == null || script.commands.isEmpty()) {
            notifyStatus("No script loaded");
            return;
        }
        if (running) return;
        running = true;
        notifyStatus("Running: " + script.name);
        tick();
    }

    public void stop(String reason) {
        running = false;
        handler.removeCallbacksAndMessages(null);
        notifyFinished(reason);
    }

    public boolean isRunning() {
        return running;
    }

    public void next() {
        if (!running || script == null) return;
        tick();
    }

    private void tick() {
        if (!running || script == null) return;
        if (index >= script.commands.size()) {
            stop("Script finished");
            return;
        }
        if (loopCounter++ > MAX_LOOPS) {
            stop("Safety limit reached");
            return;
        }

        GbotScript.CommandLine cmd = script.commands.get(index);
        notifyCommand(index, cmd.raw);

        long delay = execute(cmd);
        index++;

        if (running) {
            if (delay > 0) {
                handler.postDelayed(this::tick, delay);
            } else {
                handler.post(this::tick);
            }
        }
    }

    private long execute(GbotScript.CommandLine cmd) {
        List<String> args = cmd.args;
        try {
            switch (cmd.type) {
                case DELAY:
                    return args.isEmpty() ? 500 : parseInt(args.get(0), 500);
                case GOTO:
                    if (!args.isEmpty()) {
                        String label = args.get(0).toLowerCase();
                        Integer target = script.labels.get(label);
                        if (target != null) {
                            index = target - 1; // tick() will ++
                            notifyStatus("Goto " + label);
                        } else {
                            notifyStatus("Unknown label: " + label);
                        }
                    }
                    return 0;
                case IF:
                    return handleIf(cmd.raw, args);
                case TAP:
                    if (args.size() >= 2) {
                        interactor.tap(parseInt(args.get(0), 0), parseInt(args.get(1), 0));
                    }
                    return 200;
                case SWIPE:
                    if (args.size() >= 5) {
                        interactor.swipe(
                            parseInt(args.get(0), 0), parseInt(args.get(1), 0),
                            parseInt(args.get(2), 0), parseInt(args.get(3), 0),
                            parseInt(args.get(4), 300)
                        );
                    }
                    return 300;
                case LONG_PRESS:
                    if (args.size() >= 2) {
                        interactor.longPress(parseInt(args.get(0), 0), parseInt(args.get(1), 0), 800);
                    }
                    return 800;
                case KILL:
                    interactor.kill(args.isEmpty() ? "" : args.get(0));
                    return 2000;
                case JOIN:
                    interactor.join(args.isEmpty() ? "" : args.get(0));
                    return 3000;
                case ACCEPT:
                    interactor.acceptQuest(args.isEmpty() ? "" : args.get(0));
                    return 1500;
                case TURNIN:
                    interactor.turnInQuest(args.isEmpty() ? "" : args.get(0));
                    return 1500;
                case REST:
                    interactor.rest();
                    return 2000;
                case SKILL:
                    interactor.useSkill(args.isEmpty() ? 1 : parseInt(args.get(0), 1));
                    return 800;
                case MOVE:
                    if (args.size() >= 2) {
                        interactor.move(parseInt(args.get(0), 0), parseInt(args.get(1), 0));
                    }
                    return 1500;
                case BUY:
                    interactor.buy(args.isEmpty() ? "" : args.get(0));
                    return 1500;
                case SELL:
                    interactor.sell(args.isEmpty() ? "" : args.get(0));
                    return 1500;
                case EQUIP:
                    interactor.equip(args.isEmpty() ? "" : args.get(0));
                    return 1000;
                case UNEQUIP:
                    interactor.unequip(args.isEmpty() ? "" : args.get(0));
                    return 1000;
                case STOP:
                    stop("Stop command");
                    return 0;
                case LABEL:
                    return 0;
                case UNKNOWN:
                    notifyStatus("Unknown: " + cmd.raw);
                    return 0;
                default:
                    return 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing: " + cmd.raw, e);
            notifyStatus("Error: " + e.getMessage());
            return 500;
        }
    }

    private long handleIf(String raw, List<String> args) {
        // Simple if parser: "if <var/condition> goto <label>"
        // Supports: "if killed goto end", "if gold > 1000 goto bank"
        if (args.size() < 3) return 0;
        // Find "goto" keyword
        int gotoIdx = -1;
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equalsIgnoreCase("goto")) { gotoIdx = i; break; }
        }
        if (gotoIdx < 1 || gotoIdx >= args.size() - 1) return 0;

        String label = args.get(gotoIdx + 1).toLowerCase();
        boolean condition = evaluateCondition(args.subList(0, gotoIdx));

        if (condition) {
            Integer target = script.labels.get(label);
            if (target != null) {
                index = target - 1;
                notifyStatus("If true -> " + label);
            }
        }
        return 0;
    }

    private boolean evaluateCondition(List<String> tokens) {
        if (tokens.isEmpty()) return false;
        // Very basic condition evaluation
        String left = tokens.get(0).toLowerCase();
        // Check variables
        String val = variables.get(left);
        if (val != null) left = val;

        if (tokens.size() == 1) {
            // e.g., "if killed goto ..." -> check interactor state
            return interactor.getState(left);
        }
        if (tokens.size() >= 3) {
            String op = tokens.get(1);
            String right = tokens.get(2);
            try {
                int l = parseInt(left, 0);
                int r = parseInt(right, 0);
                switch (op) {
                    case ">": return l > r;
                    case "<": return l < r;
                    case ">=": return l >= r;
                    case "<=": return l <= r;
                    case "==": case "=": return l == r;
                    case "!=": return l != r;
                }
            } catch (NumberFormatException ignored) {}
            // String compare
            switch (op) {
                case "==": case "=": return left.equalsIgnoreCase(right);
                case "!=": return !left.equalsIgnoreCase(right);
            }
        }
        return false;
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private void notifyStatus(String s) {
        if (callback != null) callback.onStatus(s);
    }
    private void notifyCommand(int idx, String raw) {
        if (callback != null) callback.onCommand(idx, raw);
    }
    private void notifyFinished(String reason) {
        if (callback != null) callback.onFinished(reason);
    }
}
