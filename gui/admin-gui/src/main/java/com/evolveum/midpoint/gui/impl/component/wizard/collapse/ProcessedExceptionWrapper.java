/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Structured view of a failure reported by the connector development wizard, built from the raw
 * exception so that the drawer can show it as a title, a script, a map of key-value pairs and an
 * explanatory text instead of one long message.
 */
public class ProcessedExceptionWrapper implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private static final Pattern SCRIPT_LOCATION =
            Pattern.compile("([\\w.$-]+\\.groovy):(\\d+)");

    private static final Pattern EXCEPTION_CLASS =
            Pattern.compile("([\\w.$]+(?:Exception|Error|Throwable))");

    private final String script;
    private final Map<Key, String> map;
    private final OperationResult originalException;

    private ProcessedExceptionWrapper(
            String script, Map<Key, String> map, OperationResult originalException) {
        this.script = script;
        this.map = map;
        this.originalException = originalException;
    }

    /**
     * @param fixPanelId step the failure belongs to, used as the script when the message itself
     *                   does not name one
     */
    public static @NotNull ProcessedExceptionWrapper from(
            @NotNull OperationResult result, String message, String fixPanelId) {

        String text = StringUtils.isNotEmpty(message) ? message : result.getMessage();

        String parsedScript = null;
        Integer line = null;
        Matcher location = SCRIPT_LOCATION.matcher(StringUtils.defaultString(text));
        if (location.find()) {
            parsedScript = location.group(1);
            line = Integer.valueOf(location.group(2));
        }

        String script = parsedScript != null ? parsedScript : StringUtils.trimToNull(fixPanelId);

        Map<Key, String> map = new LinkedHashMap<>();
        addIfNotEmpty(map, Key.ERROR_CODE, errorCode(result, text));
        addIfNotEmpty(map, Key.LOCATION, location(parsedScript, line));
        addIfNotEmpty(map, Key.MESSAGE, humanMessage(text));

        return new ProcessedExceptionWrapper(script, map, result);
    }

    /**
     * Either the script the failure comes from, or the wizard step it belongs to when the message
     * does not name a script.
     */
    public String getScript() {
        return script;
    }

    public Map<Key, String> getMap() {
        return map;
    }

    public OperationResult getOriginalException() {
        return originalException;
    }

    private static void addIfNotEmpty(Map<Key, String> map, Key key, String value) {
        if (StringUtils.isNotEmpty(value)) {
            map.put(key, value);
        }
    }

    private static @Nullable String errorCode(OperationResult result, String message) {
        Throwable cause = rootCause(result.getCause());
        if (cause != null) {
            return cause.getClass().getSimpleName();
        }
        Matcher matcher = EXCEPTION_CLASS.matcher(StringUtils.defaultString(message));
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        return lastMatch != null ? StringUtils.substringAfterLast(lastMatch, ".") : null;
    }

    private static @Nullable Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static @Nullable String location(String script, Integer line) {
        if (script == null) {
            return null;
        }
        return line != null ? script + ":" + line : script;
    }

    private static @Nullable String humanMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return null;
        }
        String stripped = SCRIPT_LOCATION.matcher(message).replaceFirst("");
        return StringUtils.removeStart(stripped.trim(), "- ").trim();
    }

    /** Keys of the recognized map rows, translated through {@code ProcessedExceptionPanel.key.*}. */
    public enum Key {
        ERROR_CODE,
        LOCATION,
        MESSAGE
    }
}
