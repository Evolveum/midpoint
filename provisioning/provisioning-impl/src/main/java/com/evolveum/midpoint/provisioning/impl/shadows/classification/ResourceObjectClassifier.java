/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.classification;

import static com.evolveum.midpoint.provisioning.impl.shadows.classification.ClassificationContext.Builder.aClassificationContext;
import static com.evolveum.midpoint.schema.util.ShadowUtil.getObjectClassRequired;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.Resource;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classifies a resource object, i.e. determines its type (kind + intent).
 *
 * == Expected use
 *
 * Currently, this functionality is invoked during shadow acquisition process, or - as the last instance - during
 * the synchronization process in the `model` module.
 *
 * The classification uses object type delineation. Currently, the implementation is limited in the sense that it assumes
 * non-overlapping, and completely specified sets of resource objects. (I.e. no "default" flags there.) TODO this is no longer true
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
            result.computeStatusIfUnknown();
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

        public ResourceObjectClassification execute(OperationResult result) throws SchemaException, ExpressionEvaluationException,
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
                if (ShadowUtil.isKnown(kind) && ShadowUtil.isKnown(intent)) {
                    // We are interested in _type_ definition because we want to classify the shadow (by assigning kind/intent).
                    typeDefinition = schema.getObjectTypeDefinition(kind, intent);
                } else {
                    // We don't accept partial sorter results (like kind known, intent unknown, or vice versa).
                    // TODO Shouldn't we try the default classification here?
                    typeDefinition = null;
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
         * Temporary code, based on conditions only. Here should be more sophisticated treatment,
         * using e.g. base context with filter(s).
         */
        private @Nullable ResourceObjectTypeDefinition classifyResourceObject(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            ShadowType shadow = context.getShadowedResourceObject();
            LOGGER.trace("Classifying {}", shadow);

            List<SynchronizationPolicy> allMatchingPolicies = findAllMatchingPolicies(result);

            SynchronizationPolicy defaultPolicy = selectDefaultPolicy(allMatchingPolicies);
            if (defaultPolicy != null) {
                LOGGER.debug("Default type matched for {}: {}", shadow, defaultPolicy);
                return defaultPolicy.getResourceTypeDefinitionRequired();
            }

            if (allMatchingPolicies.size() > 1) {
                return selectFromMatchingNonDefaultPolicies(allMatchingPolicies);
            } else if (allMatchingPolicies.size() == 1) {
                ResourceObjectTypeDefinition theOne = allMatchingPolicies.get(0).getResourceTypeDefinitionRequired();
                LOGGER.debug("Exactly one type definition matched {}: {}", shadow, theOne);
                return theOne;
            } else {
                LOGGER.debug("No type definition matched {}", shadow);
                return null;
            }
        }

        /**
         * Note that returning definitions would be sufficient in normal cases.
         * But we need to inspect some configuration details that are available only through
         * {@link SynchronizationPolicy}, so be it. The policy is connected to {@link ResourceObjectTypeDefinition},
         * not to {@link ResourceObjectClassDefinition}.
         */
        private @NotNull List<SynchronizationPolicy> findAllMatchingPolicies(OperationResult result)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {
            ShadowType shadow = context.getShadowedResourceObject();
            List<SynchronizationPolicy> matching = new ArrayList<>();

            for (ResourceObjectTypeDefinition typeDefinition : schema.getObjectTypeDefinitions()) {
                SynchronizationPolicy policy = SynchronizationPolicyFactory.forKindAndIntentStrictlyRequired(
                        typeDefinition.getKind(), typeDefinition.getIntent(), context.getResource());
                assert policy.getResourceObjectDefinition() instanceof ResourceObjectTypeDefinition;

                LOGGER.trace("Trying applicability of {}", policy);
                if (!policy.isObjectClassNameMatching(getObjectClassRequired(shadow))) {
                    LOGGER.trace(" -> it's not applicable to the shadow because of object class name mismatch");
                    continue;
                }

                DelineationMatcher matcher = new DelineationMatcher(
                        policy.getDelineation(), policy.getResourceObjectDefinition(), context);
                if (!matcher.matches(result)) {
                    LOGGER.trace(" -> delineation does not match");
                    continue;
                }

                LOGGER.trace("Adding {} to a list of potential matches for {}", policy, shadow);
                matching.add(policy);

                // We might consider stopping the search on the first type marked as default.
                // But let's go through it all, and e.g. check multiple defaults.
                // (Although that should be already checked on schema parse... anyway.)
            }
            return matching;
        }

        private SynchronizationPolicy selectDefaultPolicy(List<SynchronizationPolicy> matching) {
            var matchingDefault = matching.stream()
                    .filter(policy -> policy.getResourceTypeDefinitionRequired().isDefaultForObjectClass())
                    .collect(Collectors.toList());
            return MiscUtil.extractSingleton(
                    matchingDefault,
                    () -> new IllegalStateException(
                            "Multiple types marked as 'default for object class': " + matchingDefault));
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
                    return policy.getResourceTypeDefinitionRequired();
                }
            }

            ResourceObjectTypeDefinition first = matching.get(0).getResourceTypeDefinitionRequired();
            LOGGER.debug("Returning the first one: {}", first);
            return first;
        }
    }
}
