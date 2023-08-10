/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
