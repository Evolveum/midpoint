/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import com.evolveum.midpoint.prism.ItemDefinition;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Formats any object value.
 */
public class GeneralFormatter implements Formatter {

    @Override
    public @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def) {
        return List.of(def.getItemName().getLocalPart());
    }

    @Override
    public @NotNull List<String> formatValue(Object object) {
        if (object != null) {
            return List.of(String.valueOf(object));
        } else {
            return List.of("");
        }
    }

    @Override
    public @NotNull List<String> formatMultiValue(Collection<?> values) {
        return List.of(String.valueOf(values));
    }
}
