/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlation;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlation.CorrelationDefinitionProvider;
import com.evolveum.midpoint.model.impl.correlator.tasks.CorrelationDefinitionProviderFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ResourceCorrelationDefinitionProviderFactory
        implements CorrelationDefinitionProviderFactory<ResourceType> {

    @Override
    public CorrelationDefinitionProvider providerFor(ResourceType resource, OperationResult result) {
        return new ResourceCorrelationDefinitionProvider(resource);
    }

}
