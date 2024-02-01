/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CorrelationPropertyValuesDescription;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Values (primary and secondary) for given correlation property. Currently in their {@link String} form.
 */
public class CorrelationPropertyValues implements Serializable {

    @NotNull private final Set<String> primaryValues;
    @NotNull private final Set<String> secondaryValues;

    private CorrelationPropertyValues(@NotNull Set<String> primaryValues, @NotNull Set<String> secondaryValues) {
        this.primaryValues = primaryValues;
        this.secondaryValues = Sets.difference(secondaryValues, primaryValues);
    }

    static @NotNull CorrelationPropertyValues fromDescription(
            @Nullable CorrelationPropertyValuesDescription description) {
        if (description != null) {
            return new CorrelationPropertyValues(
                    stringify(description.getPrimaryValues()),
                    stringify(description.getSecondaryValues()));
        } else {
            return new CorrelationPropertyValues(Set.of(), Set.of());
        }
    }

    static @NotNull CorrelationPropertyValues fromObject(
            @NotNull PrismObject<?> object, @NotNull ItemPath path) {
        return new CorrelationPropertyValues(stringify(object.getAllValues(path)), Set.of());
    }

    private static Set<String> stringify(@NotNull Collection<? extends PrismValue> values) {
        try {
            return values.stream()
                    .map(PrismValue::getRealValue)
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            // "Get real value" method can throw an exception, so let's avoid crashing GUI because of that.
            return Set.of(e.getMessage());
        }
    }

    public String format() {
        String primary = getValuesAsString(primaryValues);
        if (secondaryValues.isEmpty()) {
            return primary;
        } else {
            return primary + " (" + getValuesAsString(secondaryValues) + ")";
        }
    }

    private String getValuesAsString(Set<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return "CorrelationPropertyValues{" +
                "primaryValues=" + primaryValues +
                ", secondaryValues=" + secondaryValues +
                '}';
    }
}
