/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class QueryConversionUtil {

    public static List<ObjectFilter> parseFilters(
            @NotNull List<SearchFilterType> filterBeans,
            @NotNull ResourceObjectDefinition definition) throws SchemaException {
        List<ObjectFilter> parsed = new ArrayList<>();
        for (SearchFilterType filterBean : filterBeans) {
            ObjectFilter parsedFilter = parseFilter(filterBean, definition);
            if (parsedFilter != null) {
                parsed.add(parsedFilter);
            }
        }
        return parsed;
    }

    public static @Nullable ObjectFilter parseFilter(
            @Nullable SearchFilterType filterBean, @NotNull ResourceObjectDefinition definition)
            throws SchemaException {
        if (filterBean == null) {
            return null;
        }

        var rawShadowDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        var preciseShadowDef = ShadowUtil.applyObjectDefinition(rawShadowDef, definition);

        // Note that we don't need to re-apply the definitions to the parsed filter, as the definitions are set during parsing.
        return PrismContext.get().getQueryConverter().createObjectFilter(preciseShadowDef, filterBean);
    }
}
