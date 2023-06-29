package com.evolveum.midpoint.ninja.util;

import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.action.Action;

public final class ConsoleFormat {

    private ConsoleFormat() {
    }

    public static String formatActionStartMessage(Action action) {
        return Ansi.ansi().a("Starting ").fgGreen().a(action.getOperationName()).reset().toString();
    }

    public static String formatSuccessMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Ansi.Color.GREEN);
    }

    public static String formatWarnMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Ansi.Color.YELLOW);
    }

    public static String formatErrorMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Ansi.Color.RED);
    }

    public static String formatInfoMessageWithParameter(String message, Object parameter) {
        return formatMessageWithParameter(message, parameter, Ansi.Color.BLUE);
    }

    public static String formatMessageWithParameter(String message, Object parameter, Ansi.Color parameterColor) {
        return Ansi.ansi().a(message).fg(parameterColor).a(parameter).reset().toString();
    }

    public static String formatWarn(String message) {
        return format(message, Ansi.Color.YELLOW);
    }

    public static String formatError(String message) {
        return format(message, Ansi.Color.RED);
    }

    public static String format(String message, Ansi.Color parameterColor) {
        return Ansi.ansi().fg(parameterColor).a(message).reset().toString();
    }
}
