/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Formats bucket content value into three columns (characterization, from, to).
 */
public class QualifiedOutcomeFormatter implements Formatter {

    @Override
    public @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def) {
        String name = def.getItemName().getLocalPart();
        return List.of(name, name + "-qualifier");
    }

    @Override
    public @NotNull List<String> formatValue(Object v) {
        if (!(v instanceof QualifiedItemProcessingOutcomeType)) {
            return List.of("", "");
        } else {
            QualifiedItemProcessingOutcomeType outcome = (QualifiedItemProcessingOutcomeType) v;
            return List.of(
                    String.valueOf(outcome.getOutcome()),
                    MiscUtil.emptyIfNull(outcome.getQualifierUri()));
        }
    }

    @Override
    public @NotNull List<String> formatMultiValue(Collection<?> values) {
        return List.of(
                String.format("%d values?", values.size()),
                "");
    }
}
