/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.schema.expression.VariablesMap;

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
     * VariablesMap from the line.
     */
    @NotNull private final VariablesMap variables;

    private Object correlationValue;

    InputReportLine(int lineNumber, @NotNull VariablesMap variables) {
        this.lineNumber = lineNumber;
        this.variables = variables;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public @NotNull VariablesMap getVariables() {
        return variables;
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
                ", variables='" + variables + "'" +
                ", correlationValue=" + correlationValue +
                '}';
    }
}
