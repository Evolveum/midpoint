/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.CorrelatorDiscriminator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

public class ObjectTemplateTypeUtil {

    public static @Nullable CompositeCorrelatorType getCorrelators(
            @Nullable ObjectTemplateType template,
            @NotNull CorrelatorDiscriminator discriminator) throws ConfigurationException {
        if (template == null) {
            return null;
        }
        ObjectTemplateCorrelationType correlation = template.getCorrelation();
        List<CompositeCorrelatorType> correlators = correlation != null ? correlation.getCorrelators() : List.of();
        var matching = correlators.stream()
                .filter(discriminator::match)
                .toList();
        return MiscUtil.extractSingleton(
                matching,
                () -> new ConfigurationException("%d correlators matching %s in %s".formatted(
                        matching.size(), discriminator, template)));
    }

    public static @Nullable ObjectTemplateItemDefinitionType findItemDefinition(
            @NotNull ObjectTemplateType template, @NotNull ItemPath path) throws ConfigurationException {
        List<ObjectTemplateItemDefinitionType> definitions = new ArrayList<>();
        for (ObjectTemplateItemDefinitionType itemDefBean : template.getItem()) {
            if (ItemRefinedDefinitionTypeUtil.getRef(itemDefBean).equivalent(path)) {
                definitions.add(itemDefBean);
            }
        }
        if (definitions.size() > 1) {
            throw new ConfigurationException(
                    "Multiple (" + definitions.size() + ") definitions for '" + path + "' in " + template);
        } else {
            return MiscUtil.extractSingleton(definitions);
        }
    }
}
