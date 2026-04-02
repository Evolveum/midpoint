/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlation;

import java.util.Objects;
import java.util.Optional;

import com.evolveum.midpoint.model.api.correlation.CorrelationDefinitionProvider;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Provides correlation definition for a specific resource object type.
 *
 * This implementation of {@link CorrelationDefinitionProvider} resolves the correlation definition
 * by merging information from two sources:
 *
 * . The resource object type definition from the complete schema (including short - attribute bound - form).
 * . The object synchronization configuration from the resource's synchronization settings
 *
 * @see CorrelationDefinitionProvider
 */
public class ResourceCorrelationDefinitionProvider implements CorrelationDefinitionProvider {
    private final ResourceType resource;
    private final ResourceObjectTypeIdentification objectTypeId;

    public ResourceCorrelationDefinitionProvider(ResourceType resource, ResourceObjectTypeIdentification objectTypeId) {
        this.resource = resource;
        this.objectTypeId = objectTypeId;
    }

    @Override
    public CorrelationDefinitionType get() throws SchemaException, ObjectNotFoundException, ConfigurationException {
        final ResourceObjectTypeDefinition objectTypeDefinition = Resource.of(resource)
                .getCompleteSchemaRequired().getObjectTypeDefinitionRequired(this.objectTypeId);

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
        return Objects.equals(this.objectTypeId.getKind(), ShadowUtil.resolveDefault(synchronizationType.getKind()))
                && Objects.equals(this.objectTypeId.getIntent(),
                        ShadowUtil.resolveDefault(synchronizationType.getIntent()));
    }

}
