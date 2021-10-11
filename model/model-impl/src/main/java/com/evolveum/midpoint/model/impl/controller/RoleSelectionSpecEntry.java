/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.util.DisplayableValue;

/**
 * @author semancik
 *
 */
public class RoleSelectionSpecEntry implements DisplayableValue<String> {

    boolean negative = false;

    private String value;
    private String label;
    private String description;

    public RoleSelectionSpecEntry(String value, String label, String description) {
        this.label = label;
        this.value = value;
        this.description = description;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "RoleSelectionSpecEntry(" + value + ": " + label + " (" + description + "))";
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public void negate() {
        this.negative = !this.negative;
    }

    public static void negate(Collection<RoleSelectionSpecEntry> col) {
        if (col == null) {
            return;
        }
        for (RoleSelectionSpecEntry entry: col) {
            entry.negate();
        }
    }

    public static boolean hasNegative(Collection<RoleSelectionSpecEntry> col) {
        if (col == null) {
            return false;
        }
        for (RoleSelectionSpecEntry entry: col) {
            if (entry.isNegative()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNegativeValue(Collection<RoleSelectionSpecEntry> col, String value) {
        if (col == null) {
            return false;
        }
        for (RoleSelectionSpecEntry entry: col) {
            if (entry.isNegative() && value.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static Collection<RoleSelectionSpecEntry> getPositive(Collection<RoleSelectionSpecEntry> col) {
        if (col == null) {
            return null;
        }
        Collection<RoleSelectionSpecEntry> out = new ArrayList<>();
        for (RoleSelectionSpecEntry entry: col) {
            if (!entry.isNegative()) {
                out.add(entry);
            }
        }
        return out;
    }
}
