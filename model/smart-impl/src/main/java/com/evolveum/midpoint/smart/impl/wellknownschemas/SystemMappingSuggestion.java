/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.Nullable;

/**
 * System-provided mapping suggestion containing the data needed to construct a mapping.
 */
public record SystemMappingSuggestion(
        ItemPath shadowAttributePath,
        ItemPath focusPropertyPath,
        @Nullable ExpressionType expression,
        MappingStrengthType strength) {

    /**
     * Creates a simple as-is mapping suggestion without any script transformation.
     */
    public static SystemMappingSuggestion createAsIsSuggestion(
            String shadowAttrName,
            ItemPath focusPropertyPath) {
        return new SystemMappingSuggestion(
                ItemPath.create(ShadowType.F_ATTRIBUTES, shadowAttrName),
                focusPropertyPath,
                null,
                MappingStrengthType.STRONG);
    }

    /**
     * Creates a simple as-is mapping suggestion with specified strength.
     */
    public static SystemMappingSuggestion createAsIsSuggestion(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            MappingStrengthType strength) {
        return new SystemMappingSuggestion(
                ItemPath.create(ShadowType.F_ATTRIBUTES, shadowAttrName),
                focusPropertyPath,
                null,
                strength);
    }

    /**
     * Creates mapping suggestion with a transformation script and specified strength.
     */
    public static SystemMappingSuggestion createScriptSuggestion(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            String script,
            @Nullable String scriptDescription,
            MappingStrengthType strength) {
        ExpressionType expression = new ExpressionType()
                .description(scriptDescription)
                .expressionEvaluator(
                        new ObjectFactory().createScript(
                                new ScriptExpressionEvaluatorType().code(script)));
        return new SystemMappingSuggestion(
                ItemPath.create(ShadowType.F_ATTRIBUTES, shadowAttrName),
                focusPropertyPath,
                expression,
                strength);
    }
}
