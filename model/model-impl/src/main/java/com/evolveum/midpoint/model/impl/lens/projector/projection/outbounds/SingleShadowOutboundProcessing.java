/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.lens.projector.projection.outbounds;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.RestrictedObjectException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * !!!! BEWARE, TOTALLY WIP THING, NOT FOR PRODUCTION AT ALL. IF YOU SEE IT IN MASTER, CALL 911 !!!!
 *
 * Outbound oriented counterpart of the
 * {@link com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing} just much
 * simpler and created mainly with simulations in mind.
 */
@Component
public class SingleShadowOutboundProcessing {

    private static final Trace LOGGER = TraceManager.getTrace(SingleShadowOutboundProcessing.class);

    private final MappingFactory mappingFactory;
    private final MappingEvaluator evaluator;
    private final Clock clock;

    public SingleShadowOutboundProcessing(MappingFactory mappingFactory, MappingEvaluator evaluator, Clock clock) {
        this.mappingFactory = mappingFactory;
        this.evaluator = evaluator;
        this.clock = clock;
    }

    public Collection<ItemDelta<?, ?>> executeToDeltas(
            ItemPath targetItemPath,
            MappingType mapping,
            ShadowType shadow,
            ResourceType resource,
            FocusType focus,
            @Nullable SystemConfigurationType systemConfiguration,
            Task task,
            OperationResult result)
            throws CommonException {

        final ResourceObjectDefinition resourceObjectDefinition = getResourceObjectDefinition(resource, shadow);

        final ItemDefinition<?> targetDefinition = resourceObjectDefinition.findItemDefinition(targetItemPath);
        if (targetDefinition == null) {
            throw new ConfigurationException("Unable to find definition of the target attribute " + targetItemPath);
        }

        final String contextDescription = "outbound mapping simulation for " + targetItemPath + " on " + shadow.getName();
        final MappingConfigItem mappingCI = MappingConfigItem.of(mapping, ConfigurationItemOrigin.embedded(mapping));
        final MappingBuilder<PrismValue, ItemDefinition<?>> builder = mappingFactory.createMappingBuilder(mappingCI,
                contextDescription);
        @SuppressWarnings("unchecked")
        final PrismObject<FocusType> focusObject = (PrismObject<FocusType>) focus.asPrismObject();
        final ObjectDeltaObject<FocusType> focusOdo = ObjectDeltaObject.forUnchanged(focusObject);

        builder.defaultSourceContextIdi(focusOdo);
        builder.addRootVariableDefinition(focusOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
        builder.addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

        builder.defaultTargetDefinition(targetDefinition);
        builder.defaultTargetPath(targetItemPath);

        final ItemName lastName = targetItemPath.lastName();
        if (lastName != null) {
            builder.targetItemName(lastName);
        }

        final PrismObject<ShadowType> shadowObject = shadow.asPrismObject();
        final ObjectDeltaObject<ShadowType> shadowOdo = new ObjectDeltaObject<>(shadowObject, null, null,
                shadowObject.getDefinition());
        builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadowOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadowOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadowOdo);
        builder.addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION);
        builder.addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION);

        builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);
        builder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, systemConfiguration,
                SystemConfigurationType.class);

        builder.originType(OriginType.OUTBOUND);
        builder.originObject(resource);
        builder.mappingKind(MappingKindType.OUTBOUND);
        builder.now(this.clock.currentTimeXMLGregorianCalendar());

        final MappingImpl<PrismValue, ItemDefinition<?>> mappingImpl = builder.build();

        this.evaluator.evaluateMapping(mappingImpl, MappingEvaluator.EvaluationContext.empty(), task, result);

        final DeltaSetTripleIvwoMap tripleMap = new DeltaSetTripleIvwoMap();
        tripleMap.putOrMerge(targetItemPath, ItemValueWithOrigin.createOutputTriple(mappingImpl));
        return consolidateToDeltas(tripleMap, shadow, targetDefinition, task, result);
    }

    private ResourceObjectDefinition getResourceObjectDefinition(ResourceType resource, ShadowType shadow)
            throws ConfigurationException, SchemaException {
        final ResourceSchema schema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        final ResourceObjectTypeIdentification objectTypeId =  ResourceObjectTypeIdentification.of(
                ShadowUtil.resolveDefault(shadow.getKind()),
                ShadowUtil.resolveDefault(shadow.getIntent()));
        return schema.getObjectTypeDefinitionRequired(objectTypeId);
    }

    private Collection<ItemDelta<?, ?>> consolidateToDeltas(
            DeltaSetTripleIvwoMap tripleMap,
            ShadowType target,
            ItemDefinition<?> targetDefinition,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, RestrictedObjectException {

        final MappingEvaluationEnvironment evaluationEnvironment = new MappingEvaluationEnvironment(
                "simulating inbounds processing of " + target,
                ModelBeans.get().clock.currentTimeXMLGregorianCalendar(), task);

        final DeltaSetTripleMapConsolidation<ShadowType> consolidation = new DeltaSetTripleMapConsolidation<>(
                tripleMap,
                target.asPrismObject().getValue(),
                DeltaSetTripleMapConsolidation.APrioriDeltaProvider.none(),
                path -> false,
                true,
                consolidationBuilder -> {},
                targetPath -> targetDefinition,
                evaluationEnvironment,
                null,
                result);
        consolidation.computeItemDeltas();
        return consolidation.getItemDeltas();
    }
}
