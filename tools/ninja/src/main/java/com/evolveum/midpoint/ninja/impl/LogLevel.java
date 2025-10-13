/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.impl;

import com.evolveum.midpoint.ninja.util.ConsoleFormat;

import org.fusesource.jansi.Ansi;

public enum LogLevel {

    ERROR("ERROR", ConsoleFormat.Color.ERROR.color),

    WARNING("WARNING", ConsoleFormat.Color.WARN.color),

    INFO("INFO", ConsoleFormat.Color.INFO.color),

    DEBUG("DEBUG", ConsoleFormat.Color.DEFAULT.color);

    private final String label;

    private final Ansi.Color color;

    LogLevel(String label, Ansi.Color color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public Ansi.Color color() {
        return color;
    }
}
