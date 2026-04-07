/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CorrelationDefinitionProvider;
import com.evolveum.midpoint.model.impl.correlation.ResourceCorrelationDefinitionProvider;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public final class CorrelationDefinitionProviderFactory {

    private final RepositoryService repository;

    public CorrelationDefinitionProviderFactory(@Qualifier("cacheRepositoryService") RepositoryService repository) {
        this.repository = repository;
    }

    public CorrelationDefinitionProvider providerFor(SimulatedCorrelatorsType correlatorSpecification,
            ResourceWithObjectTypeId resourceWithObjectTypeId, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        final ResourceObjectTypeIdentification objectTypeId = ResourceObjectTypeIdentification.of(
                resourceWithObjectTypeId.kind(), resourceWithObjectTypeId.intent());

        if (Boolean.TRUE == correlatorSpecification.isIncludeExistingCorrelators()
                && correlatorSpecification.getInlineCorrelators() != null) {
            return new ResourceCorrelationDefinitionProvider(readResource(resourceWithObjectTypeId.oid(), result),
                    objectTypeId).union(inlineCorrelationProvider(correlatorSpecification));
        }
        if (Boolean.TRUE == correlatorSpecification.isIncludeExistingCorrelators()) {
            return new ResourceCorrelationDefinitionProvider(readResource(resourceWithObjectTypeId.oid(), result),
                    objectTypeId);
        }
        if (correlatorSpecification.getInlineCorrelators() != null) {
            return inlineCorrelationProvider(correlatorSpecification);
        }

        throw new IllegalArgumentException("No source of correlations were specified: " + correlatorSpecification);
    }

    private static CorrelationDefinitionProvider inlineCorrelationProvider(
            SimulatedCorrelatorsType correlatorDefinition) {
        return correlatorDefinition::getInlineCorrelators;
    }

    private ResourceType readResource(String oid, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        final Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                GetOperationOptions.createNoFetch());
        return this.repository.getObject(ResourceType.class, oid, options, result)
                .asObjectable();
    }

    public record ResourceWithObjectTypeId(String oid, ShadowKindType kind, String intent) {
        public static ResourceWithObjectTypeId from(ResourceObjectSetType resourceObjectSet) {
            return new ResourceWithObjectTypeId(resourceObjectSet.getResourceRef().getOid(),
                    resourceObjectSet.getKind(), resourceObjectSet.getIntent());
        }
    }

}
