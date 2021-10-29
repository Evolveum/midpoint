/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Formats bucket content value into three columns (characterization, from, to).
 */
public class BucketContentFormatter implements Formatter {

    @Override
    public @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def) {
        String name = def.getItemName().getLocalPart();
        return List.of(name, name + "-from", name + "-to");
    }

    @Override
    public @NotNull List<String> formatValue(Object v) {
        if (v == null || v instanceof NullWorkBucketContentType) {
            return List.of("", "", "");
        } else if (v instanceof FilterWorkBucketContentType) {
            FilterWorkBucketContentType f = (FilterWorkBucketContentType) v;
            return List.of(
                    String.format("%d filters", f.getFilter().size()),
                    "", "");
        } else if (v instanceof StringIntervalWorkBucketContentType) {
            StringIntervalWorkBucketContentType si = (StringIntervalWorkBucketContentType) v;
            String from = MiscUtil.emptyIfNull(si.getFrom());
            String to = MiscUtil.emptyIfNull(si.getTo());
            return List.of(
                    String.format("[%s-%s)", from, to),
                    from, to);
        } else if (v instanceof StringPrefixWorkBucketContentType) {
            StringPrefixWorkBucketContentType sp = (StringPrefixWorkBucketContentType) v;
            String text = sp.getPrefix().size() == 1 ? sp.getPrefix().get(0) : String.valueOf(sp.getPrefix());
            return List.of(
                    String.format("prefix: %s", text), text, "");
        } else if (v instanceof NumericIntervalWorkBucketContentType) {
            NumericIntervalWorkBucketContentType ni = (NumericIntervalWorkBucketContentType) v;
            String from = ni.getFrom() != null ? String.valueOf(ni.getFrom()) : "";
            String to = ni.getTo() != null ? String.valueOf(ni.getTo()) : "";
            return List.of(
                    String.format("[%s-%s)", from, to),
                    from, to);
        } else if (v instanceof StringValueWorkBucketContentType) {
            StringValueWorkBucketContentType sv = (StringValueWorkBucketContentType) v;
            String text = sv.getValue().size() == 1 ? sv.getValue().get(0) : String.valueOf(sv.getValue());
            return List.of(
                    String.format("value: %s", text), text, "");
        } else {
            return List.of(
                    String.valueOf(v), "", "");
        }
    }

    @Override
    public @NotNull List<String> formatMultiValue(Collection<?> values) {
        return List.of(
                String.format("%d values?", values.size()),
                "", "");
    }
}
