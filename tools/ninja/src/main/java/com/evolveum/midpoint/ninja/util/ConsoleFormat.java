package com.evolveum.midpoint.ninja.util;

import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.action.Action;

public final class ConsoleFormat {

    public enum Level {

        DEFAULT(Ansi.Color.DEFAULT),

        INFO(Ansi.Color.BLUE),

        SUCCESS(Ansi.Color.GREEN),

        WARN(Ansi.Color.YELLOW),

        ERROR(Ansi.Color.RED);

        final Ansi.Color color;

        Level(Ansi.Color color) {
            this.color = color;
        }
    }

    private ConsoleFormat() {
    }

    public static void setBatchMode(boolean batchMode) {
        Ansi.setEnabled(batchMode);
    }

    public static String formatActionStartMessage(Action action) {
        String operation = action.getOperationName();
        return Ansi.ansi().a("Starting ").fgGreen().a(operation).reset().toString();
    }

    public static String formatSuccessMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Level.SUCCESS);
    }

    public static String formatWarnMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Level.WARN);
    }

    public static String formatErrorMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Level.ERROR);
    }

    public static String formatInfoMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Level.INFO);
    }

    public static String formatMessageWithParameter(String message, Object parameter, Level level) {
        return Ansi.ansi().a(message).fg(level.color).a(parameter).reset().toString();
    }

    public static String formatWarn(String message) {
        return format(message, Level.WARN);
    }

    public static String formatError(String message) {
        return format(message, Level.ERROR);
    }

    public static String format(String message, Level level) {
        return Ansi.ansi().fg(level.color).a(message).reset().toString();
    }
}
