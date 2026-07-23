/*
 * Copyright (C) 2026 Evolveum and contributors
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Correlation definition provider factory for correlators simulations.
 *
 * The factory creates a provider, which based on the data in the provider definition holder creates a provider which
 * does either of:
 *
 * * Reads the definition from the resource
 * * Reads the definition which is in the holder directly
 * * Reads both of the above and merges them to one definition.
 */
@Component
public final class CorrelationDefinitionProviderForSimulationFactory implements
        CorrelationDefinitionProviderFactory<CorrelationDefinitionProviderForSimulationFactory.SimulatedCorrelatorsSpec> {

    private final RepositoryService repository;

    public CorrelationDefinitionProviderForSimulationFactory(
            @Qualifier("cacheRepositoryService") RepositoryService repository) {
        this.repository = repository;
    }

    @Override public CorrelationDefinitionProvider providerFor(SimulatedCorrelatorsSpec definitionRetrievalSpec,
            OperationResult result) {

        final SimulatedCorrelatorsType simulatedCorrelators = definitionRetrievalSpec.correlators();
        if (Boolean.TRUE == simulatedCorrelators.isIncludeExistingCorrelators()
                && simulatedCorrelators.getInlineCorrelators() != null) {
            return new ResourceCorrelationDefinitionProvider(
                    readResource(definitionRetrievalSpec.resourceOid(), result))
                    .union(inlineCorrelationProvider(simulatedCorrelators));
        }
        if (Boolean.TRUE == simulatedCorrelators.isIncludeExistingCorrelators()) {
            return new ResourceCorrelationDefinitionProvider(
                    readResource(definitionRetrievalSpec.resourceOid(), result));
        }
        if (simulatedCorrelators.getInlineCorrelators() != null) {
            return inlineCorrelationProvider(simulatedCorrelators);
        }

        throw new IllegalArgumentException("No source of correlations were specified: " + definitionRetrievalSpec);
    }

    private static CorrelationDefinitionProvider inlineCorrelationProvider(
            SimulatedCorrelatorsType correlatorDefinition) {
        return objectTypeId -> correlatorDefinition.getInlineCorrelators();
    }

    private ResourceType readResource(String oid, OperationResult result) {
        final Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                GetOperationOptions.createNoFetch());
        try {
            return this.repository.getObject(ResourceType.class, oid, options, result)
                    .asObjectable();
        } catch (ObjectNotFoundException | SchemaException e) {
            throw SystemException.unexpected(e, "Unable to read the resource with oid " + oid);
        }
    }

    public record SimulatedCorrelatorsSpec(SimulatedCorrelatorsType correlators, String resourceOid) { }

}
