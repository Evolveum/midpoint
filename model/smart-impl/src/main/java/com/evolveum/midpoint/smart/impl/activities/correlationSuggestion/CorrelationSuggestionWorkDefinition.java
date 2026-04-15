/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.correlationSuggestion;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.smart.impl.activities.ObjectTypeRelatedSuggestionWorkDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Work definition marker for correlation suggestions.
 *
 * It extends {@link ObjectTypeRelatedSuggestionWorkDefinition} but has its own class
 * so the activity handler registry can distinguish it from the mappings variant.
 */
public class CorrelationSuggestionWorkDefinition extends ObjectTypeRelatedSuggestionWorkDefinition {

    private final List<ItemPath> targetPathsToIgnore;
    private final List<DataAccessPermissionType> permissions;

    CorrelationSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var workDefinition = (CorrelationSuggestionWorkDefinitionType) info.getBean();
        this.targetPathsToIgnore = getTargetPathsToIgnore(workDefinition);
        this.permissions = workDefinition.getPermissions();
    }

    /**
     * Extracts target paths to ignore from the work definition.
     */
    private static @NotNull List<ItemPath> getTargetPathsToIgnore(@NotNull CorrelationSuggestionWorkDefinitionType workDefinition) {
        return workDefinition.getTargetPathsToIgnore() == null
                ? List.of()
                : workDefinition.getTargetPathsToIgnore().stream()
                .filter(Objects::nonNull)
                .map(ItemPathType::getItemPath)
                .toList();
    }

    public List<ItemPath> getTargetPathsToIgnore() {
        return targetPathsToIgnore;
    }

    public List<DataAccessPermissionType> getPermissions() {
        return this.permissions;
    }
}
