/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import org.jetbrains.annotations.NotNull;

/**
 * Work definition marker for mappings suggestions.
 *
 * It extends {@link ObjectTypeRelatedSuggestionWorkDefinition} but has its own class
 * so the activity handler registry can distinguish it from the correlation variant.
 */
public class MappingsSuggestionWorkDefinition extends ObjectTypeRelatedSuggestionWorkDefinition {

    MappingsSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
    }
}
