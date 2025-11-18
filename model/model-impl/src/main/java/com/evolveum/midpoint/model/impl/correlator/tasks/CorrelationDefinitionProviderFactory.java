/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.correlator.tasks.CorrelationDefinitionProvider.ResourceWithObjectTypeId;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public final class CorrelationDefinitionProviderFactory {

    @Qualifier("cacheRepositoryService")
    private final RepositoryService repository;

    public CorrelationDefinitionProviderFactory(RepositoryService repository) {
        this.repository = repository;
    }

    public CorrelationDefinitionProvider providerFor(CorrelatorsDefinitionType correlatorSpecification,
            ResourceWithObjectTypeId resourceWithObjectTypeId) {
        if (Boolean.TRUE == correlatorSpecification.isIncludeExistingCorrelators()
                && correlatorSpecification.getInlineCorrelators() != null) {
            return new ResourceCorrelationDefinitionProvider(this.repository, resourceWithObjectTypeId)
                    .union(inlineCorrelationProvider(correlatorSpecification));
        }
        if (Boolean.TRUE == correlatorSpecification.isIncludeExistingCorrelators()) {
            return new ResourceCorrelationDefinitionProvider(this.repository, resourceWithObjectTypeId);
        }
        if (correlatorSpecification.getInlineCorrelators() != null) {
            return inlineCorrelationProvider(correlatorSpecification);
        }

        throw new IllegalArgumentException("No source of correlations were specified: " + correlatorSpecification);
    }

    private static CorrelationDefinitionProvider inlineCorrelationProvider(
            CorrelatorsDefinitionType correlatorDefinition) {
        return result -> correlatorDefinition.getInlineCorrelators();
    }

}
