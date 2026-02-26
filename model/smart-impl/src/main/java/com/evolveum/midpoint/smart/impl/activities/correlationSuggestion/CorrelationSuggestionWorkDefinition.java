/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.correlationSuggestion;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.smart.impl.activities.ObjectTypeRelatedSuggestionWorkDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import org.jetbrains.annotations.NotNull;

/**
 * Work definition marker for correlation suggestions.
 *
 * It extends {@link ObjectTypeRelatedSuggestionWorkDefinition} but has its own class
 * so the activity handler registry can distinguish it from the mappings variant.
 */
public class CorrelationSuggestionWorkDefinition extends ObjectTypeRelatedSuggestionWorkDefinition {

    CorrelationSuggestionWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
    }
}
