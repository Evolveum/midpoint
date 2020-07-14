/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.FocalMappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.TargetObjectSpecification;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTripleUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getAprioriItemDelta;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

/**
 * Evaluates a set of mappings. This includes considering their dependencies (chaining).
 */
class MappingSetEvaluation<F extends AssignmentHolderType, T extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingSetEvaluation.class);

    private static final List<String> FOCUS_VARIABLE_NAMES = Arrays.asList(ExpressionConstants.VAR_FOCUS, ExpressionConstants.VAR_USER);

    private final ModelBeans beans;

    private final LensContext<F> context;

    /**
     * Prepared evaluation requests: filtered by phase and (optionally) sorted on dependencies.
     */
    private final List<? extends FocalMappingEvaluationRequest<?, ?>> requests;

    private final ObjectDeltaObject<F> focusOdo;

    /**
     * How's the target object determined? Currently it's always a fixed object but we can consider
     * updating it dynamically as mappings are progressively evaluated.
     */
    private final TargetObjectSpecification<T> targetSpecification;

    /**
     * Are we doing mapping chaining? It is not necessary if the target object is different from the source one.
     */
    private final boolean doingChaining;

    private final PathKeyedMap<DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap = new PathKeyedMap<>();

    /**
     * Customizes triples produced by mappings before they are aggregated into overall triple map.
     */
    private final TripleCustomizer<PrismValue, ItemDefinition> tripleCustomizer;

    /**
     * Receives each mapping as it's created and evaluated.
     */
    private final EvaluatedMappingConsumer<PrismValue, ItemDefinition> mappingConsumer;

    private final int iteration;

    private final String iterationToken;

    private final MappingEvaluationEnvironment env;

    private final OperationResult result;

    private NextRecompute nextRecompute;

    MappingSetEvaluation(MappingSetEvaluationBuilder<F, T> builder) {
        beans = builder.beans;
        context = builder.context;
        focusOdo = builder.focusOdo;
        targetSpecification = builder.targetSpecification;
        tripleCustomizer = builder.tripleCustomizer;
        mappingConsumer = builder.mappingConsumer;
        iteration = builder.iteration;
        iterationToken = builder.iterationToken;
        env = builder.env;
        result = builder.result;

        doingChaining = targetSpecification.isSameAsSource();

        MappingSorter sorter = new MappingSorter(beans);
        requests = doingChaining ?
                sorter.filterAndSort(builder.evaluationRequests, builder.phase) :
                sorter.filter(builder.evaluationRequests, builder.phase);
    }

    void evaluateMappingsToTriples() throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        for (FocalMappingEvaluationRequest<?, ?> request : requests) {
            evaluateMappingRequest(request);
        }
    }

    private void evaluateMappingRequest(FocalMappingEvaluationRequest<?, ?> request)
            throws ExpressionEvaluationException, PolicyViolationException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException {

        MappingImpl<PrismValue, ItemDefinition> mapping = createMapping(request);
        if (mapping == null) {
            return;
        }

        beans.mappingEvaluator.evaluateMapping(mapping, context, env.task, result);

        // We need to update nextRecompute even for mappings with "time valid" state.
        nextRecompute = NextRecompute.update(mapping, nextRecompute);

        if (!mapping.isTimeConstraintValid()) {
            return;
        }

        if (mappingConsumer != null) {
            mappingConsumer.accept(mapping, request);
        }
        updateOutputTripleMap(mapping, request);
    }

    private MappingImpl<PrismValue, ItemDefinition> createMapping(FocalMappingEvaluationRequest<?, ?> request)
            throws ExpressionEvaluationException, PolicyViolationException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException {
        String description = request.shortDump();
        LOGGER.trace("Starting evaluation of {}", description);

        ObjectDeltaObject<F> updatedFocusOdo = doingChaining ?
                getUpdatedFocusOdo(context, focusOdo, outputTripleMap, request, description, result) :
                focusOdo;

        PrismObject<T> targetObject = targetSpecification.getTargetObject();

        return beans.mappingEvaluator.createFocusMapping(context,
                request, updatedFocusOdo, targetObject, iteration, iterationToken,
                context.getSystemConfiguration(), env.now, description, env.task, result);
    }

    private void updateOutputTripleMap(MappingImpl<PrismValue, ItemDefinition> mapping, FocalMappingEvaluationRequest<?, ?> request) {
        ItemPath outputPath = mapping.getOutputPath();
        if (outputPath != null) {
            DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition>> rawOutputTriple =
                    ItemValueWithOrigin.createOutputTriple(mapping, beans.prismContext);
            LOGGER.trace("Raw output triple for {}:\n{}", mapping, debugDumpLazily(rawOutputTriple));
            DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> customizedOutputTriple = customizeOutputTriple(rawOutputTriple, mapping, request);
            //noinspection unchecked
            DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, outputPath, customizedOutputTriple);
        }
    }

    @Nullable
    private DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition>> customizeOutputTriple(
            DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition>> rawOutputTriple, MappingImpl<PrismValue, ItemDefinition> mapping, FocalMappingEvaluationRequest<?, ?> request) {
        if (tripleCustomizer != null) {
            DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition>> customizedOutputTriple =
                    tripleCustomizer.customize(rawOutputTriple, request);
            LOGGER.trace("Updated (customized) output triple for {}:\n{}", mapping, debugDumpLazily(customizedOutputTriple));
            return customizedOutputTriple;
        } else {
            return rawOutputTriple;
        }
    }

    private ObjectDeltaObject<F> getUpdatedFocusOdo(LensContext<F> context, ObjectDeltaObject<F> focusOdo,
            PathKeyedMap<DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            FocalMappingEvaluationRequest<?, ?> evaluationRequest, String contextDesc, OperationResult result) throws ExpressionEvaluationException,
            PolicyViolationException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        Holder<ObjectDeltaObject<F>> focusOdoClonedHolder = new Holder<>();
        MappingType mappingBean = evaluationRequest.getMapping();
        for (VariableBindingDefinitionType source : mappingBean.getSource()) {
            updateSource(context, focusOdo, focusOdoClonedHolder, outputTripleMap, contextDesc, source, result);
        }
        ObjectDeltaObject<F> focusOdoCloned = focusOdoClonedHolder.getValue();
        return focusOdoCloned != null ? focusOdoCloned : focusOdo;
    }

    private void updateSource(LensContext<F> context, ObjectDeltaObject<F> focusOdo,
            Holder<ObjectDeltaObject<F>> focusOdoClonedHolder,
            PathKeyedMap<DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap, String contextDesc,
            VariableBindingDefinitionType source, OperationResult result) throws ExpressionEvaluationException,
            PolicyViolationException, SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException {
        if (source.getPath() == null) {
            return;
        }
        ItemPath path = stripFocusVariableSegment(beans.prismContext.toUniformPath(source.getPath()));
        if (path.startsWithVariable()) {
            return;
        }
        DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> triple = outputTripleMap.get(path);
        if (triple == null) {
            return;
        }
        ObjectDeltaObject<F> focusOdoCloned;
        if (focusOdoClonedHolder.isEmpty()) {
            LOGGER.trace("Cloning and updating focusOdo because of chained mappings; chained source/target path: {}", path);
            focusOdoCloned = focusOdo.clone();
            focusOdoClonedHolder.setValue(focusOdoCloned);
        } else {
            LOGGER.trace("Updating focusOdo because of chained mappings; chained source/target path: {}", path);
            focusOdoCloned = focusOdoClonedHolder.getValue();
        }
        Class<F> focusClass = context.getFocusContext().getObjectTypeClass();
        ItemDefinition<?> itemDefinition = getObjectDefinition(focusClass).findItemDefinition(path);

        // TODO not much sure about the parameters
        //noinspection unchecked
        try (IvwoConsolidator consolidator = new IvwoConsolidatorBuilder()
                    .itemPath(path)
                    .ivwoTriple(triple)
                    .itemDefinition(itemDefinition)
                    .aprioriItemDelta(getAprioriItemDelta(focusOdo.getObjectDelta(), path))
                    .itemContainer(focusOdo.getNewObject())
                    .valueMatcher(null)
                    .comparator(null)
                    .addUnchangedValues(true)
                    .addUnchangedValuesExceptForNormalMappings(false) // todo
                    .existingItemKnown(true)
                    .isExclusiveStrong(false)
                    .contextDescription(" updating chained source (" + path + ") in " + contextDesc)
                    .strengthSelector(StrengthSelector.ALL)
                    .valueMetadataComputer(null) // todo
                    .result(result)
                    .build()) {

            ItemDelta itemDelta = consolidator.consolidateToDelta();
            itemDelta.simplify();

            LOGGER.trace("Updating focus ODO with delta:\n{}", itemDelta.debugDumpLazily());
            focusOdoCloned.update(itemDelta);
        }
    }

    private PrismObjectDefinition<F> getObjectDefinition(Class<F> focusClass) {
        return beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
    }

    @FunctionalInterface
    public interface TripleCustomizer<V extends PrismValue, D extends ItemDefinition> {
        DeltaSetTriple<ItemValueWithOrigin<V, D>> customize(DeltaSetTriple<ItemValueWithOrigin<V, D>> triple,
                FocalMappingEvaluationRequest<?, ?> request);
    }

    @FunctionalInterface
    public interface EvaluatedMappingConsumer<V extends PrismValue, D extends ItemDefinition> {
        void accept(MappingImpl<V, D> mapping, FocalMappingEvaluationRequest<?, ?> request);
    }

    PathKeyedMap<DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> getOutputTripleMap() {
        return outputTripleMap;
    }

    NextRecompute getNextRecompute() {
        return nextRecompute;
    }

    // must be Uniform because of the later use in outputTripleMap
    static ItemPath stripFocusVariableSegment(ItemPath sourcePath) {
        if (sourcePath.startsWithVariable()) {
            QName variableQName = sourcePath.firstToVariableNameOrNull();
            if (variableQName != null && FOCUS_VARIABLE_NAMES.contains(variableQName.getLocalPart())) {
                return sourcePath.stripVariableSegment();
            }
        }
        return sourcePath;
    }
}
