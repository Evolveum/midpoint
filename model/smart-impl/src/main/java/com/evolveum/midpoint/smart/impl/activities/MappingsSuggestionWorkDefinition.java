/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionWorkDefinitionType;

import java.util.List;
import java.util.Objects;

/**
 * Work definition marker for mappings suggestions.
 * <p>
 * It extends {@link ObjectTypeRelatedSuggestionWorkDefinition} but has its own class
 * so the activity handler registry can distinguish it from the correlation variant.
 */
public class MappingsSuggestionWorkDefinition extends ObjectTypeRelatedSuggestionWorkDefinition {

    private final boolean isInbound;
    private final List<ItemPath> targetPathsToIgnore;

    MappingsSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var workDefinition = (MappingsSuggestionWorkDefinitionType) info.getBean();
        this.isInbound = workDefinition.isInbound();
        this.targetPathsToIgnore = getTargetPathsToIgnore(workDefinition);
    }

    /**
     * Extracts target paths to ignore from the work definition.
     */
    private static @NotNull List<ItemPath> getTargetPathsToIgnore(@NotNull MappingsSuggestionWorkDefinitionType workDefinition) {
        return workDefinition.getTargetPathsToIgnore() == null
                ? List.of()
                : workDefinition.getTargetPathsToIgnore().stream()
                .filter(Objects::nonNull)
                .map(ItemPathType::getItemPath)
                .toList();
    }

    public boolean isInbound() {
        return isInbound;
    }

    public List<ItemPath> getTargetPathsToIgnore() {
        return targetPathsToIgnore;
    }
}
