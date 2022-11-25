/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.classification;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrimitiveType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.RefinedDefinitionUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO Currently only used during classification and synchronization.
 */
@Component
public class ShadowTagGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowTagGenerator.class);

    private static final String OP_GENERATE = ShadowTagGenerator.class.getName() + ".generate";

    @Autowired private CommonBeans beans;
    @Autowired private SystemObjectCache systemObjectCache;

    /**
     * Generates a tag for the shadow.
     *
     * @param shadow Resource object that we want to generate the tag for. TODO
     * @param resource Resource on which the resource object was found
     * @param definition Object type definition for the shadow. Included for performance reasons (we assume the client knows it).
     */
    public @Nullable String generateTag(
            @NotNull ShadowType shadow,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition definition,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_GENERATE)
                .addParam("shadow", shadow)
                .addParam("resource", resource)
                .build();
        try {
            ResourceObjectMultiplicityType multiplicity = definition.getObjectMultiplicity();
            if (!RefinedDefinitionUtil.isMultiaccount(multiplicity)) {
                result.recordNotApplicable();
                return null;
            }

            ShadowTagSpecificationType tagSpec = multiplicity.getTag();
            ExpressionType expressionBean = tagSpec != null ? tagSpec.getExpression() : null;
            if (expressionBean == null) {
                String tag = shadow.getOid();
                LOGGER.debug("SYNCHRONIZATION: TAG derived from shadow OID: {}", tag);
                return tag;
            } else {
                VariablesMap variables = ExpressionUtil.getDefaultVariablesMap(
                        null,
                        shadow,
                        resource,
                        systemObjectCache.getSystemConfigurationBean(result));
                ItemDefinition<?> outputDefinition = PrismContext.get().definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, PrimitiveType.STRING.getQname());
                try {
                    String shortDesc = "tag expression for " + shadow;
                    ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(task, result);
                    PrismPropertyValue<String> tagProp = ExpressionUtil.evaluateExpression(
                            variables,
                            outputDefinition,
                            expressionBean,
                            MiscSchemaUtil.getExpressionProfile(),
                            beans.expressionFactory,
                            shortDesc,
                            task,
                            result);
                    String tag = getRealValue(tagProp);
                    LOGGER.debug("SYNCHRONIZATION: TAG generated: {}", tag);
                    return tag;
                } finally {
                    ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }
}
