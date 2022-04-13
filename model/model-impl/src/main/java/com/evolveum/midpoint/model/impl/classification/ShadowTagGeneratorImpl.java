/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.classification;

import static com.evolveum.midpoint.model.impl.ResourceObjectProcessingContextImpl.ResourceObjectProcessingContextBuilder.aResourceObjectProcessingContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.provisioning.api.ShadowTagGenerator;
import com.evolveum.midpoint.schema.processor.RefinedDefinitionUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
public class ShadowTagGeneratorImpl implements ShadowTagGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowTagGeneratorImpl.class);

    private static final String OP_GENERATE = ResourceObjectClassifierImpl.class.getName() + ".generate";

    @Autowired private ModelBeans beans;

    @PostConstruct
    void initialize() {
        beans.provisioningService.setShadowTagGenerator(this);
    }

    @PreDestroy
    void destroy() {
        beans.provisioningService.setShadowTagGenerator(null);
    }

    @Override
    public @Nullable String generateTag(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectTypeDefinition definition,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_GENERATE)
                .addParam("combinedObject", combinedObject)
                .addParam("resource", resource)
                .build();
        try {
            ResourceObjectProcessingContext context = aResourceObjectProcessingContext(combinedObject, resource, task, beans)
                    .withSystemConfiguration(
                            beans.systemObjectCache.getSystemConfigurationBean(result))
                    .build();
            return generateTagInternal(context, definition, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public @Nullable String generateTag(
            @NotNull ResourceObjectProcessingContext context,
            @NotNull ResourceObjectTypeDefinition definition,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_GENERATE)
                .addParam("combinedObject", context.getShadowedResourceObject())
                .addParam("resource", context.getResource())
                .build();
        try {
            return generateTagInternal(context, definition, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private String generateTagInternal(
            @NotNull ResourceObjectProcessingContext context,
            @NotNull ResourceObjectTypeDefinition definition,
            @NotNull OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ResourceObjectMultiplicityType multiplicity = definition.getObjectMultiplicity();
        if (!RefinedDefinitionUtil.isMultiaccount(multiplicity)) {
            result.recordNotApplicable();
            return null;
        }
        String tag = beans.synchronizationExpressionsEvaluator.generateTag(
                multiplicity,
                context.getShadowedResourceObject(),
                context.getResource(),
                context.getSystemConfiguration(),
                "tag expression for " + context.getShadowedResourceObject(),
                context.getTask(),
                result);
        LOGGER.debug("SYNCHRONIZATION: TAG generated: {}", tag);
        return tag;
    }
}
