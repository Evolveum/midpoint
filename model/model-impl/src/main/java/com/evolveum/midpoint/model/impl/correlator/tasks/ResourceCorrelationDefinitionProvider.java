/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import java.util.Optional;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ResourceCorrelationDefinitionProvider implements CorrelationDefinitionProvider {
    private final RepositoryService repository;
    private final ResourceWithObjectTypeId resourceWithObjectTypeId;

    public ResourceCorrelationDefinitionProvider(RepositoryService repository,
            ResourceWithObjectTypeId resourceWithObjectTypeId) {
        this.repository = repository;
        this.resourceWithObjectTypeId = resourceWithObjectTypeId;
    }

    @Override
    public CorrelationDefinitionType get(OperationResult result) throws SchemaException, ObjectNotFoundException,
            ConfigurationException {
        final PrismObject<ResourceType> resource;
        resource = this.repository.getObject(ResourceType.class,
                this.resourceWithObjectTypeId.oid(),
                SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), result);
        return combineCorrelationsSources(resource.asObjectable());
    }

    private CorrelationDefinitionType combineCorrelationsSources(ResourceType resource)
            throws SchemaException, ConfigurationException {

        final ResourceObjectTypeIdentification objectTypeId = ResourceObjectTypeIdentification.of(
                this.resourceWithObjectTypeId.kind(), this.resourceWithObjectTypeId.intent());
        final ResourceObjectTypeDefinition objectTypeDefinition = Resource.of(resource)
                .getCompleteSchemaRequired().getObjectTypeDefinitionRequired(objectTypeId);

        final ObjectSynchronizationType objectSynchronization =
                Optional.ofNullable(resource.getSynchronization())
                        .flatMap(synchronization -> synchronization.getObjectSynchronization()
                                .stream()
                                .filter(this::matchKindAndIntent)
                                .findFirst())
                        .orElse(null);

        return CorrelatorsDefinitionUtil.mergeCorrelationDefinition(objectTypeDefinition, objectSynchronization,
                resource);
    }

    private boolean matchKindAndIntent(ObjectSynchronizationType synchronizationType) {
        return this.resourceWithObjectTypeId.kind().equals(synchronizationType.getKind())
                && this.resourceWithObjectTypeId.intent().equals(synchronizationType.getIntent());
    }

}
