/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhasesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingUseType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingUseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingUseType.ALL;

public class InboundMappingConfigItem
        extends ConfigurationItem<InboundMappingType>
        implements AbstractMappingConfigItem<InboundMappingType> {

    @SuppressWarnings("unused") // called dynamically
    public InboundMappingConfigItem(@NotNull ConfigurationItem<? extends InboundMappingType> original) {
        super(original);
    }

    public Boolean determineApplicability(@NotNull InboundMappingEvaluationPhaseType currentPhase) throws ConfigurationException {
        InboundMappingEvaluationPhasesType mappingPhases = value().getEvaluationPhases();
        InboundMappingUseType use = value().getUse();
        configCheck(mappingPhases == null || use == null,
                "Both 'evaluationPhases' and 'use' items present in %s", DESC);
        if (mappingPhases != null) {
            if (mappingPhases.getExclude().contains(currentPhase)) {
                return false;
            } else if (mappingPhases.getInclude().contains(currentPhase)) {
                return true;
            }
        } else if (use != null) {
            // The "use" information is definite, if present. Default phases nor correlation usage are not taken into account.
            return switch (currentPhase) {
                case BEFORE_CORRELATION -> use == CORRELATION || use == ALL;
                case CLOCKWORK -> use == SYNCHRONIZATION || use == ALL;
            };
        }
        return null; // no definite answer
    }
}
