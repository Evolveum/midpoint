/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Values (primary and secondary) for given correlation property.
 */
public class CorrelationPropertyValues implements Serializable {

    @NotNull private final Set<String> primaryValues;
    @NotNull private final Set<String> secondaryValues;
    @NotNull private final Set<String> allValues;

    CorrelationPropertyValues(@NotNull Set<String> primaryValues, @NotNull Set<String> secondaryValues) {
        this.primaryValues = primaryValues;
        this.secondaryValues = Sets.difference(secondaryValues, primaryValues);
        this.allValues = Sets.union(primaryValues, secondaryValues);
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

    /**
     * Uses `this` as a reference, and evaluates how the provided values match it.
     */
    public Match match(CorrelationPropertyValues other) {

        Set<String> referenceValues = this.primaryValues;
        stateCheck(secondaryValues.isEmpty(), "Reference cannot have secondary values: %s", this);

        if (referenceValues.isEmpty()) {
            return Match.NOT_APPLICABLE; // What if "other" has some values?
        }

        if (other.primaryValues.containsAll(referenceValues)) {
            return Match.FULL; // We may distinguish proper subset vs equality
        }

        if (!Sets.intersection(other.allValues, referenceValues).isEmpty()) {
            return Match.PARTIAL; // There may or may not be reference values not present in other: but there's some overlap.
        }

        return Match.NONE;
    }

    @Override
    public String toString() {
        return "CorrelationPropertyValues{" +
                "primaryValues=" + primaryValues +
                ", secondaryValues=" + secondaryValues +
                '}';
    }
}
