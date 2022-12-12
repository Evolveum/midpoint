/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchValue<T> implements DisplayableValue<T>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String F_VALUE = "value";
    public static final String F_LABEL = "label";

    private T value;
    private String label;
    private String displayName;

    public SearchValue() {
        this(null, null);
    }

    public SearchValue(T value) {
        this.value = value;
    }

    public SearchValue(T value, String label) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String getDescription() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        //the label for ObjectReferenceType should be reloaded according to the current attributes values
        if (value instanceof ObjectReferenceType) {
            String valueToShow = "";
            ObjectReferenceType ort = (ObjectReferenceType) value;
            if (ort.getOid() != null) {
                valueToShow += "oid=" + ort.getOid() + "/";
            }

            if (ort.getType() != null) {
                valueToShow += "type=" + ort.getType().getLocalPart() + "/";
            }

            if (ort.getRelation() != null && !ort.getRelation().equals(PrismConstants.Q_ANY)) {
                valueToShow += "relation=" + ort.getRelation().getLocalPart();
            }
            return valueToShow;
        }
        if (label == null) {
            if (displayName != null) {
                return displayName;
            } else if (value != null) {
                return value.toString();
            }
        }

        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(T value) {
        this.value = value;
        this.label = null;

        if (value instanceof DisplayableValue) {
            DisplayableValue<?> dv = (DisplayableValue) value;
            setLabel(dv.getLabel());
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void clear() {
        value = null;
        label = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        SearchValue<?> that = (SearchValue<?>) o;

        return Objects.equals(label, that.label)
                && Objects.equals(value, that.value);

    }

    @Override
    public int hashCode() {
        return Objects.hash(label, value);
    }

    @Override
    public String toString() {
        return "SearchValue{" +
                "label='" + label + '\'' +
                ", value=" + value +
                '}';
    }
}
