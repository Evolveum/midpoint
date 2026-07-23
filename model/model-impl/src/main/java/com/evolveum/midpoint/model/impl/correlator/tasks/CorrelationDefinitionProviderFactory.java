/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import com.evolveum.midpoint.model.api.correlation.CorrelationDefinitionProvider;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Factory for creating {@link CorrelationDefinitionProvider} instances.
 *
 * @param <T> The type of the correlation definition holder.
 */
public interface CorrelationDefinitionProviderFactory<T> {

    /**
     * Creates a correlation definition provider for a given definition holder.
     *
     * NOTE: The structure and content of the {@code definitionRetrievalSpec} parameter type is completely up to the
     * implementation of the provider. It may even contain the correlation definitions directly, or it may just hold
     * data necessary to retrieve the definitions from elsewhere.
     *
     * @param definitionRetrievalSpec The object containing necessary information for the provider to get the definitions.
     * @return The new instance of a definition provider.
     */
    CorrelationDefinitionProvider providerFor(T definitionRetrievalSpec, OperationResult result);

}
