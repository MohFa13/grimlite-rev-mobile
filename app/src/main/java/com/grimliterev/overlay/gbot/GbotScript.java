package com.grimliterev.overlay.gbot;

import java.util.*;

public class GbotScript {
    public final String name;
    public final List<CommandLine> commands;
    public final Map<String, Integer> labels;

    public GbotScript(String name, List<CommandLine> commands, Map<String, Integer> labels) {
        this.name = name;
        this.commands = Collections.unmodifiableList(commands);
        this.labels = Collections.unmodifiableMap(labels);
    }

    public static class CommandLine {
        public final int lineNumber;
        public final GbotCommand type;
        public final String raw;
        public final List<String> args;

        public CommandLine(int lineNumber, GbotCommand type, String raw, List<String> args) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.raw = raw;
            this.args = args;
        }
    }
}
