/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author semancik
 *
 */
public class LocalizableMessageBuilder {

    private String key;
    private final List<Object> args = new ArrayList<>();
    private String fallbackMessage;
    private LocalizableMessage fallbackLocalizableMessage;

    public LocalizableMessageBuilder() {
    }

    public LocalizableMessageBuilder key(String key) {
        this.key = key;
        return this;
    }

    public static SingleLocalizableMessage buildKey(String key) {
        return new SingleLocalizableMessage(key, null, (SingleLocalizableMessage) null);
    }

    public LocalizableMessageBuilder args(Object... args) {
        Collections.addAll(this.args, args);
        return this;
    }

    public LocalizableMessageBuilder args(List<Object> args) {
        this.args.addAll(args);
        return this;
    }

    public LocalizableMessageBuilder arg(Object arg) {
        this.args.add(arg);
        return this;
    }

    public LocalizableMessageBuilder fallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
        return this;
    }

    public LocalizableMessageBuilder fallbackLocalizableMessage(LocalizableMessage fallbackLocalizableMessage) {
        this.fallbackLocalizableMessage = fallbackLocalizableMessage;
        return this;
    }

    public static SingleLocalizableMessage buildFallbackMessage(String fallbackMessage) {
        return new SingleLocalizableMessage(null, null, fallbackMessage);
    }

    public SingleLocalizableMessage build() {
        if (fallbackMessage != null) {
            if (fallbackLocalizableMessage != null) {
                throw new IllegalStateException("fallbackMessage and fallbackLocalizableMessage cannot be both set");
            }
            return new SingleLocalizableMessage(key, args.toArray(), fallbackMessage);
        } else {
            return new SingleLocalizableMessage(key, args.toArray(), fallbackLocalizableMessage);
        }
    }
}
