/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import org.jetbrains.annotations.NotNull;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
public class TemplateMappingEvaluationRequest extends FocalMappingEvaluationRequest<ObjectTemplateMappingType, ObjectTemplateType> {

    public TemplateMappingEvaluationRequest(@NotNull ObjectTemplateMappingType mapping,
            @NotNull ObjectTemplateType objectTemplate) {
        super(mapping, MappingKindType.TEMPLATE, objectTemplate);
    }

    @Override
    public ObjectTemplateMappingEvaluationPhaseType getEvaluationPhase() {
        return defaultIfNull(mapping.getEvaluationPhase(), ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("template mapping ");
        sb.append("'").append(getMappingInfo()).append("' in ").append(originObject);
    }
}
