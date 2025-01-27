/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.classification;

import static com.evolveum.midpoint.provisioning.impl.shadows.classification.ClassificationContext.Builder.aClassificationContext;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicyFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Classifies a resource object, i.e. determines its type (kind + intent).
 *
 * == Expected use
 *
 * Currently, this functionality is invoked
 *
 * 1. during shadow acquisition process (during search, live sync, or async update operation),
 * 2. after a resource object is fetched as part of {@link ProvisioningService#getObject(Class, String, Collection, Task,
 * OperationResult)} call,
 * 3. or - as the last instance - during the synchronization process in the `model` module.
 *
 * == Algorithm
 *
 * The classification uses object type delineation, i.e. object class name, base context, filters, and condition.
 * Each delineation has "correlation order", driving the sequence in which it is tried to be matched:
 *
 * 1. First, all types with ordered delineations are tried, in the order specified. The first matching one is returned.
 * 2. Then, all others are tried. If a default one (or only one) is found, it is returned immediately.
 * 3. Otherwise, the first one with legacy `synchronization` spec if returned.
 * 4. If there's none, the first one is returned.
 *
 * The points 3 & 4 above are a bit non-deterministic, because there is no order guaranteed. Maybe we should
 * re-think the behavior in such a case (e.g. by not returning anything).
 *
 * See {@link ClassificationProcess#classifyResourceObject(OperationResult)}.
 */
@Component
public class ResourceObjectClassifier {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectClassifier.class);

    private static final String OP_CLASSIFY = ResourceObjectClassifier.class.getName() + ".classify";

    @Autowired private ProvisioningServiceImpl provisioningService;
    @Autowired private CommonBeans beans;

    /**
     * Classifies the shadowed resource object.
     *
     * @param combinedObject Resource object that we want to classify. It should be connected to the shadow,
     * however, exact "shadowization" is not required. Currently it should contain all the information from the shadow,
     * plus all the attributes from resource object. If needed, more elaborate processing (up to full shadowization)
     * can be added later.
     *
     * @param resource Resource on which the resource object was found
     */
    public @NotNull ResourceObjectClassification classify(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OP_CLASSIFY)
                .addParam("combinedObject", combinedObject)
                .addParam("resource", resource)
                .build();
        try {
            ClassificationContext context = aClassificationContext(combinedObject, resource, task, beans)
                    .withSystemConfiguration(
                            beans.systemObjectCache.getSystemConfigurationBean(result))
                    .build();
            return new ClassificationProcess(context, existingSorterResult)
                    .execute(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private class ClassificationProcess {

        @NotNull private final ClassificationContext context;
        @NotNull private final ResourceSchema schema;
        @Nullable private final ObjectSynchronizationDiscriminatorType existingSorterResult;

        ClassificationProcess(
                @NotNull ClassificationContext context,
                @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult)
                throws SchemaException, ConfigurationException {
            this.context = context;
            this.schema = Resource.of(context.getResource()).getCompleteSchemaRequired();
            this.existingSorterResult = existingSorterResult;
        }

        ResourceObjectClassification execute(OperationResult result) throws SchemaException, ExpressionEvaluationException,
                CommunicationException, ConfigurationException, ObjectNotFoundException, SecurityViolationException {

            // Just in case the definition is missing (normally it's already present). See MID-7236.
            // Note that even if we usually don't know the type (kind/intent), we know the object class,
            // so we apply the definition from it.
            //
            // TODO what about potential re-classification of a shadow? But this is probably not done; even
            //  shadow integrity checker code seems to be faulty in this regard (it does not even classify "unknown" shadows)
            provisioningService.applyDefinition(
                    context.getShadowedResourceObject().asPrismObject(),
                    context.getTask(),
                    result);

            ObjectSynchronizationDiscriminatorType sorterResult = evaluateSorterIfNeeded(result);

            ResourceObjectTypeDefinition typeDefinition;
            if (sorterResult != null) {
                ShadowKindType kind = sorterResult.getKind();
                String intent = sorterResult.getIntent();
                if (ShadowUtil.isClassified(kind, intent)) {
                    // We are interested in _type_ definition because we want to classify the shadow (by assigning kind/intent).
                    typeDefinition = schema.getObjectTypeDefinition(kind, intent);
                    if (typeDefinition == null) {
                        throw new ConfigurationException(
                                String.format("Synchronization sorter provided classification of %s/%s for which there's "
                                        + "no type definition in %s", kind, intent, context.getResource()));
                    }
                } else {
                    // We don't accept partial sorter results (like kind known, intent unknown, or vice versa).
                    typeDefinition = classifyResourceObject(result);
                    LOGGER.warn("Synchronization sorter provided incomplete classification ({}/{}), the standard classification "
                            + "process was used, and yielded: {}", kind, intent, typeDefinition);
                }
            } else {
                typeDefinition = classifyResourceObject(result);
            }

            return ResourceObjectClassification.of(typeDefinition);
        }

        private ObjectSynchronizationDiscriminatorType evaluateSorterIfNeeded(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            if (existingSorterResult != null) {
                return existingSorterResult;
            } else {
                return provisioningService.getSynchronizationSorterEvaluator().evaluate(
                        context.getShadowedResourceObject(),
                        context.getResource(),
                        context.getTask(),
                        result);
            }
        }

        /**
         * The algorithm is described in class-level javadoc.
         */
        private @Nullable ResourceObjectTypeDefinition classifyResourceObject(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ShadowType shadow = context.getShadowedResourceObject();
            LOGGER.debug("Classifying {}", shadow);

            List<SynchronizationPolicy> orderedPolicies = new ArrayList<>();
            List<SynchronizationPolicy> unorderedPolicies = new ArrayList<>();
            for (ResourceObjectTypeDefinition typeDefinition : schema.getObjectTypeDefinitions()) {
                SynchronizationPolicy policy =
                        SynchronizationPolicyFactory.forTypeDefinition(typeDefinition, context.getResource());
                if (policy.getClassificationOrder() != null) {
                    orderedPolicies.add(policy);
                } else {
                    unorderedPolicies.add(policy);
                }
            }
            orderedPolicies.sort(
                    Comparator.comparing(
                            SynchronizationPolicy::getClassificationOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())));

            LOGGER.trace("Trying ordered policies");
            for (SynchronizationPolicy policy : orderedPolicies) {
                if (matches(policy, result)) {
                    ResourceObjectTypeDefinition typeDef = policy.getObjectTypeDefinition();
                    LOGGER.debug("Matched (ordered) type definition for {}: {}", shadow, typeDef);
                    return typeDef;
                }
            }

            LOGGER.trace("Trying unordered policies");
            List<SynchronizationPolicy> matchingUnorderedPolicies = new ArrayList<>();
            for (SynchronizationPolicy policy : unorderedPolicies) {
                if (matches(policy, result)) {
                    if (policy.isDefaultForClassification()) {
                        ResourceObjectTypeDefinition typeDef = policy.getObjectTypeDefinition();
                        LOGGER.debug("Matched default type definition for {}: {}", shadow, typeDef);
                        return typeDef;
                    } else {
                        matchingUnorderedPolicies.add(policy);
                    }
                }
            }

            if (matchingUnorderedPolicies.size() > 1) {
                return selectFromMatchingNonDefaultPolicies(matchingUnorderedPolicies);
            } else if (matchingUnorderedPolicies.size() == 1) {
                ResourceObjectTypeDefinition theOne = matchingUnorderedPolicies.get(0).getObjectTypeDefinition();
                LOGGER.debug("Exactly one type definition matched {}: {}", shadow, theOne);
                return theOne;
            } else {
                LOGGER.debug("No type definition matched {}", shadow);
                return null;
            }
        }

        private boolean matches(SynchronizationPolicy policy, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            DelineationMatcher matcher = new DelineationMatcher(policy.getDelineation(), context);
            return matcher.matches(result);
        }

        /**
         * This is unfortunate case where we have multiple matching non-default policies.
         * Some heuristics are needed.
         */
        private ResourceObjectTypeDefinition selectFromMatchingNonDefaultPolicies(List<SynchronizationPolicy> matching) {
            ShadowType shadow = context.getShadowedResourceObject();
            LOGGER.warn("Multiple object types matching {}, trying to determine the best one: {}", shadow, matching);

            // Before 4.6, the "synchronization" section presence was required to use the definition as a candidate
            // for classification (see e.g. TestCaseIgnore). So let's try to use this as a criterion.
            for (SynchronizationPolicy policy : matching) {
                if (policy.hasLegacyConfiguration()) {
                    LOGGER.debug("Returning the first type with legacy sync policy for {}: {}", shadow, policy);
                    return policy.getObjectTypeDefinition();
                }
            }

            // TODO or should we return nothing?
            ResourceObjectTypeDefinition first = matching.get(0).getObjectTypeDefinition();
            LOGGER.debug("Returning the first one: {}", first);
            return first;
        }
    }
}
