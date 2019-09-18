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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConstants;
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

    public <V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType, T extends AssignmentHolderType>
    NextRecompute evaluateMappingsToTriples(
            LensContext<AH> context, List<FocalMappingEvaluationRequest<?, ?>> mappings, ObjectTemplateMappingEvaluationPhaseType phase,
            ObjectDeltaObject<AH> focusOdo, PrismObject<T> target,
            Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            int iteration, String iterationToken,
            XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        List<FocalMappingEvaluationRequest<?, ?>> filteredMappings = filterMappingsByPhase(mappings, phase);
        List<FocalMappingEvaluationRequest<?, ?>> sortedMappings = sortMappingsByDependencies(filteredMappings);

        NextRecompute nextRecompute = null;

        for (FocalMappingEvaluationRequest<?, ?> mappingRequest : sortedMappings) {
            String mappingDesc = mappingRequest.shortDump();
            LOGGER.trace("Starting evaluation of {}", mappingDesc);
            ObjectDeltaObject<AH> updatedFocusOdo = getUpdatedFocusOdo(context, focusOdo, outputTripleMap, mappingRequest, mappingDesc);		// for mapping chaining

            MappingImpl<V,D> mapping = mappingEvaluator.createFocusMapping(mappingFactory, context, mappingRequest.getMapping(),
                    mappingRequest.getOriginObject(), updatedFocusOdo, mappingRequest.constructDefaultSource(focusOdo), target,
                    null, iteration, iterationToken, context.getSystemConfiguration(), now, mappingDesc, task, result);
            if (mapping == null) {
                continue;
            }

            // Used to populate autoassign assignments
            mapping.setMappingPreExpression(mappingRequest);

            Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(task, result);

            if (timeConstraintValid != null && !timeConstraintValid) {
                // Delayed mapping. Just schedule recompute time
                XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
                LOGGER.trace("Evaluation of mapping {} delayed to {}", mapping, mappingNextRecomputeTime);
                if (mappingNextRecomputeTime != null) {
                    if (nextRecompute == null || nextRecompute.nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER) {
                        nextRecompute = new NextRecompute(mappingNextRecomputeTime, mapping.getIdentifier());
                    }
                }
                continue;
            }

            mappingEvaluator.evaluateMapping(mapping, context, task, result);

            ItemPath outputPath = mapping.getOutputPath();
            if (outputPath == null) {
                continue;
            }
            DeltaSetTriple<ItemValueWithOrigin<V,D>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping, prismContext);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Output triple for {}:\n{}", mapping, DebugUtil.debugDump(outputTriple));
            }
            if (outputTriple == null) {
                continue;
            }
            UniformItemPath uniformItemPath = prismContext.toUniformPath(outputPath);
            //noinspection unchecked
            DeltaSetTriple<ItemValueWithOrigin<V,D>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V,D>>) outputTripleMap.get(uniformItemPath);
            if (mapTriple == null) {
                outputTripleMap.put(uniformItemPath, outputTriple);
            } else {
                mapTriple.merge(outputTriple);
            }
        }

        return nextRecompute;
    }

    private List<FocalMappingEvaluationRequest<?, ?>> filterMappingsByPhase(List<FocalMappingEvaluationRequest<?, ?>> mappings,
            ObjectTemplateMappingEvaluationPhaseType phase) {
        if (phase != null) {
            return mappings.stream()
                    .filter(m -> m.getEvaluationPhase() == phase)
                    .collect(Collectors.toList());
        } else {
            return mappings;
        }
    }

    @SuppressWarnings("unchecked")
    private <AH extends AssignmentHolderType> ObjectDeltaObject<AH> getUpdatedFocusOdo(LensContext<AH> context, ObjectDeltaObject<AH> focusOdo,
            Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            FocalMappingEvaluationRequest<?, ?> mapping, String contextDesc) throws ExpressionEvaluationException,
            PolicyViolationException, SchemaException {
        ObjectDeltaObject<AH> focusOdoCloned = null;
        for (VariableBindingDefinitionType source : mapping.getMapping().getSource()) {
            if (source.getPath() == null) {
                continue;
            }
            ItemPath path = stripFocusVariableSegment(prismContext.toUniformPath(source.getPath()));
            if (path.startsWithVariable()) {
                continue;
            }
            DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> triple = DeltaSetTripleUtil.find(outputTripleMap, path);
            if (triple == null) {
                continue;
            }
            if (focusOdoCloned == null) {
                LOGGER.trace("Cloning and updating focusOdo because of chained mappings; chained source path: {}", path);
                focusOdoCloned = focusOdo.clone();
            } else {
                LOGGER.trace("Updating focusOdo because of chained mappings; chained source path: {}", path);
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
        return focusOdoCloned != null ? focusOdoCloned : focusOdo;
    }

    private <F extends ObjectType> PrismObjectDefinition<F> getObjectDefinition(Class<F> focusClass) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
    }

    /**
     * If M2 has a source of X, and M1 has a target of X, then M1 must be placed before M2; we want also to detect cycles.
     *
     * So let's stratify mappings according to their dependencies.
     */
    private List<FocalMappingEvaluationRequest<?, ?>> sortMappingsByDependencies(List<FocalMappingEvaluationRequest<?, ?>> mappings) {
        // map.get(X) = { Y1 ... Yn } means that mapping X depends on output of mappings Y1 ... Yn
        // using indices instead of actual mappings because of equality issues
        Map<Integer, Set<Integer>> dependencyMap = createDependencyMap(mappings);
        LOGGER.trace("sortMappingsByDependencies: dependencyMap: {}", dependencyMap);

        List<Integer> processed = new ArrayList<>();
        List<Integer> toProcess = Stream.iterate(0, t -> t+1).limit(mappings.size()).collect(Collectors.toList());		// not a set: to preserve original order
        while (!toProcess.isEmpty()) {
            LOGGER.trace("sortMappingsByDependencies: toProcess: {}, processed: {}", toProcess, processed);
            Integer available = toProcess.stream()
                    .filter(i -> CollectionUtils.isSubCollection(dependencyMap.get(i), processed))	// cannot depend on yet-unprocessed mappings
                    .findFirst().orElse(null);
            if (available == null) {
                LOGGER.warn("Cannot sort mappings according to dependencies, there is a cycle. Processing in the original order: {}", mappings);
                return mappings;
            }
            processed.add(available);
            toProcess.remove(available);
        }
        LOGGER.trace("sortMappingsByDependencies: final ordering: {}", processed);
        return processed.stream().map(i -> mappings.get(i)).collect(Collectors.toList());
    }

    private Map<Integer, Set<Integer>> createDependencyMap(List<FocalMappingEvaluationRequest<?, ?>> mappings) {
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

}
