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
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DefaultInboundMappingEvaluationPhasesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhasesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;

/**
 * Determines applicability of a mapping in given evaluation phase.
 */
class ApplicabilityEvaluator {

    private static final InboundMappingEvaluationPhaseType[] DEFAULT_PHASES =
            new InboundMappingEvaluationPhaseType[] { InboundMappingEvaluationPhaseType.CLOCKWORK };

    /** Default phases for mappings evaluation. */
    @NotNull private final Collection<InboundMappingEvaluationPhaseType> defaultPhases;

    /** Current phase in which we are going to evaluate the mappings in question. */
    @NotNull private final InboundMappingEvaluationPhaseType currentPhase;

    ApplicabilityEvaluator(
            @Nullable DefaultInboundMappingEvaluationPhasesType defaultPhasesConfiguration,
            @NotNull InboundMappingEvaluationPhaseType currentPhase) {
        this.defaultPhases = getDefaultPhases(defaultPhasesConfiguration);
        this.currentPhase = currentPhase;
    }

    private static @NotNull Collection<InboundMappingEvaluationPhaseType> getDefaultPhases(
            @Nullable DefaultInboundMappingEvaluationPhasesType defaultPhasesConfiguration) {
        if (defaultPhasesConfiguration == null) {
            return Set.of(DEFAULT_PHASES);
        } else {
            return new HashSet<>(defaultPhasesConfiguration.getPhase());
        }
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
        return defaultPhases.contains(currentPhase);
    }
}
