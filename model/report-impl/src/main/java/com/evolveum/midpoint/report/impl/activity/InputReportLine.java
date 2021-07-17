/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a line of report to be imported.
 */
public class InputReportLine {

    /**
     * Line number.
     */
    private final int lineNumber;

    /**
     * Text on the line.
     *
     * TODO - or maybe we can use parsed form here - an advantage would be that we could derive the correlation value more
     *  easily (?)
     */
    @NotNull private final String text;

    private Object correlationValue;

    InputReportLine(int lineNumber, @NotNull String text) {
        this.lineNumber = lineNumber;
        this.text = text;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public @NotNull String getText() {
        return text;
    }

    public Object getCorrelationValue() {
        return correlationValue;
    }

    public void setCorrelationValue(Object correlationValue) {
        this.correlationValue = correlationValue;
    }

    @Override
    public String toString() {
        return "InputReportLine{" +
                "number=" + lineNumber +
                ", text='" + text + '\'' +
                ", correlationValue=" + correlationValue +
                '}';
    }
}
