/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhaseType.BEFORE_CORRELATION;

/**
 * Determines applicability of a mapping in given evaluation phase.
 */
class ApplicabilityEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ApplicabilityEvaluator.class);

    private static final List<InboundMappingEvaluationPhaseType> DEFAULT_PHASES =
            List.of(InboundMappingEvaluationPhaseType.CLOCKWORK);

    /** Default phases for mappings evaluation. */
    @NotNull private final Collection<InboundMappingEvaluationPhaseType> defaultPhases;

    /** Current phase in which we are going to evaluate the mappings in question. */
    @NotNull private final InboundMappingEvaluationPhaseType currentPhase;

    /** Focus items for which the correlation is defined. Only for "before clockwork" phase. */
    @NotNull private final Collection<ItemPath> correlationItemPaths;

    ApplicabilityEvaluator(
            @Nullable DefaultInboundMappingEvaluationPhasesType defaultPhasesConfiguration,
            boolean resourceItemCorrelationDefined,
            @NotNull Collection<ItemPath> correlationItemPaths,
            @NotNull InboundMappingEvaluationPhaseType currentPhase) {
        this.defaultPhases = new HashSet<>(
                defaultPhasesConfiguration != null ? defaultPhasesConfiguration.getPhase() : DEFAULT_PHASES);
        if (resourceItemCorrelationDefined) {
            defaultPhases.add(BEFORE_CORRELATION);
        }
        this.currentPhase = currentPhase;
        this.correlationItemPaths = correlationItemPaths;
    }

    List<InboundMappingType> filterApplicableMappingBeans(List<InboundMappingType> beans) {
        return beans.stream()
                .filter(this::isApplicable)
                .collect(Collectors.toList());
    }

    private boolean isApplicable(@NotNull InboundMappingType mappingBean) {
        InboundMappingEvaluationPhasesType mappingPhases = mappingBean.getEvaluationPhases();
        if (mappingPhases != null) {
            if (mappingPhases.getExclude().contains(currentPhase)) {
                return false;
            } else if (mappingPhases.getInclude().contains(currentPhase)) {
                return true;
            }
        }

        if (defaultPhases.contains(currentPhase)) {
            return true;
        }

        if (currentPhase == BEFORE_CORRELATION && targetIsUsedForCorrelation(mappingBean)) {
            LOGGER.trace("Mapping is applicable because its target is a correlation item");
            return true;
        }

        return false;
    }

    private boolean targetIsUsedForCorrelation(InboundMappingType mappingBean) {
        VariableBindingDefinitionType target = mappingBean.getTarget();
        if (target == null) {
            return false;
        }
        // Note that we ignore the variable in the path. Currently, it must point to the focus anyway.
        ItemPathType path = target.getPath();
        return path != null
                && correlationItemPaths.contains(path.getItemPath().stripVariableSegment());
    }
}
