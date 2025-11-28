/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import java.util.Objects;

/**
 * Holds all the data needed to execute an operation on a specific resource and object type.
 *
 * @see CorrelationSuggestionOperation
 * @see MappingsSuggestionOperation
 */
class TypeOperationContext extends OperationContext {

    final ResourceObjectTypeDefinition typeDefinition;

    private TypeOperationContext(
            ResourceType resource,
            ResourceSchema resourceSchema,
            ResourceObjectTypeDefinition typeDefinition,
            ServiceClient serviceClient,
            @Nullable CurrentActivityState<?> activityState,
            Task task) {
        super(resource, resourceSchema, typeDefinition.getObjectClassDefinition(), serviceClient, activityState, task);
        this.typeDefinition = typeDefinition;
    }

    static TypeOperationContext init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var resource = SmartIntegrationBeans.get().modelService
                .getObject(ResourceType.class, resourceOid, null, task, result)
                .asObjectable();
        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        var typeDefinition = resourceSchema.getObjectTypeDefinitionRequired(typeIdentification);
        return new TypeOperationContext(resource, resourceSchema, typeDefinition, serviceClient, activityState, task);
    }

    PrismObjectDefinition<?> getFocusTypeDefinition() {
        var focusTypeName = getFocusTypeName();
        return MiscUtil.argNonNull(
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(focusTypeName),
                "Focus type definition not found for %s", focusTypeName);
    }

    private QName getFocusTypeName() {
        return MiscUtil.argNonNull(
                typeDefinition.getFocusTypeName(),
                "Focus type not defined for %s", typeDefinition.getTypeIdentification());
    }

    PrismObjectDefinition<ShadowType> getShadowDefinition() {
        return typeDefinition.getPrismObjectDefinition();
    }

    <F extends FocusType> Class<F> getFocusClass() {
        //noinspection unchecked
        return Objects.requireNonNull((Class <F>) getFocusTypeDefinition().getTypeClass());
    }

    public ResourceObjectTypeIdentification getTypeIdentification() {
        return typeDefinition.getTypeIdentification();
    }
}
