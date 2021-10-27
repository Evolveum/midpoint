/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Formats object reference value into two columns (oid, name).
 *
 * Temporary solution: we need to be able to configure the representation.
 */
public class ObjectReferenceFormatter implements Formatter {

    @Override
    public @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def) {
        String name = def.getItemName().getLocalPart();
        return List.of(name, name + "-name");
    }

    @Override
    public @NotNull List<String> formatValue(Object v) {
        if (!(v instanceof ObjectReferenceType)) {
            return List.of("", "");
        } else {
            ObjectReferenceType ref = (ObjectReferenceType) v;
            return List.of(
                    String.valueOf(ref.getOid()),
                    MiscUtil.emptyIfNull(PolyString.getOrig(ref.getTargetName())));
        }
    }

    @Override
    public @NotNull List<String> formatMultiValue(Collection<?> values) {
        return List.of(
                String.format("%d values?", values.size()),
                "");
    }
}
