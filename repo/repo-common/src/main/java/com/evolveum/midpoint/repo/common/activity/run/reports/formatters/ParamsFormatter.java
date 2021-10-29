/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.util.ParamsTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;

/**
 * Formatter for `ParamsType` objects.
 */
public class ParamsFormatter implements Formatter {

    @Override
    public @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def) {
        String name = def.getItemName().getLocalPart();
        return List.of(name);
    }

    @Override
    public @NotNull List<String> formatValue(Object v) {
        if (!(v instanceof ParamsType)) {
            return List.of("");
        } else {
            return List.of(formatParams((ParamsType) v));
        }
    }

    private String formatParams(ParamsType params) {
        Map<String, Collection<String>> map = ParamsTypeUtil.fromParamsType(params);
        return map.entrySet().stream()
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("; "));
    }

    @Override
    public @NotNull List<String> formatMultiValue(Collection<?> values) {
        return List.of(
                String.format("%d values?", values.size()));
    }
}
