/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.FocalMappingEvaluationRequest;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

/**
 * Sorts mappings (evaluation requests) by their dependencies.
 */
class MappingSorter {

    private static final Trace LOGGER = TraceManager.getTrace(MappingSorter.class);

    private final ModelBeans beans;

    MappingSorter(ModelBeans beans) {
        this.beans = beans;
    }

    List<? extends FocalMappingEvaluationRequest<?, ?>> filterAndSort(List<? extends FocalMappingEvaluationRequest<?, ?>> evaluationRequests,
            ObjectTemplateMappingEvaluationPhaseType phase) {
        List<? extends FocalMappingEvaluationRequest<?, ?>> filtered = filter(evaluationRequests, phase);
        return sortRequestsByDependencies(filtered);
    }

    List<? extends FocalMappingEvaluationRequest<?, ?>> filter(
            List<? extends FocalMappingEvaluationRequest<?, ?>> evaluationRequests,
            ObjectTemplateMappingEvaluationPhaseType phase) {
        if (phase != null) {
            return evaluationRequests.stream()
                    .filter(m -> m.getEvaluationPhase() == phase)
                    .collect(Collectors.toList());
        } else {
            return evaluationRequests;
        }
    }

    /**
     * If M2 has a source of X, and M1 has a target of X, then M1 must be placed before M2; we want also to detect cycles.
     *
     * So let's stratify mappings according to their dependencies.
     */
    private List<? extends FocalMappingEvaluationRequest<?, ?>> sortRequestsByDependencies(List<? extends FocalMappingEvaluationRequest<?, ?>> requests) {
        // map.get(X) = { Y1 ... Yn } means that mapping X depends on output of mappings Y1 ... Yn
        // using indices instead of actual mappings because of equality issues
        Map<Integer, Set<Integer>> dependencyMap = createDependencyMap(requests);
        LOGGER.trace("sortMappingsByDependencies: Starting with mapping requests:\n{}\nDependency map (z=[a,b,c] means that z depends on a, b, c):\n{}\n",
                lazy(() -> dumpMappings(requests)), dependencyMap);

        List<Integer> processed = new ArrayList<>();
        List<Integer> toProcess = Stream.iterate(0, t -> t+1).limit(requests.size()).collect(Collectors.toList()); // not a set: to preserve original order
        while (!toProcess.isEmpty()) {
            LOGGER.trace("sortMappingsByDependencies: toProcess: {}, processed: {}", toProcess, processed);
            Integer available = toProcess.stream()
                    .filter(i -> CollectionUtils.isSubCollection(dependencyMap.get(i), processed))    // cannot depend on yet-unprocessed mappings
                    .findFirst().orElse(null);
            if (available == null) {
                LOGGER.warn("Cannot sort mappings according to dependencies, because there is a cycle involving mappings #{}. "
                        + "Computation results may be incorrect. Please enable DEBUG logging to see the details.", toProcess);
                LOGGER.debug("Mappings that cannot be sorted:\n{}", lazy(() -> dumpMappings(selectRequests(requests, toProcess))));
                List<Integer> finalOrder = new ArrayList<>(processed);
                finalOrder.addAll(toProcess);
                List<? extends FocalMappingEvaluationRequest<?, ?>> rv = selectRequests(requests, finalOrder);
                LOGGER.debug("Processing mappings in partially satisfying order: {}\n{}", finalOrder, lazy(() -> dumpMappings(rv)));
                return rv;
            }
            processed.add(available);
            toProcess.remove(available);
        }
        //noinspection UnnecessaryLocalVariable
        List<Integer> finalOrder = processed;
        List<? extends FocalMappingEvaluationRequest<?, ?>> rv = selectRequests(requests, finalOrder);
        LOGGER.trace("sortMappingsByDependencies: final ordering: {}\n{}", finalOrder, lazy(() -> dumpMappings(rv)));
        return rv;
    }

    private String dumpMappings(Iterable<? extends FocalMappingEvaluationRequest<?, ?>> requests) {
        StringBuilder sb = new StringBuilder();
        for (FocalMappingEvaluationRequest<?, ?> request : requests) {
            sb.append(" - ").append(request.shortDump()).append("\n");
        }
        return sb.toString();
    }

    private List<? extends FocalMappingEvaluationRequest<?, ?>> selectRequests(
            List<? extends FocalMappingEvaluationRequest<?, ?>> requests, Collection<Integer> numbers) {
        if (numbers != null) {
            return numbers.stream()
                    .map(requests::get)
                    .collect(Collectors.toList());
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
    private boolean dependsOn(
            FocalMappingEvaluationRequest<?, ?> mappingRequest1,
            FocalMappingEvaluationRequest<?, ?> mappingRequest2) {
        VariableBindingDefinitionType target = mappingRequest2.getTarget();
        if (target == null || target.getPath() == null) {
            return false;
        }
        ItemPath targetPath = target.getPath().getItemPath().stripVariableSegment();

        for (VariableBindingDefinitionType source : mappingRequest1.getSources()) {
            ItemPath sourcePath = beans.prismContext.toPath(source.getPath());
            if (sourcePath != null && FocalMappingSetEvaluation.stripFocusVariableSegment(sourcePath).equivalent(targetPath)) {
                return true;
            }
        }
        return false;
    }
}
