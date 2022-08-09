/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import java.util.Collection;

import com.evolveum.midpoint.model.api.correlator.CandidateOwnerMap;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult.OwnersInfo;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;

/**
 * Helps with creating the {@link CorrelationResult} for some correlators.
 *
 * TODO better name
 */
@Component
public class BuiltInResultCreator {

    @Autowired private ExpressionFactory expressionFactory;

    public <F extends FocusType> CorrelationResult createCorrelationResult(
            @NotNull Collection<F> candidates,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (candidates.isEmpty()) {
            return CorrelationResult.noOwner();
        }

        OwnersInfo ownersInfo = createOwnersInfo(candidates, correlatorContext, task, result);
        if (candidates.size() == 1) {
            return CorrelationResult.existingOwner(candidates.iterator().next(), ownersInfo);
        } else {
            return CorrelationResult.uncertain(ownersInfo);
        }
    }

    private <F extends FocusType> OwnersInfo createOwnersInfo(
            Collection<F> candidates, CorrelatorContext<?> correlatorContext, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ResourceObjectOwnerOptionsType options = new ResourceObjectOwnerOptionsType();
        CandidateOwnerMap<F> candidateOwnerMap = new CandidateOwnerMap<>();
        for (F candidate : candidates) {
            Double confidence = determineConfidence(candidate, correlatorContext, task, result);
            options.getOption().add(
                    createOwnerOption(candidate, confidence));
            candidateOwnerMap.put(candidate, confidence);
        }
        options.getOption().add(
                createOwnerOption(null, null));
        return new OwnersInfo(candidateOwnerMap, options, candidates);
    }

    private <F extends FocusType> Double determineConfidence(
            F candidate, CorrelatorContext<?> correlatorContext, Task task, OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        CorrelationConfidenceDefinitionType confidenceDef = correlatorContext.getConfigurationBean().getConfidence();
        if (confidenceDef == null) {
            return null;
        }
        Double value = confidenceDef.getValue();
        if (value != null) {
            return value;
        }
        ExpressionType expressionBean = confidenceDef.getExpression();
        if (expressionBean != null) {
            VariablesMap variablesMap = new VariablesMap();
            variablesMap.put(ExpressionConstants.VAR_CANDIDATE, new TypedValue<>(candidate, String.class));
            PrismPropertyDefinition<Double> outputDefinition =
                    PrismContext.get().definitionFactory().createPropertyDefinition(
                            ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_DOUBLE);
            PrismValue output = ExpressionUtil.evaluateExpression(
                    variablesMap,
                    outputDefinition,
                    expressionBean,
                    MiscSchemaUtil.getExpressionProfile(),
                    expressionFactory,
                    "confidence expression for " + candidate,
                    task,
                    result);
            if (output == null) {
                return null;
            } else {
                return JavaTypeConverter.convert(
                        Double.class,
                        output.getRealValue());
            }
        }
        throw new ConfigurationException("No value nor expression specified for confidence in " + correlatorContext);
    }

    private ResourceObjectOwnerOptionType createOwnerOption(@Nullable FocusType candidate, @Nullable Double confidence) {
        OwnerOptionIdentifier identifier = candidate != null ?
                OwnerOptionIdentifier.forExistingOwner(candidate.getOid()) : OwnerOptionIdentifier.forNoOwner();
        return new ResourceObjectOwnerOptionType()
                .identifier(identifier.getStringValue())
                .candidateOwnerRef(ObjectTypeUtil.createObjectRef(candidate))
                .confidence(confidence);
    }
}
