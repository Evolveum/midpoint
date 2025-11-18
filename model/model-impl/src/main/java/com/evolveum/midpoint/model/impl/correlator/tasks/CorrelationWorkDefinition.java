/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import com.evolveum.midpoint.model.impl.correlator.tasks.CorrelationDefinitionProvider.ResourceWithObjectTypeId;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Work definition for correlation simulation activity.
 */
public class CorrelationWorkDefinition extends ResourceSetTaskWorkDefinition {

    private final CorrelatorsDefinitionType correlatorsToUse;
    private final CorrelationDefinitionProviderFactory correlationDefProviderFactory;

    public CorrelationWorkDefinition(WorkDefinitionFactory.WorkDefinitionInfo info,
            CorrelationDefinitionProviderFactory correlationDefProviderFactory) {
        super(info);

        final AbstractWorkDefinitionType workDefBean = info.getBean();
        if (!(workDefBean instanceof final CorrelationWorkDefinitionType workDef)) {
            throw new IllegalArgumentException("Expected " + CorrelationWorkDefinitionType.class.getSimpleName()
                    + " but got: " + workDefBean.getClass());
        }

        this.correlatorsToUse = workDef.getCorrelators();
        this.correlationDefProviderFactory = correlationDefProviderFactory;
    }

    public CorrelationDefinitionType provideCorrelators(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        final ResourceWithObjectTypeId resourceWithObjectType = ResourceWithObjectTypeId.from(
                getResourceObjectSetSpecification());
        return this.correlationDefProviderFactory.providerFor(this.correlatorsToUse, resourceWithObjectType).get(result);
    }

}
