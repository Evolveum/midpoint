/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;

/**
 * A free-form string value with a label. Useful for displaying a free-form data in forms and tables that
 * require a label.
 *
 * IMMUTABLE
 *
 * @author Radovan Semancik
 *
 */
public class LabeledString implements Serializable {

    private final String label;
    private final String data;

    public LabeledString(String label, String data) {
        this.label = label;
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    public String getData() {
        return data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        LabeledString other = (LabeledString) obj;
        if (data == null) {
            if (other.data != null) return false;
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) return false;
        } else if (!label.equals(other.label)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LabeledString(" + label + ": " + data + ")";
    }

}
