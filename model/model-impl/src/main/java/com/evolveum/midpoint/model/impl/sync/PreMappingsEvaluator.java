/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.DefaultSingleShadowInboundsProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessingContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundProcessingDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluates "pre-mappings" i.e. inbound mappings that are evaluated before the actual clockwork is run.
 * (This is currently done to simplify the correlation process.)
 *
 * This tiny class serves as a bridge between the world of correlation and the world of mappings.
 */
public class PreMappingsEvaluator {

    static <F extends FocusType> void computePreFocus(
            @NotNull SingleShadowInboundsProcessingContext<F> ctx, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        SingleShadowInboundsProcessing.evaluate(ctx, result);
    }

    @VisibleForTesting
    public static <F extends FocusType> @NotNull F computePreFocus(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceObjectTypeDefinition objectTypeDefinition,
            @NotNull ResourceType resource,
            @NotNull Class<F> focusClass,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return SingleShadowInboundsProcessing.evaluate(
                new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        AbstractShadow.of(shadowedResourceObject),
                        resource,
                        new InboundMappingContextSpecification(
                                // We may reconsider if we shouldn't require type identification explicitly from the caller; but
                                // for now, it seems that the type definition is derived straight from the SynchronizationPolicy
                                objectTypeDefinition.getTypeIdentification(),
                                null,
                                shadowedResourceObject.getTag()),
                        PrismContext.get().createObjectable(focusClass),
                        ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                        task,
                        objectTypeDefinition,
                        objectTypeDefinition,
                        true),
                result);
    }

    /**
     * Note the `resourceObjectDefinition` is where we look for definitions of the mapped items; so for the trivial associations
     * it should be the definition of the _subject_.
     */
    public static <C extends Containerable> void computePreFocusForAssociationValue(
            @NotNull ShadowAssociationValue associationValue,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ResourceObjectInboundProcessingDefinition inboundDefinition,
            @NotNull ResourceType resource,
            @NotNull InboundMappingContextSpecification mappingContextSpecification,
            @NotNull C targetObject,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        SingleShadowInboundsProcessing.evaluate(
                new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        associationValue,
                        resource,
                        mappingContextSpecification,
                        targetObject,
                        ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                        task,
                        resourceObjectDefinition,
                        inboundDefinition,
                        true),
                result);
    }
}
