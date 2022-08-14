/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseCorrelationContextType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.CaseEngine;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.impl.engine.CaseBeans;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCaseSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * A bridge between the {@link CaseEngine} and the correlation business.
 *
 * Does not deal with technical details of correlation. These are delegated e.g. to {@link CorrelationService}.
 */
@Component
public class CorrelationCaseEngineExtension extends DefaultEngineExtension {

    @Autowired private CorrelationService correlationService;

    @Autowired
    public CorrelationCaseEngineExtension(CaseBeans beans) {
        super(beans);
    }

    @Override
    public @NotNull Collection<String> getArchetypeOids() {
        return List.of(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value());
    }

    @Override
    protected SimpleCaseSchemaType getCaseSchema(@NotNull CaseEngineOperation operation) {
        CaseCorrelationContextType context = operation.getCurrentCase().getCorrelationContext();
        return context != null ? context.getSchema() : null;
    }

    @Override
    public void finishCaseClosing(
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException {

        correlationService.completeCorrelationCase(
                operation.getCurrentCase(),
                operation::closeCaseInRepository,
                operation.getTask(),
                result);
    }
}
