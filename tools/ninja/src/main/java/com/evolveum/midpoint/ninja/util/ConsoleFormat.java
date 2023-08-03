package com.evolveum.midpoint.ninja.util;

import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.LogLevel;

/**
 * TODO think this through - how to format different messages
 */
public final class ConsoleFormat {

    public enum Level {

        DEFAULT(Ansi.Color.DEFAULT),

        INFO(Ansi.Color.BLUE),

        SUCCESS(Ansi.Color.GREEN),

        WARN(Ansi.Color.YELLOW),

        ERROR(Ansi.Color.RED);

        public final Ansi.Color color;

        Level(Ansi.Color color) {
            this.color = color;
        }
    }

    private ConsoleFormat() {
    }

    public static void setBatchMode(boolean batchMode) {
        Ansi.setEnabled(!batchMode);
    }

    public static String formatActionStartMessage(Action action) {
        String operation = action.getOperationName();
        return Ansi.ansi().a("Starting ").fgGreen().a(operation).reset().toString();
    }

    public static String formatInfoMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Level.INFO);
    }

    public static String formatMessageWithParameter(String message, Object parameter, Level level) {
        return Ansi.ansi().a(message).fgBright(level.color).a(parameter).reset().toString();
    }

    public static String formatLogMessage(LogLevel level, String msg) {
        return Ansi.ansi()
                .reset()
                .a("[").fgBright(level.color()).a(level.label()).reset().a("] ")
                .a(msg)
                .toString();
    }
}
