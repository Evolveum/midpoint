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

    public static String formatMessageWithErrorParameters(String message, Object... parameters) {
        return formatMessageWithParameter(message, parameters, Level.ERROR);
    }

    public static String formatMessageWithWarningParameters(String message, Object... parameters) {
        return formatMessageWithParameter(message, parameters, Level.WARN);
    }

    public static String formatMessageWithInfoParameters(String message, Object... parameters) {
        return formatMessageWithParameter(message, parameters, Level.INFO);
    }

    public static String formatMessageWithParameter(String message, Object[] parameters, Level level) {
        String[] formatted = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            formatted[i] = Ansi.ansi().fgBright(level.color).a(parameters[i]).reset().toString();
        }

        return NinjaUtils.printFormatted(message, formatted);
    }

    public static String formatLogMessage(LogLevel level, String msg) {
        return Ansi.ansi()
                .reset()
                .a("[").fgBright(level.color()).a(level.label()).reset().a("] ")
                .a(msg)
                .toString();
    }
}
