package com.grimliterev.overlay.gbot;

import java.util.*;

public class GbotParser {

    public static GbotScript parse(String name, String text) {
        List<GbotScript.CommandLine> commands = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();
        String[] lines = text.split("\r?\n");

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty()) continue;
            if (raw.startsWith("//") || raw.startsWith("#") || raw.startsWith(";") || raw.startsWith("--")) continue;

            // Label definition: "label foo" or "[foo]"
            if (raw.startsWith("label ")) {
                String labelName = raw.substring(6).trim().toLowerCase();
                labels.put(labelName, commands.size());
                commands.add(new GbotScript.CommandLine(i + 1, GbotCommand.LABEL, raw, Arrays.asList(labelName)));
                continue;
            }
            if (raw.startsWith("[") && raw.endsWith("]")) {
                String labelName = raw.substring(1, raw.length() - 1).trim().toLowerCase();
                labels.put(labelName, commands.size());
                commands.add(new GbotScript.CommandLine(i + 1, GbotCommand.LABEL, raw, Arrays.asList(labelName)));
                continue;
            }

            List<String> tokens = tokenize(raw);
            if (tokens.isEmpty()) continue;

            String cmd = tokens.get(0).toLowerCase();
            List<String> args = tokens.subList(1, tokens.size());
            GbotCommand type = mapCommand(cmd);
            commands.add(new GbotScript.CommandLine(i + 1, type, raw, args));
        }
        return new GbotScript(name, commands, labels);
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) tokens.add(current.toString());
        return tokens;
    }

    private static GbotCommand mapCommand(String cmd) {
        switch (cmd) {
            case "delay": case "sleep": case "wait": return GbotCommand.DELAY;
            case "goto": case "jump": return GbotCommand.GOTO;
            case "if": return GbotCommand.IF;
            case "tap": case "click": return GbotCommand.TAP;
            case "swipe": return GbotCommand.SWIPE;
            case "longpress": case "long_press": return GbotCommand.LONG_PRESS;
            case "kill": return GbotCommand.KILL;
            case "join": case "map": case "room": return GbotCommand.JOIN;
            case "accept": case "getquest": return GbotCommand.ACCEPT;
            case "turnin": case "complete": case "turn": return GbotCommand.TURNIN;
            case "rest": case "heal": return GbotCommand.REST;
            case "skill": case "ability": case "use": return GbotCommand.SKILL;
            case "move": case "walk": case "goto_pos": return GbotCommand.MOVE;
            case "buy": return GbotCommand.BUY;
            case "sell": return GbotCommand.SELL;
            case "equip": return GbotCommand.EQUIP;
            case "unequip": return GbotCommand.UNEQUIP;
            case "stop": case "exit": case "end": return GbotCommand.STOP;
            default: return GbotCommand.UNKNOWN;
        }
    }
}
