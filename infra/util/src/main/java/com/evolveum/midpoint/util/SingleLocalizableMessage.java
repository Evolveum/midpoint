/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class is final and must remain so, as it's not cloneable.
 *
 * @author semancik
 */
public class SingleLocalizableMessage implements LocalizableMessage {
    private static final long serialVersionUID = 1L;

    private final String key;
    private final Object[] args;
    // at most one of the following can be present
    private final LocalizableMessage fallbackLocalizableMessage;
    private final String fallbackMessage;

    public SingleLocalizableMessage(String key, Object[] args, LocalizableMessage fallbackLocalizableMessage) {
        super();
        this.key = key;
        this.args = args;
        this.fallbackLocalizableMessage = fallbackLocalizableMessage;
        this.fallbackMessage = null;
    }

    public SingleLocalizableMessage(String key, Object[] args, String fallbackMessage) {
        super();
        this.key = key;
        this.args = args;
        this.fallbackLocalizableMessage = null;
        this.fallbackMessage = fallbackMessage;
    }

    /**
     * Message key. This is the key in localization files that
     * determine message or message template.
     */
    public String getKey() {
        return key;
    }

    /**
     * Message template arguments.
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * Fallback message. This message is used in case that the
     * message key cannot be found in the localization files.
     */
    public String getFallbackMessage() {
        return fallbackMessage;
    }

    /**
     * Fallback localization message. This message is used in case that the
     * message key cannot be found in the localization files.
     */
    public LocalizableMessage getFallbackLocalizableMessage() {
        return fallbackLocalizableMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SingleLocalizableMessage)) {
            return false;
        }
        SingleLocalizableMessage that = (SingleLocalizableMessage) o;
        return Objects.equals(key, that.key) &&
                Arrays.equals(args, that.args) &&
                Objects.equals(fallbackLocalizableMessage, that.fallbackLocalizableMessage) &&
                Objects.equals(fallbackMessage, that.fallbackMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, args, fallbackLocalizableMessage, fallbackMessage);
    }

    @Override
    public String toString() {
        return "SingleLocalizableMessage(" + key + ": " + Arrays.toString(args) + " ("
                + (fallbackMessage != null ? fallbackMessage : fallbackLocalizableMessage) + "))";
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (key != null) {
            sb.append(key);
            if (args != null) {
                sb.append(": ");
                sb.append(Arrays.toString(args));
            }
            if (fallbackMessage != null) {
                sb.append(" (");
                sb.append(fallbackMessage);
                sb.append(")");
            }
            if (fallbackLocalizableMessage != null) {
                sb.append(" (");
                sb.append(fallbackLocalizableMessage.shortDump());
                sb.append(")");
            }
        } else {
            sb.append(fallbackLocalizableMessage != null ? fallbackLocalizableMessage.shortDump() : fallbackMessage);
        }
    }

    @Override
    public boolean isEmpty() {
        return key == null && LocalizableMessage.isEmpty(fallbackLocalizableMessage) && fallbackMessage == null;
    }
}
