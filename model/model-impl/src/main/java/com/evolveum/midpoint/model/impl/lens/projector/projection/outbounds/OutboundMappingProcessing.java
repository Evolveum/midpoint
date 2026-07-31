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
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
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
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates outbound mapping for a single shadow, producing item deltas.
 *
 * Designed primarily for simulation scenarios where a specific mapping needs to be evaluated
 * against a given focus and shadow combination.
 */
@Component
public class OutboundMappingProcessing {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundMappingProcessing.class);

    private static final String OP_EVALUATE = OutboundMappingProcessing.class.getName() + ".evaluate";

    private final MappingFactory mappingFactory;
    private final MappingEvaluator evaluator;
    private final Clock clock;

    public OutboundMappingProcessing(MappingFactory mappingFactory, MappingEvaluator evaluator, Clock clock) {
        this.mappingFactory = mappingFactory;
        this.evaluator = evaluator;
        this.clock = clock;
    }

    /**
     * Evaluates the given outbound mapping and returns the resulting item deltas.
     *
     * @param targetItemPath the path of the target attribute on the shadow
     * @param mapping the mapping bean to evaluate
     * @param shadow the shadow object
     * @param resource the resource where the shadow resides
     * @param focus the focus object providing source values
     * @param systemConfiguration the optional system configuration. It is exposed to expressions as a variable.
     * @return collection of item deltas produced by the mapping evaluation
     */
    public Collection<ItemDelta<?, ?>> executeToDeltas(
            ItemPath targetItemPath,
            MappingType mapping,
            ShadowType shadow,
            ResourceType resource,
            FocusType focus,
            @Nullable SystemConfigurationType systemConfiguration,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
                   SecurityViolationException, ConfigurationException, ObjectNotFoundException,
                   SubscriptionComplianceException {

        final OperationResult evaluationResult = result.subresult(OP_EVALUATE)
                .addParam("target", targetItemPath.toString())
                .addParam("mapping", mapping.getName() != null ? mapping.getName() : "")
                .addParam("shadow", shadow.getName() != null ? shadow.getName().toString() : "")
                .build();

        try {
            LOGGER.trace("Starting evaluation of {} outbound mapping on shadow {}", targetItemPath, shadow.getName());

            final ResourceObjectDefinition resourceObjectDefinition = getResourceObjectDefinition(resource, shadow);
            final ItemDefinition<?> targetDefinition = resourceObjectDefinition.findItemDefinition(targetItemPath);
            if (targetDefinition == null) {
                throw new ConfigurationException("Unable to find item definition for path " + targetItemPath +
                        " in resource " + resource.getName());
            }

            final DeltaSetTripleIvwoMap tripleMap = evaluateMappingToTripleMap(
                    targetItemPath, mapping, shadow, resource, focus, systemConfiguration, task, targetDefinition,
                    evaluationResult);

            final Collection<ItemDelta<?, ?>> deltas = consolidateToDeltas(tripleMap, shadow, targetDefinition,
                    task, evaluationResult);

            LOGGER.debug("Outbound mapping evaluation for {} produced {} delta(s)", targetItemPath, deltas.size());
            LOGGER.trace("Computed deltas:\n{}", DebugUtil.debugDumpLazily(deltas, 1));

            return deltas;

        } catch (Throwable t) {
            evaluationResult.recordFatalError(t);
            throw t;
        } finally {
            evaluationResult.close();
        }
    }

    /**
     * Evaluates the mapping and populates the triple map with the output.
     */
    private DeltaSetTripleIvwoMap evaluateMappingToTripleMap(
            ItemPath targetItemPath,
            MappingType mapping,
            ShadowType shadow,
            ResourceType resource,
            FocusType focus,
            @Nullable SystemConfigurationType systemConfiguration,
            Task task,
            ItemDefinition<?> targetDefinition,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
                   SecurityViolationException, ConfigurationException, ObjectNotFoundException,
                   SubscriptionComplianceException {

        final String contextDescription = "outbound mapping simulation for " + targetItemPath +
                " on " + shadow.getName();
        final MappingConfigItem mappingCI = MappingConfigItem.of(mapping, ConfigurationItemOrigin.embedded(mapping));
        final MappingBuilder<PrismValue, ItemDefinition<?>> builder = this.mappingFactory.createMappingBuilder(
                mappingCI, contextDescription);

        @SuppressWarnings("unchecked")
        final PrismObject<FocusType> focusObject = (PrismObject<FocusType>) focus.asPrismObject();
        final ObjectDeltaObject<FocusType> focusOdo = ObjectDeltaObject.forUnchanged(focusObject);

        configureFocusSources(builder, focusOdo);
        configureTarget(builder, targetItemPath, targetDefinition);
        configureShadowVariables(builder, shadow);
        configureAdditionalVariables(builder, resource, systemConfiguration);

        final MappingImpl<PrismValue, ItemDefinition<?>> mappingImpl = builder.build();

        LOGGER.trace("Evaluating mapping: {}", mappingImpl.getMappingContextDescription());
        this.evaluator.evaluateMapping(mappingImpl, MappingEvaluator.EvaluationContext.empty(), task, result);

        final DeltaSetTripleIvwoMap tripleMap = new DeltaSetTripleIvwoMap();
        tripleMap.putOrMerge(ShadowType.F_ATTRIBUTES.append(targetItemPath), ItemValueWithOrigin.createOutputTriple(mappingImpl));

        return tripleMap;
    }

    /**
     * Configures the builder with focus object as the default source context.
     */
    private void configureFocusSources(MappingBuilder<PrismValue, ItemDefinition<?>> builder,
            ObjectDeltaObject<FocusType> focusOdo) {
        builder.defaultSourceContextIdi(focusOdo);
        builder.addRootVariableDefinition(focusOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
        builder.addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);
    }

    /**
     * Configures the builder with the target item path and definition.
     */
    private void configureTarget(MappingBuilder<PrismValue, ItemDefinition<?>> builder, ItemPath targetItemPath,
            ItemDefinition<?> targetDefinition) {
        builder.defaultTargetDefinition(targetDefinition);
        builder.defaultTargetPath(targetItemPath);

        final ItemName lastName = targetItemPath.lastName();
        if (lastName != null) {
            builder.targetItemName(lastName);
        }
    }

    /**
     * Configures the builder with shadow (account) variables.
     */
    private void configureShadowVariables(MappingBuilder<PrismValue, ItemDefinition<?>> builder, ShadowType shadow) {
        final PrismObject<ShadowType> shadowObject = shadow.asPrismObject();
        final ObjectDeltaObject<ShadowType> shadowOdo = new ObjectDeltaObject<>(shadowObject, null, null,
                shadowObject.getDefinition());

        builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadowOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadowOdo);
        builder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadowOdo);
        builder.addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION);
        builder.addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION);
    }

    /**
     * Configures the builder with additional variables (resource, system configuration).
     */
    private void configureAdditionalVariables(MappingBuilder<PrismValue, ItemDefinition<?>> builder,
            ResourceType resource, @Nullable SystemConfigurationType systemConfiguration) {
        builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource, ResourceType.class);
        builder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, systemConfiguration,
                SystemConfigurationType.class);
    }

    /**
     * Consolidates the triple map into item deltas.
     */
    private Collection<ItemDelta<?, ?>> consolidateToDeltas(DeltaSetTripleIvwoMap tripleMap, ShadowType shadow,
            ItemDefinition<?> targetDefinition, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        final MappingEvaluationEnvironment evaluationEnvironment = new MappingEvaluationEnvironment(
                "consolidating outbound processing of " + shadow, this.clock.currentTimeXMLGregorianCalendar(), task);

        final DeltaSetTripleMapConsolidation<ShadowType> consolidation = new DeltaSetTripleMapConsolidation<>(
                tripleMap,
                shadow.asPrismObject().getValue(),
                DeltaSetTripleMapConsolidation.APrioriDeltaProvider.none(),
                path -> false,
                true,
                consolidationBuilder -> consolidationBuilder.deleteExistingValues(true),
                targetPath -> targetDefinition,
                evaluationEnvironment,
                null,
                result);

        consolidation.computeItemDeltas();
        return consolidation.getItemDeltas();
    }

    private ResourceObjectDefinition getResourceObjectDefinition(ResourceType resource, ShadowType shadow)
            throws ConfigurationException, SchemaException {
        final ResourceSchema schema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        final ResourceObjectTypeIdentification objectTypeId = ResourceObjectTypeIdentification.of(
                ShadowUtil.resolveDefault(shadow.getKind()),
                ShadowUtil.resolveDefault(shadow.getIntent()));
        return schema.getObjectTypeDefinitionRequired(objectTypeId);
    }
}
