/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.IvwoConsolidator;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.StrengthSelector;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTripleUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getAprioriItemDelta;

/**
 *  Evaluates a set of mappings. This includes considering their dependencies (chaining).
 */
@Component
public class MappingSetEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(MappingEvaluator.class);

    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private MappingFactory mappingFactory;
    @Autowired private PrismContext prismContext;

    /**
     * Evaluates a set of mappings; chaining them appropriately if needed.
     *
     * @param targetSpecification How's the target object determined? Currently it's always a fixed object but we can consider
     *                            updating it dynamically as mappings are progressively evaluated.
     * @param tripleCustomizer Customizes triples produced by mappings before they are aggregated into overall triple map.
     * @param mappingConsumer Receives each mapping as it's created and evaluated.
     * @param iteration TODO should this be a parameter for this method?
     * @param iterationToken TODO should this be a parameter for this method?
     */
    public <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType, T extends AssignmentHolderType>
    NextRecompute evaluateMappingsToTriples(
            LensContext<AH> context, List<? extends FocalMappingEvaluationRequest<?, ?>> evaluationRequests,
            ObjectTemplateMappingEvaluationPhaseType phase, ObjectDeltaObject<AH> focusOdo, TargetObjectSpecification<T> targetSpecification,
            Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            TripleCustomizer<V, D> tripleCustomizer, EvaluatedMappingConsumer<V, D> mappingConsumer,
            int iteration, String iterationToken, XMLGregorianCalendar now,
            Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        List<? extends FocalMappingEvaluationRequest<?, ?>> filteredRequests = filterMappingsByPhase(evaluationRequests, phase);
        List<? extends FocalMappingEvaluationRequest<?, ?>> sortedRequests = sortMappingsByDependencies(filteredRequests);

        NextRecompute nextRecompute = null;

        for (FocalMappingEvaluationRequest<?, ?> request : sortedRequests) {
            String description = request.shortDump();
            LOGGER.trace("Starting evaluation of {}", description);

            boolean updateAlsoTargetItem = targetSpecification.isUpdatedWithMappingResults();

            // for mapping chaining
            ObjectDeltaObject<AH> updatedFocusOdo = getUpdatedFocusOdo(context, focusOdo, outputTripleMap, request,
                    updateAlsoTargetItem, description);

            PrismObject<T> targetObject = targetSpecification.getTargetObject(updatedFocusOdo);

            MappingImpl<V,D> mapping = mappingEvaluator.createFocusMapping(mappingFactory, context, request.getMapping(),
                    request.getMappingKind(), request.getOriginObject(), updatedFocusOdo, request.constructDefaultSource(updatedFocusOdo),
                    targetObject, request.getAssignmentPathVariables(), iteration, iterationToken,
                    context.getSystemConfiguration(), now, description, task, result);
            if (mapping == null) {
                continue;
            }

            // Used to populate autoassign assignments
            mapping.setMappingPreExpression(request);

            mappingEvaluator.evaluateMapping(mapping, context, task, result);

            // We need to update nextRecompute even for mappings with "time valid" state.
            nextRecompute = NextRecompute.update(mapping, nextRecompute);

            if (!mapping.evaluateTimeConstraintValid(task, result)) {
                continue;
            }

            if (mappingConsumer != null) {
                mappingConsumer.accept(mapping, request);
            }

            ItemPath outputPath = mapping.getOutputPath();
            if (outputPath != null) {
                DeltaSetTriple<ItemValueWithOrigin<V, D>> rawOutputTriple = ItemValueWithOrigin
                        .createOutputTriple(mapping, prismContext);
                LOGGER.trace("Raw output triple for {}:\n{}", mapping, DebugUtil.debugDumpLazily(rawOutputTriple));
                DeltaSetTriple<ItemValueWithOrigin<V, D>> updatedOutputTriple;
                if (tripleCustomizer != null) {
                    updatedOutputTriple = tripleCustomizer.updateTriple(rawOutputTriple, request);
                    LOGGER.trace("Updated output triple for {}:\n{}", mapping, DebugUtil.debugDumpLazily(updatedOutputTriple));
                } else {
                    updatedOutputTriple = rawOutputTriple;
                }
                if (updatedOutputTriple != null) {
                    UniformItemPath uniformItemPath = prismContext.toUniformPath(outputPath);
                    //noinspection unchecked
                    DeltaSetTriple<ItemValueWithOrigin<V, D>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V, D>>)
                            outputTripleMap.get(uniformItemPath);
                    if (mapTriple == null) {
                        outputTripleMap.put(uniformItemPath, updatedOutputTriple);
                    } else {
                        mapTriple.merge(updatedOutputTriple);
                    }
                }
            }
        }

        return nextRecompute;
    }

    private List<? extends FocalMappingEvaluationRequest<?, ?>> filterMappingsByPhase(
            List<? extends FocalMappingEvaluationRequest<?, ?>> mappings, ObjectTemplateMappingEvaluationPhaseType phase) {
        if (phase != null) {
            return mappings.stream()
                    .filter(m -> m.getEvaluationPhase() == phase)
                    .collect(Collectors.toList());
        } else {
            return mappings;
        }
    }

    @SuppressWarnings("unused")
    private <AH extends AssignmentHolderType> ObjectDeltaObject<AH> getUpdatedFocusOdo(LensContext<AH> context, ObjectDeltaObject<AH> focusOdo,
            Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            FocalMappingEvaluationRequest<?, ?> evaluationRequest, boolean updateAlsoTargetItem, String contextDesc) throws ExpressionEvaluationException,
            PolicyViolationException, SchemaException {
        Holder<ObjectDeltaObject<AH>> focusOdoClonedHolder = new Holder<>();
        MappingType mappingBean = evaluationRequest.getMapping();
        for (VariableBindingDefinitionType source : mappingBean.getSource()) {
            updateSourceOrTarget(context, focusOdo, focusOdoClonedHolder, outputTripleMap, contextDesc, source);
        }
        // TODO Consider if we need to update the target item.
        //   Generally, we use its value to know if we have to remove a value because of range specification.
        //   (or is there any other reason?) But if two mappings conflict on range, we should not "help" them too eagerly.
        //   Let's keep the computation running, and then see if there's something to be resolved.
        //   --> Keeping the code commented out just in case it would be needed some day.
//        if (updateAlsoTargetItem && mappingBean.getTarget() != null) {
//            updateSourceOrTarget(context, focusOdo, focusOdoClonedHolder, outputTripleMap, contextDesc, mappingBean.getTarget());
//        }
        ObjectDeltaObject<AH> focusOdoCloned = focusOdoClonedHolder.getValue();
        return focusOdoCloned != null ? focusOdoCloned : focusOdo;
    }

    private <AH extends AssignmentHolderType> void updateSourceOrTarget(LensContext<AH> context, ObjectDeltaObject<AH> focusOdo,
            Holder<ObjectDeltaObject<AH>> focusOdoClonedHolder,
            Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap, String contextDesc,
            VariableBindingDefinitionType source) throws ExpressionEvaluationException,
            PolicyViolationException, SchemaException {
        if (source.getPath() == null) {
            return;
        }
        ItemPath path = stripFocusVariableSegment(prismContext.toUniformPath(source.getPath()));
        if (path.startsWithVariable()) {
            return;
        }
        DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> triple = DeltaSetTripleUtil.find(outputTripleMap, path);
        if (triple == null) {
            return;
        }
        ObjectDeltaObject<AH> focusOdoCloned;
        if (focusOdoClonedHolder.isEmpty()) {
            LOGGER.trace("Cloning and updating focusOdo because of chained mappings; chained source/target path: {}", path);
            focusOdoCloned = focusOdo.clone();
            focusOdoClonedHolder.setValue(focusOdoCloned);
        } else {
            LOGGER.trace("Updating focusOdo because of chained mappings; chained source/target path: {}", path);
            focusOdoCloned = focusOdoClonedHolder.getValue();
        }
        Class<AH> focusClass = context.getFocusContext().getObjectTypeClass();
        ItemDefinition<?> itemDefinition = getObjectDefinition(focusClass).findItemDefinition(path);

        // TODO not much sure about the parameters
        IvwoConsolidator consolidator = new IvwoConsolidator<>();
        consolidator.setItemPath(path);
        consolidator.setIvwoTriple(triple);
        consolidator.setItemDefinition(itemDefinition);
        consolidator.setAprioriItemDelta(getAprioriItemDelta(focusOdo.getObjectDelta(), path));
        consolidator.setItemContainer(focusOdo.getNewObject());
        consolidator.setValueMatcher(null);
        consolidator.setComparator(null);
        consolidator.setAddUnchangedValues(true);
        consolidator.setFilterExistingValues(true);
        consolidator.setExclusiveStrong(false);
        consolidator.setContextDescription(" updating chained source (" + path + ") in " + contextDesc);
        consolidator.setStrengthSelector(StrengthSelector.ALL);

        ItemDelta itemDelta = consolidator.consolidateToDelta();

        LOGGER.trace("Updating focus ODO with delta:\n{}", itemDelta.debugDumpLazily());
        focusOdoCloned.update(itemDelta);
    }

    private <F extends ObjectType> PrismObjectDefinition<F> getObjectDefinition(Class<F> focusClass) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
    }

    /**
     * If M2 has a source of X, and M1 has a target of X, then M1 must be placed before M2; we want also to detect cycles.
     *
     * So let's stratify mappings according to their dependencies.
     */
    private List<? extends FocalMappingEvaluationRequest<?, ?>> sortMappingsByDependencies(List<? extends FocalMappingEvaluationRequest<?, ?>> requests) {
        // map.get(X) = { Y1 ... Yn } means that mapping X depends on output of mappings Y1 ... Yn
        // using indices instead of actual mappings because of equality issues
        Map<Integer, Set<Integer>> dependencyMap = createDependencyMap(requests);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("sortMappingsByDependencies: Starting with mapping requests:\n{}\nDependency map (z=[a,b,c] means that z depends on a, b, c):\n{}\n",
                    dumpMappings(requests), dependencyMap);
        }

        List<Integer> processed = new ArrayList<>();
        List<Integer> toProcess = Stream.iterate(0, t -> t+1).limit(requests.size()).collect(Collectors.toList());        // not a set: to preserve original order
        while (!toProcess.isEmpty()) {
            LOGGER.trace("sortMappingsByDependencies: toProcess: {}, processed: {}", toProcess, processed);
            Integer available = toProcess.stream()
                    .filter(i -> CollectionUtils.isSubCollection(dependencyMap.get(i), processed))    // cannot depend on yet-unprocessed mappings
                    .findFirst().orElse(null);
            if (available == null) {
                LOGGER.warn("Cannot sort mappings according to dependencies, there is a cycle involving mappings {}:\n{}",
                        toProcess, dumpMappings(selectRequests(requests, toProcess)));
                List<Integer> finalOrder = new ArrayList<>(processed);
                finalOrder.addAll(toProcess);
                List<? extends FocalMappingEvaluationRequest<?, ?>> rv = selectRequests(requests, finalOrder);
                LOGGER.warn("Processing mappings in partially satisfying order: {}\n{}", finalOrder, dumpMappings(rv));
                return rv;
            }
            processed.add(available);
            toProcess.remove(available);
        }
        //noinspection UnnecessaryLocalVariable
        List<Integer> finalOrder = processed;
        List<? extends FocalMappingEvaluationRequest<?, ?>> rv = selectRequests(requests, finalOrder);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("sortMappingsByDependencies: final ordering: {}\n{}", finalOrder, dumpMappings(rv));
        }
        return rv;
    }

    private String dumpMappings(List<? extends FocalMappingEvaluationRequest<?,?>> requests) {
        StringBuilder sb = new StringBuilder();
        for (FocalMappingEvaluationRequest<?, ?> request : requests) {
            sb.append(" - ").append(request.shortDump()).append("\n");
        }
        return sb.toString();
    }

    private List<? extends FocalMappingEvaluationRequest<?, ?>> selectRequests(
            List<? extends FocalMappingEvaluationRequest<?, ?>> requests, List<Integer> numbers) {
        if (numbers != null) {
            return numbers.stream().map(index -> requests.get(index)).collect(Collectors.toList());
        } else {
            return requests;
        }
    }

    private Map<Integer, Set<Integer>> createDependencyMap(List<? extends FocalMappingEvaluationRequest<?, ?>> mappings) {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        for (int i = 0; i < mappings.size(); i++) {
            Set<Integer> dependsOn = new HashSet<>();
            for (int j = 0; j < mappings.size(); j++) {
                if (i == j) {
                    continue;
                }
                if (dependsOn(mappings.get(i), mappings.get(j))) {
                    dependsOn.add(j);
                }
            }
            map.put(i, dependsOn);
        }
        return map;
    }

    // true if any source of mapping1 is equivalent to the target of mapping2
    private boolean dependsOn(FocalMappingEvaluationRequest<?, ?> mappingRequest1,
            FocalMappingEvaluationRequest<?, ?> mappingRequest2) {
        MappingType mapping1 = mappingRequest1.getMapping();
        MappingType mapping2 = mappingRequest2.getMapping();
        if (mapping2.getTarget() == null || mapping2.getTarget().getPath() == null) {
            return false;
        }
        ItemPath targetPath = mapping2.getTarget().getPath().getItemPath().stripVariableSegment();

        for (VariableBindingDefinitionType source : mapping1.getSource()) {
            ItemPath sourcePath = prismContext.toPath(source.getPath());
            if (sourcePath != null && stripFocusVariableSegment(sourcePath).equivalent(targetPath)) {
                return true;
            }
        }
        return false;
    }

    // must be Uniform because of the later use in outputTripleMap
    private ItemPath stripFocusVariableSegment(ItemPath sourcePath) {
        if (sourcePath.startsWithVariable()) {
            QName variableQName = sourcePath.firstToVariableNameOrNull();
            if (variableQName != null && MappingEvaluator.FOCUS_VARIABLE_NAMES.contains(variableQName.getLocalPart())) {
                return sourcePath.stripVariableSegment();
            }
        }
        return sourcePath;
    }

    @FunctionalInterface
    public interface TripleCustomizer<V extends PrismValue, D extends ItemDefinition> {
        DeltaSetTriple<ItemValueWithOrigin<V, D>> updateTriple(DeltaSetTriple<ItemValueWithOrigin<V, D>> triple,
                FocalMappingEvaluationRequest<?, ?> request);
    }

    @FunctionalInterface
    public interface EvaluatedMappingConsumer<V extends PrismValue, D extends ItemDefinition> {
        void accept(MappingImpl<V, D> mapping, FocalMappingEvaluationRequest<?, ?> request);
    }
}
