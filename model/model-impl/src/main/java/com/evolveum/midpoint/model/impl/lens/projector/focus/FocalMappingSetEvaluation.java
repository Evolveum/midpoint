/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathSegmentImpl;
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
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.repo.common.expression.ConsolidationValueMetadataComputer;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getAprioriItemDelta;
import static com.evolveum.midpoint.repo.common.expression.ExpressionUtil.getPath;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

/**
 * Evaluates a set of focus -> focus mappings.
 *
 * The mappings come from assignments, or from a template - this includes persona mappings.
 *
 * Special responsibilities:
 *
 * 1. Creation of {@link Mapping} objects from beans ({@link AbstractMappingType}).
 * 2. Chaining of mapping evaluation.
 *
 * Exclusions:
 *
 * 1. Sorting mappings according to dependencies. They must come pre-sorted.
 * 2. Consolidation of output triples into deltas.
 *
 * @param <F> type of the focus, in which the whole operation is carried out, i.e. type of {@link LensContext}
 * @param <T> type of the target object
 *
 * @see FocalMappingSetEvaluationBuilder
 */
public class FocalMappingSetEvaluation<F extends AssignmentHolderType, T extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocalMappingSetEvaluation.class);

    private static final List<String> FOCUS_VARIABLE_NAMES = List.of(ExpressionConstants.VAR_FOCUS, ExpressionConstants.VAR_USER);

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

    private final PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap = new PathKeyedMap<>();

    /**
     * Customizes triples produced by mappings before they are aggregated into overall triple map.
     */
    private final TripleCustomizer<?, ?> tripleCustomizer;

    /**
     * Receives each mapping as it's created and evaluated.
     */
    private final EvaluatedMappingConsumer mappingConsumer;

    private final int iteration;

    private final String iterationToken;

    private final MappingEvaluationEnvironment env;

    private final OperationResult result;

    private NextRecompute nextRecompute;

    FocalMappingSetEvaluation(FocalMappingSetEvaluationBuilder<F, T> builder) {
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

    void evaluateMappingsToTriples()
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        for (FocalMappingEvaluationRequest<?, ?> request : requests) {
            evaluateMappingRequest(request);
        }
    }

    private void evaluateMappingRequest(FocalMappingEvaluationRequest<?, ?> request)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException {

        MappingImpl<?, ?> mapping = createMapping(request);
        if (mapping == null) {
            return;
        }

        beans.mappingEvaluator.evaluateMapping(mapping, context, env.task, result);
        if (!mapping.isEnabled()) { // We could check this right after mapping is created. But this seems a bit cleaner.
            return;
        }

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

    private MappingImpl<PrismValue, ItemDefinition<?>> createMapping(FocalMappingEvaluationRequest<?, ?> request)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException {
        String description = request.shortDump();
        LOGGER.trace("Starting evaluation of {}", description);

        ObjectDeltaObject<F> updatedFocusOdo = doingChaining ?
                getUpdatedFocusOdo(context, focusOdo, outputTripleMap, request, description, result) :
                focusOdo;

        PrismObject<T> targetObject = targetSpecification.getTargetObject();
        if (targetObject == null) {
            LOGGER.trace("No target object, skipping mapping evaluation"); // probably the focus is being deleted
            return null;
        }

        return createFocusMapping(context,
                request, updatedFocusOdo, targetObject, iteration, iterationToken,
                context.getSystemConfiguration(), env.now, description, env.task, result);
    }

    private <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType>
    MappingImpl<V, D> createFocusMapping(
            LensContext<AH> context,
            FocalMappingEvaluationRequest<?, ?> request,
            ObjectDeltaObject<AH> focusOdo,
            @NotNull PrismObject<T> targetContext,
            Integer iteration,
            String iterationToken,
            PrismObject<SystemConfigurationType> configuration,
            XMLGregorianCalendar now,
            String contextDesc,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        MappingType mappingBean = request.getMapping();
        MappingKindType mappingKind = request.getMappingKind();
        ObjectType originObject = request.getOriginObject();
        Source<V, D> defaultSource = request.constructDefaultSource(focusOdo);
        AssignmentPathVariables assignmentPathVariables = request.getAssignmentPathVariables();

        if (!MappingImpl.isApplicableToChannel(mappingBean, context.getChannel())) {
            LOGGER.trace("Mapping {} is not applicable to channel {}, skipping", mappingBean, context.getChannel());
            return null;
        }

        if (!task.canSee(mappingBean)) {
            LOGGER.trace("Mapping {} is not visible in the current task, skipping", mappingBean);
            return null;
        }

        ConfigurableValuePolicySupplier valuePolicySupplier = new ConfigurableValuePolicySupplier() {
            private ItemDefinition<?> outputDefinition;

            @Override
            public void setOutputDefinition(ItemDefinition<?> outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType get(OperationResult result) {
                // TODO need to switch to ObjectValuePolicyEvaluator
                if (outputDefinition.getItemName().equals(PasswordType.F_VALUE)) {
                    return beans.credentialsProcessor.determinePasswordPolicy(context.getFocusContext());
                }
                ExpressionType expressionBean = mappingBean.getExpression();
                if (expressionBean == null) {
                    return null;
                }
                List<JAXBElement<?>> evaluators = expressionBean.getExpressionEvaluator();
                if (evaluators != null) {
                    for (JAXBElement<?> jaxbEvaluator : evaluators) {
                        Object object = jaxbEvaluator.getValue();
                        if (!(object instanceof GenerateExpressionEvaluatorType genEvaluatorBean)) {
                            continue;
                        }
                        ObjectReferenceType ref = genEvaluatorBean.getValuePolicyRef();
                        if (ref == null) {
                            continue;
                        }
                        try {
                            return beans.mappingFactory.getObjectResolver().resolve(
                                    ref, ValuePolicyType.class,
                                    null,
                                    "resolving value policy for generate attribute " + outputDefinition.getItemName() + " value",
                                    task, result);
                        } catch (CommonException ex) {
                            throw new SystemException(ex.getMessage(), ex);
                        }
                    }
                }
                return null;
            }
        };

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_FOCUS, focusOdo, focusOdo.getDefinition());
        variables.put(ExpressionConstants.VAR_USER, focusOdo, focusOdo.getDefinition());
        variables.registerAlias(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);
        variables.put(ExpressionConstants.VAR_ITERATION, iteration, Integer.class);
        variables.put(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken, String.class);
        variables.put(ExpressionConstants.VAR_CONFIGURATION, configuration, SystemConfigurationType.class);
        variables.put(ExpressionConstants.VAR_OPERATION, context.getFocusContext().getOperation().getValue(), String.class);
        variables.put(ExpressionConstants.VAR_SOURCE, originObject, ObjectType.class);

        PrismContext prismContext = beans.prismContext;

        TypedValue<PrismObject<T>> defaultTargetContext = new TypedValue<>(targetContext);
        Collection<V> targetValues =
                ExpressionUtil.computeTargetValues(
                        getPath(mappingBean.getTarget()),
                        defaultTargetContext,
                        variables,
                        beans.mappingFactory.getObjectResolver(),
                        contextDesc,
                        task,
                        result);

        MappingSpecificationType specification = new MappingSpecificationType()
                .mappingName(mappingBean.getName())
                .definitionObjectRef(ObjectTypeUtil.createObjectRef(originObject, prismContext))
                .assignmentId(createAssignmentId(assignmentPathVariables));

        MappingBuilder<V, D> mappingBuilder =
                beans.mappingFactory.<V, D>createMappingBuilder(mappingBean, request.getMappingOrigin(), contextDesc)
                        .sourceContext(focusOdo)
                        .defaultSource(defaultSource)
                        .targetContext(targetContext.getDefinition())
                        .variablesFrom(variables)
                        .variablesFrom(LensUtil.getAssignmentPathVariablesMap(assignmentPathVariables))
                        .originalTargetValues(targetValues)
                        .mappingKind(mappingKind)
                        .originType(OriginType.USER_POLICY)
                        .originObject(originObject)
                        .valuePolicySupplier(valuePolicySupplier)
                        .rootNode(focusOdo)
                        .mappingPreExpression(request.getMappingPreExpression()) // Used to populate auto-assign assignments
                        .mappingSpecification(specification)
                        .now(now);

        MappingImpl<V, D> mapping = mappingBuilder.build();

        ItemPath itemPath = mapping.getOutputPath();
        if (itemPath == null) {
            // no output element, i.e. this is a "validation mapping"
            return mapping;
        }

        Item<V, D> existingTargetItem = targetContext.findItem(itemPath);
        if (existingTargetItem != null
                && !existingTargetItem.isEmpty()
                && mapping.getStrength() == MappingStrengthType.WEAK) {
            LOGGER.trace("Mapping {} is weak and target already has a value {}, skipping.", mapping, existingTargetItem);
            return null;
        }

        return mapping;
    }

    private String createAssignmentId(AssignmentPathVariables assignmentPathVariables) {
        if (assignmentPathVariables == null || assignmentPathVariables.getAssignmentPath() == null) {
            return null;
        } else {
            return assignmentPathVariables.getAssignmentPath().getSegments().stream()
                    .map(AssignmentPathSegmentImpl::getAssignmentId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(":"));
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>>
    void updateOutputTripleMap(MappingImpl<V, D> mapping, FocalMappingEvaluationRequest<?, ?> request) {
        ItemPath outputPath = mapping.getOutputPath();
        if (outputPath != null) {
            DeltaSetTriple<ItemValueWithOrigin<V, D>> rawOutputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
            LOGGER.trace("Raw output triple for {}:\n{}", mapping, debugDumpLazily(rawOutputTriple));
            // TODO fix this hack
            //noinspection unchecked,rawtypes
            DeltaSetTriple<ItemValueWithOrigin<?, ?>> customizedOutputTriple =
                    (DeltaSetTriple) customizeOutputTriple(rawOutputTriple, mapping, request);
            DeltaSetTripleUtil.putIntoOutputTripleMap(outputTripleMap, outputPath, customizedOutputTriple);
        }
    }

    @Nullable
    private <V extends PrismValue, D extends ItemDefinition<?>>
            DeltaSetTriple<ItemValueWithOrigin<V, D>> customizeOutputTriple(
            DeltaSetTriple<ItemValueWithOrigin<V, D>> rawOutputTriple,
            MappingImpl<V, D> mapping, FocalMappingEvaluationRequest<?, ?> request) {
        if (tripleCustomizer != null) {
            //noinspection unchecked
            TripleCustomizer<V, D> typedTripleCustomizer = (TripleCustomizer<V, D>) tripleCustomizer;
            DeltaSetTriple<ItemValueWithOrigin<V, D>> customizedOutputTriple =
                    typedTripleCustomizer.customize(rawOutputTriple, request);
            LOGGER.trace("Updated (customized) output triple for {}:\n{}", mapping, debugDumpLazily(customizedOutputTriple));
            return customizedOutputTriple;
        } else {
            return rawOutputTriple;
        }
    }

    private ObjectDeltaObject<F> getUpdatedFocusOdo(
            LensContext<F> context,
            ObjectDeltaObject<F> focusOdo,
            PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap,
            FocalMappingEvaluationRequest<?, ?> evaluationRequest,
            String contextDesc,
            OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException {
        Holder<ObjectDeltaObject<F>> focusOdoClonedHolder = new Holder<>();
        for (VariableBindingDefinitionType source : evaluationRequest.getSources()) {
            updateSource(context, focusOdo, focusOdoClonedHolder, outputTripleMap, contextDesc, source, result);
        }
        ObjectDeltaObject<F> focusOdoCloned = focusOdoClonedHolder.getValue();
        return focusOdoCloned != null ? focusOdoCloned : focusOdo;
    }

    private void updateSource(LensContext<F> context, ObjectDeltaObject<F> focusOdo,
            Holder<ObjectDeltaObject<F>> focusOdoClonedHolder,
            PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap, String contextDesc,
            VariableBindingDefinitionType source, OperationResult result) throws ExpressionEvaluationException,
            SchemaException, ConfigurationException, ObjectNotFoundException,
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

        ConsolidationValueMetadataComputer valueMetadataComputer = LensMetadataUtil.createValueMetadataConsolidationComputer(
                path, context, beans, env, result);

        // TODO not much sure about the parameters
        //noinspection unchecked,rawtypes
        try (IvwoConsolidator<?, ?, ? extends ItemValueWithOrigin<?, ?>> consolidator = new IvwoConsolidatorBuilder<>()
                    .itemPath(path)
                    .ivwoTriple((DeltaSetTriple) triple)
                    .itemDefinition(itemDefinition)
                    .aprioriItemDelta(getAprioriItemDelta(focusOdo.getObjectDelta(), path))
                    .itemDeltaExists(context.primaryFocusItemDeltaExists(path))
                    .itemContainer(focusOdo.getNewObject()) // covers existingItem
                    .valueMatcher(null)
                    .comparator(null)
                    .addUnchangedValues(false) // todo
                    .addUnchangedValuesExceptForNormalMappings(true) // todo
                    .existingItemKnown(true)
                    .isExclusiveStrong(false)
                    .contextDescription(" updating chained source (" + path + ") in " + contextDesc)
                    .strengthSelector(StrengthSelector.ALL)
                    .valueMetadataComputer(valueMetadataComputer)
                    .result(result)
                    .build()) {

            ItemDelta<?, ?> itemDelta = consolidator.consolidateTriples();
            itemDelta.simplify();

            LOGGER.trace("Updating focus ODO with delta:\n{}", itemDelta.debugDumpLazily());
            focusOdoCloned.update(itemDelta);
        }
    }

    private PrismObjectDefinition<F> getObjectDefinition(Class<F> focusClass) {
        return beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
    }

    @FunctionalInterface
    public interface TripleCustomizer<V extends PrismValue, D extends ItemDefinition<?>> {
        DeltaSetTriple<ItemValueWithOrigin<V, D>> customize(DeltaSetTriple<ItemValueWithOrigin<V, D>> triple,
                FocalMappingEvaluationRequest<?, ?> request);
    }

    @FunctionalInterface
    public interface EvaluatedMappingConsumer {
        void accept(MappingImpl<?, ?> mapping, FocalMappingEvaluationRequest<?, ?> request);
    }

    PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> getOutputTripleMap() {
        return outputTripleMap;
    }

    NextRecompute getNextRecompute() {
        return nextRecompute;
    }

    // must be Uniform because of the later use in outputTripleMap
    static @NotNull ItemPath stripFocusVariableSegment(@NotNull ItemPath sourcePath) {
        if (sourcePath.startsWithVariable()) {
            QName variableQName = sourcePath.firstToVariableNameOrNull();
            if (variableQName != null && FOCUS_VARIABLE_NAMES.contains(variableQName.getLocalPart())) {
                return sourcePath.stripVariableSegment();
            }
        }
        return sourcePath;
    }
}
