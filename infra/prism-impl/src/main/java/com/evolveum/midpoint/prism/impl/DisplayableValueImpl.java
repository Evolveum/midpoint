/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl;

import java.io.Serializable;

import com.evolveum.midpoint.util.DisplayableValue;

public class DisplayableValueImpl<T> implements DisplayableValue<T>, Serializable{

    private T value;
    private String label;
    private String description;

    public DisplayableValueImpl(T value, String label, String description) {
        this.label = label;
        this.value = value;
        this.description = description;
    }

    @Override
    public T getValue() {
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
        return "DisplayableValueImpl(" + value + ": " + label + " (" + description
                + "))";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DisplayableValueImpl<?> that = (DisplayableValueImpl<?>) o;

        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        return !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
