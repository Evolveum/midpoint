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

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Collection;

@Component
public class ResourceObjectClassifierImpl implements ResourceObjectClassifier {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectClassifierImpl.class);

    private static final String OP_CLASSIFY = ResourceObjectClassifierImpl.class.getName() + ".classify";

    @Autowired private ProvisioningService provisioningService;
    @Autowired private ModelBeans beans;

    @PostConstruct
    void initialize() {
        provisioningService.setResourceObjectClassifier(this);
    }

    @PreDestroy
    void destroy() {
        provisioningService.setResourceObjectClassifier(null);
    }

    @Override
    public @NotNull Classification classify(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OP_CLASSIFY)
                .addParam("combinedObject", combinedObject)
                .addParam("resource", resource)
                .build();
        try {
            ResourceObjectProcessingContext context = aResourceObjectProcessingContext(combinedObject, resource, task, beans)
                    .withSystemConfiguration(
                            beans.systemObjectCache.getSystemConfigurationBean(result))
                    .build();
            return new ClassificationProcess(context, null)
                    .execute(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Carries out the classification of given shadow (present in processing context).
     *
     * @param existingSorterResult Reasonably fresh result of sorter evaluation (to avoid double evaluation of the sorter)
     */
    public @NotNull Classification classify(
            @NotNull ResourceObjectProcessingContext context,
            @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.subresult(OP_CLASSIFY)
                .addParam("combinedObject", context.getShadowedResourceObject())
                .addParam("resource", context.getResource())
                .build();
        try {
            return new ClassificationProcess(context, existingSorterResult)
                    .execute(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private class ClassificationProcess {

        @NotNull private final ResourceObjectProcessingContext context;
        @NotNull private final ResourceSchema schema;
        @Nullable private final ObjectSynchronizationDiscriminatorType existingSorterResult;

        ClassificationProcess(
                @NotNull ResourceObjectProcessingContext context,
                @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult)
                throws SchemaException, ConfigurationException {
            this.context = context;
            this.schema = ResourceSchemaFactory.getCompleteSchemaRequired(context.getResource());
            this.existingSorterResult = existingSorterResult;
        }

        public Classification execute(OperationResult result) throws SchemaException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException, ObjectNotFoundException, SecurityViolationException {

            // Just in case the definition is missing (normally it's already present). See MID-7236.
            provisioningService.applyDefinition(
                    context.getShadowedResourceObject().asPrismObject(),
                    context.getTask(),
                    result);

            ObjectSynchronizationDiscriminatorType sorterResult =
                    existingSorterResult != null ?
                            existingSorterResult :
                            new SynchronizationSorterEvaluation(context)
                                    .evaluate(result);

            ResourceObjectTypeDefinition typeDefinition;
            if (sorterResult != null) {
                ShadowKindType kind = sorterResult.getKind();
                String intent = sorterResult.getIntent();
                if (ShadowUtil.isKnown(kind) && ShadowUtil.isKnown(intent)) {
                    // We are interested in _type_ definition because we want to classify the shadow (by assigning kind/intent).
                    typeDefinition = schema.findObjectTypeDefinition(kind, intent);
                } else {
                    // We don't accept partial sorter results (like kind known, intent unknown, or vice versa).
                    // TODO Shouldn't we try the default classification here?
                    typeDefinition = null;
                }
            } else {
                typeDefinition = classifyResourceObject(result);
            }

            if (typeDefinition == null) {
                return Classification.unknown();
            } else {
                return Classification.of(typeDefinition);
            }
        }

        /**
         * Temporary code, based on conditions only. Here should be more sophisticated treatment,
         * using e.g. base context with filter(s).
         */
        private @Nullable ResourceObjectTypeDefinition classifyResourceObject(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ShadowType shadow = context.getShadowedResourceObject();

            Collection<SynchronizationPolicy> allPolicies =
                    schema.getAllSynchronizationPolicies(context.getResource());

            for (SynchronizationPolicy policy : allPolicies) {
                if (!policy.isSynchronizationEnabled()) { // FIXME TEMPORARY!! (we should probably classify even without sync enabled?)
                    LOGGER.trace("Policy {} is not enabled for synchronization", policy);
                    continue;
                }
                if (!policy.isApplicableToShadow(shadow)) {
                    LOGGER.trace("Policy {} is not applicable to {}", policy, shadow);
                    continue;
                }
                if (!isPolicyConditionTrue(policy, result)) {
                    LOGGER.trace("Condition of policy {} is evaluates to false for {}", policy, shadow);
                    continue;
                }
                ResourceObjectDefinition definition = policy.getResourceObjectDefinition();
                if (!(definition instanceof ResourceObjectTypeDefinition)) {
                    LOGGER.debug("Couldn't classify {} as {} (not a type definition)", shadow, definition);
                    continue;
                }

                LOGGER.debug("Classified {} as {}", shadow, definition);
                return (ResourceObjectTypeDefinition) definition;
            }

            LOGGER.debug("No type definition matched {}", shadow);
            return null;
        }

        private boolean isPolicyConditionTrue(
                @NotNull SynchronizationPolicy policy,
                @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
                ConfigurationException, SecurityViolationException {
            ExpressionType conditionExpressionBean = policy.getClassificationCondition();
            if (conditionExpressionBean == null) {
                return true;
            }
            String desc = "condition in object synchronization";
            try {
                Task task = context.getTask();
                ModelExpressionThreadLocalHolder.pushExpressionEnvironment(
                        new ExpressionEnvironment<>(task, result));
                ExpressionFactory expressionFactory = beans.expressionFactory;
                return ExpressionUtil.evaluateConditionDefaultTrue(
                        context.createVariablesMap(),
                        conditionExpressionBean,
                        MiscSchemaUtil.getExpressionProfile(),
                        expressionFactory,
                        desc,
                        task,
                        result);
            } finally {
                ModelExpressionThreadLocalHolder.popExpressionEnvironment();
            }
        }
    }
}
