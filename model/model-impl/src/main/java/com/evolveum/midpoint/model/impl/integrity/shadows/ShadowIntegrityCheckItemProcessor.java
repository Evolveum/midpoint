/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

class ShadowIntegrityCheckItemProcessor {

    private static final String CLASS_DOT = ShadowIntegrityCheckItemProcessor.class.getName() + ".";
    static final String KEY_EXISTS_ON_RESOURCE = CLASS_DOT + "existsOnResource";
    static final String KEY_OWNERS = CLASS_DOT + "owners";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowIntegrityCheckItemProcessor.class);

    @NotNull private final ShadowIntegrityCheckActivityRun activityRun;
    @NotNull private final PrismContext prismContext;

    ShadowIntegrityCheckItemProcessor(@NotNull ShadowIntegrityCheckActivityRun activityRun) {
        this.activityRun = activityRun;
        this.prismContext = PrismContext.get();
    }

    public boolean processObject(PrismObject<ShadowType> shadow,
            RunningTask workerTask, OperationResult parentResult)
            throws CommonException {

        OperationResult result = parentResult.createMinorSubresult(CLASS_DOT + "processObject");
        ShadowCheckResult checkResult = new ShadowCheckResult(shadow);
        try {
            checkShadow(checkResult, shadow, workerTask, result);
            for (Exception e : checkResult.getErrors()) {
                result.createSubresult(CLASS_DOT + "handleObject.result").recordPartialError(e.getMessage(), e);
            }
            for (String message : checkResult.getWarnings()) {
                result.createSubresult(CLASS_DOT + "handleObject.result").recordWarning(message);
            }
            if (!checkResult.getErrors().isEmpty()) {
                getStats().incrementShadowsWithErrors();
            } else if (!checkResult.getWarnings().isEmpty()) {
                getStats().incrementShadowsWithWarnings();
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking shadow {} (resource {}) finished - errors: {}, warnings: {}",
                        ObjectTypeUtil.toShortString(checkResult.getShadow()),
                        ObjectTypeUtil.toShortString(checkResult.getResource()),
                        checkResult.getErrors().size(), checkResult.getWarnings().size());
            }
        } catch (RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error while checking shadow {} integrity", e, ObjectTypeUtil.toShortString(shadow));
            result.recordPartialError("Unexpected error while checking shadow integrity", e);
            getStats().incrementShadowsWithErrors();
        }

        getStats().registerProblemCodeOccurrences(checkResult.getProblemCodes());
        if (checkResult.isFixApplied()) {
            getStats().registerProblemsFixes(checkResult.getFixForProblems());
        }

        result.computeStatusIfUnknown();
        return true;
    }

    private void checkShadow(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) throws SchemaException {

        ShadowCheckConfiguration cfg = activityRun.getConfiguration();

        ShadowType shadowType = shadow.asObjectable();
        ObjectReferenceType resourceRef = shadowType.getResourceRef();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking shadow {} (resource {})", ObjectTypeUtil.toShortString(shadowType), resourceRef!=null?resourceRef.getOid():"(null)");
        }
        getStats().incrementShadows();

        if (resourceRef == null) {
            checkResult.recordError(ShadowStatistics.NO_RESOURCE_OID, new SchemaException("No resourceRef"));
            fixNoResourceIfRequested(checkResult, ShadowStatistics.NO_RESOURCE_OID);
            applyFixes(checkResult, shadow, workerTask, result);
            return;
        }
        String resourceOid = resourceRef.getOid();
        if (resourceOid == null) {
            checkResult.recordError(ShadowStatistics.NO_RESOURCE_OID, new SchemaException("Null resource OID"));
            fixNoResourceIfRequested(checkResult, ShadowStatistics.NO_RESOURCE_OID);
            applyFixes(checkResult, shadow, workerTask, result);
            return;
        }
        PrismObject<ResourceType> resource = getCachedResource(resourceOid);
        if (resource == null) {
            getStats().incrementResources();
            try {
                resource = getProvisioningService().getObject(ResourceType.class, resourceOid, null, workerTask, result);
            } catch (ObjectNotFoundException e) {
                checkResult.recordError(
                        ShadowStatistics.NO_RESOURCE, e.wrap("Resource definition does not exist"));
                fixNoResourceIfRequested(checkResult, ShadowStatistics.NO_RESOURCE);
                applyFixes(checkResult, shadow, workerTask, result);
                return;
            } catch (SchemaException e) {
                checkResult.recordError(
                        ShadowStatistics.CANNOT_GET_RESOURCE, new SchemaException("Resource definition has schema problems: " + e.getMessage(), e));
                return;
            } catch (CommonException|RuntimeException e) {
                checkResult.recordError(ShadowStatistics.CANNOT_GET_RESOURCE, new SystemException("Resource definition cannot be fetched for some reason: " + e.getMessage(), e));
                return;
            }
            cacheResource(resource);
        }
        checkResult.setResource(resource);

        ShadowKindType kind = shadowType.getKind();
        if (kind == null) {
            // TODO or simply assume account?
            checkResult.recordError(ShadowStatistics.NO_KIND_SPECIFIED, new SchemaException("No kind specified"));
            return;
        }

        if (cfg.checkExtraData) {
            checkOrFixShadowActivationConsistency(checkResult, shadow);
        }

        PrismObject<ShadowType> fetchedShadow = null;
        if (cfg.checkFetch) {
            fetchedShadow = fetchShadow(checkResult, shadow, workerTask, result);
            if (fetchedShadow != null) {
                shadow.setUserData(KEY_EXISTS_ON_RESOURCE, "true");
            }
        }

        if (cfg.checkOwners) {
            List<PrismObject<FocusType>> owners = activityRun.searchOwners(shadow, result);
            if (owners != null) {
                shadow.setUserData(KEY_OWNERS, owners);
                if (owners.size() > 1) {
                    checkResult.recordError(ShadowStatistics.MULTIPLE_OWNERS, new SchemaException("Multiple owners: " + owners));
                }
            }

            if (shadowType.getSynchronizationSituation() == SynchronizationSituationType.LINKED && (owners == null || owners.isEmpty())) {
                checkResult.recordError(ShadowStatistics.LINKED_WITH_NO_OWNER, new SchemaException("Linked shadow with no owner"));
            }
            if (shadowType.getSynchronizationSituation() != SynchronizationSituationType.LINKED && owners != null && !owners.isEmpty()) {
                checkResult.recordError(ShadowStatistics.NOT_LINKED_WITH_OWNER, new SchemaException("Shadow with an owner but not marked as linked (marked as "
                    + shadowType.getSynchronizationSituation() + ")"));
            }
        }

        // FIXME what about unknown intents?
        //  and about unknown kinds?
        String intent = shadowType.getIntent();
        if (cfg.checkIntents && (intent == null || intent.isEmpty())) {
            checkResult.recordWarning(ShadowStatistics.NO_INTENT_SPECIFIED, "None or empty intent");
        }
        if (cfg.fixIntents && (intent == null || intent.isEmpty())) {
            doFixIntent(checkResult, fetchedShadow, shadow, resource, workerTask, result);
        }

        QName objectClassName = shadowType.getObjectClass();
        if (objectClassName == null) {
            checkResult.recordError(ShadowStatistics.NO_OBJECT_CLASS_SPECIFIED, new SchemaException("No object class specified"));
            return;
        }

        ContextMapKey key = new ContextMapKey(resourceOid, objectClassName);
        ObjectTypeContext context = activityRun.getObjectTypeContext(key);
        if (context == null) {
            context = new ObjectTypeContext();
            context.setResource(resource);
            ResourceSchema refinedSchema;
            try {
                refinedSchema = ResourceSchemaFactory.getCompleteSchema(context.getResource(), LayerType.MODEL);
            } catch (ConfigurationException | SchemaException e) {
                checkResult.recordError(
                        ShadowStatistics.CANNOT_GET_REFINED_SCHEMA, new SchemaException("Couldn't derive resource schema: " + e.getMessage(), e));
                return;
            }
            if (refinedSchema == null) {
                checkResult.recordError(ShadowStatistics.NO_RESOURCE_REFINED_SCHEMA, new SchemaException("No resource schema"));
                return;
            }
            String shadowIntent = ShadowUtil.getIntent(shadow); // TODO what if shadow is already updated (re-classified)?
            ResourceObjectDefinition objectDefinition =
                    ShadowUtil.isKnown(shadowIntent) ?
                            refinedSchema.findObjectDefinition(kind, shadowIntent) :
                            refinedSchema.findDefaultDefinitionForKind(kind); // TODO this is really strange, fix this!
            var typeDefinition = objectDefinition != null ? objectDefinition.getTypeDefinition() : null;
            if (typeDefinition != null) {
                context.setObjectTypeDefinition(typeDefinition);
            } else {
                // TODO or warning only?
                checkResult.recordError(ShadowStatistics.NO_OBJECT_CLASS_REFINED_SCHEMA,
                        new SchemaException("No object type definition for kind=" + kind + ", intent=" + intent));
                return;
            }
            activityRun.putObjectTypeContext(key, context);
        }

        try {
            getProvisioningService().applyDefinition(shadow, workerTask, result);
        } catch (SchemaException|ObjectNotFoundException|CommunicationException|ConfigurationException|ExpressionEvaluationException e) {
            checkResult.recordError(
                    ShadowStatistics.OTHER_FAILURE, new SystemException("Couldn't apply definition to shadow from repo", e));
            return;
        }

        Set<ResourceAttributeDefinition<?>> identifiers = new HashSet<>();
        Collection<? extends ResourceAttributeDefinition<?>> primaryIdentifiers = context.getObjectTypeDefinition().getPrimaryIdentifiers();
        identifiers.addAll(primaryIdentifiers);
        identifiers.addAll(context.getObjectTypeDefinition().getSecondaryIdentifiers());

        PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            // might happen on unfinished shadows?
            checkResult.recordError(ShadowStatistics.OTHER_FAILURE, new SchemaException("No attributes container"));
            return;
        }

        for (ResourceAttributeDefinition<?> identifier : identifiers) {
            PrismProperty<String> property = attributesContainer.getValue().findProperty(identifier.getItemName());
            if (property == null || property.size() == 0) {
                checkResult.recordWarning(ShadowStatistics.OTHER_FAILURE, "No value for identifier " + identifier.getItemName());
                continue;
            }
            if (property.size() > 1) {
                // we don't expect multi-valued identifiers
                checkResult.recordError(
                        ShadowStatistics.OTHER_FAILURE, new SchemaException("Multi-valued identifier " + identifier.getItemName() + " with values " + property.getValues()));
                continue;
            }
            // size == 1
            String value = property.getValue().getValue();
            if (value == null) {
                checkResult.recordWarning(ShadowStatistics.OTHER_FAILURE, "Null value for identifier " + identifier.getItemName());
                continue;
            }
            if (cfg.checkUniqueness) {
                if (!cfg.checkDuplicatesOnPrimaryIdentifiersOnly || primaryIdentifiers.contains(identifier)) {
                    addIdentifierValue(context, identifier.getItemName(), value, shadow);
                }
            }
            if (cfg.checkNormalization) {
                doCheckNormalization(checkResult, identifier, value);
            }
        }

        applyFixes(checkResult, shadow, workerTask, result);
    }

    private void cacheResource(PrismObject<ResourceType> resource) {
        activityRun.cacheResource(resource);
    }

    private PrismObject<ResourceType> getCachedResource(String resourceOid) {
        return activityRun.getCachedResource(resourceOid);
    }

    private void applyFixes(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow, Task workerTask,
            OperationResult result) {
        if (checkResult.isFixByRemovingShadow() || checkResult.getFixDeltas().size() > 0) {
            try {
                applyFix(checkResult, shadow, workerTask, result);
                checkResult.setFixApplied(true);
            } catch (CommonException e) {
                checkResult.recordError(ShadowStatistics.CANNOT_APPLY_FIX, new SystemException("Couldn't apply the shadow fix", e));
            }
        }
    }

    private void fixNoResourceIfRequested(ShadowCheckResult checkResult, String problemCode) {
        if (getConfiguration().fixResourceRef) {
            checkResult.setFixByRemovingShadow(problemCode);
        }
    }

    private PrismObject<ShadowType> fetchShadow(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow,
            Task task, OperationResult result) {
        try {
            return getProvisioningService().getObject(ShadowType.class, shadow.getOid(),
                    // TODO consider using forced exception mode as well
                    SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()),
                    task, result);
        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException | RuntimeException | Error e) {
            checkResult.recordError(ShadowStatistics.CANNOT_FETCH_RESOURCE_OBJECT,
                    new SystemException("The resource object couldn't be fetched", e));
            return null;
        }
    }

    private void doFixIntent(
            ShadowCheckResult checkResult,
            PrismObject<ShadowType> fetchedShadow,
            PrismObject<ShadowType> shadow,
            PrismObject<ResourceType> resource,
            Task task,
            OperationResult result) {
        PrismObject<ShadowType> fullShadow;

        if (!getConfiguration().checkFetch) {
            fullShadow = fetchShadow(checkResult, shadow, task, result);
        } else {
            fullShadow = fetchedShadow;
        }
        if (fullShadow == null) {
            checkResult.recordError(
                    ShadowStatistics.CANNOT_APPLY_FIX,
                    new SystemException("Cannot fix missing intent, because the resource object couldn't be fetched"));
            return;
        }

        ResourceObjectClassification classification;
        try {
            classification = getModelBeans().provisioningService.classifyResourceObject(
                    fullShadow.asObjectable(),
                    resource.asObjectable(),
                    null,
                    task,
                    result);
        } catch (CommonException e) {
            checkResult.recordError(
                    ShadowStatistics.CANNOT_APPLY_FIX,
                    new SystemException("Couldn't prepare fix for missing intent, because shadow cannot be classified", e));
            return;
        }

        PropertyDelta<String> delta = prismContext.deltaFactory().property()
                .createReplaceDelta(fullShadow.getDefinition(), ShadowType.F_INTENT, classification.getIntent());
        LOGGER.trace("Intent fix delta (not executed now) = \n{}", delta.debugDumpLazily());
        checkResult.addFixDelta(delta, ShadowStatistics.NO_INTENT_SPECIFIED);
    }

    private void applyFix(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) throws CommonException {
        LOGGER.info("Applying shadow fix{}:\n{}", activityRun.skippedForDryRun(),
                checkResult.isFixByRemovingShadow() ?
                        "DELETE " + ObjectTypeUtil.toShortString(shadow)
                        : DebugUtil.debugDump(checkResult.getFixDeltas()));
        if (!getConfiguration().dryRun) {
            try {
                if (checkResult.isFixByRemovingShadow()) {
                    getRepositoryService().deleteObject(ShadowType.class, shadow.getOid(), result);
                } else {
                    getRepositoryService().modifyObject(ShadowType.class, shadow.getOid(), checkResult.getFixDeltas(), result);
                }
                workerTask.recordObjectActionExecuted(shadow, ChangeType.MODIFY, null);
            } catch (Throwable t) {
                workerTask.recordObjectActionExecuted(shadow, ChangeType.MODIFY, t);
                throw t;
            }
        }
    }

    private void doCheckNormalization(ShadowCheckResult checkResult, ResourceAttributeDefinition<?> identifier, String value) throws SchemaException {
        QName matchingRuleQName = identifier.getMatchingRuleQName();
        if (matchingRuleQName == null) {
            return;
        }

        MatchingRule<Object> matchingRule;
        try {
            matchingRule = activityRun.getBeans().matchingRuleRegistry
                    .getMatchingRule(matchingRuleQName, identifier.getTypeName());
        } catch (SchemaException e) {
            checkResult.recordError(
                    ShadowStatistics.OTHER_FAILURE, new SchemaException("Couldn't retrieve matching rule for identifier " +
                    identifier.getItemName() + " (rule name = " + matchingRuleQName + ")"));
            return;
        }

        Object normalizedValue = matchingRule.normalize(value);
        if (!(normalizedValue instanceof String)) {
            checkResult.recordError(
                    ShadowStatistics.OTHER_FAILURE, new SchemaException("Normalized value is not a string, it's " + normalizedValue.getClass() +
                    " (identifier " + identifier.getItemName() + ", value " + value));
            return;
        }
        if (value.equals(normalizedValue)) {
            return;
        }
        String normalizedStringValue = (String) normalizedValue;

        checkResult.recordError(ShadowStatistics.NON_NORMALIZED_IDENTIFIER_VALUE,
                new SchemaException("Non-normalized value of identifier " + identifier.getItemName()
                        + ": " + value + " (normalized form: " + normalizedValue + ")"));

        if (getConfiguration().fixNormalization) {
            //noinspection rawtypes
            PropertyDelta delta = identifier.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, identifier.getItemName()));
            //noinspection unchecked
            delta.setRealValuesToReplace(normalizedStringValue);
            checkResult.addFixDelta(delta, ShadowStatistics.NON_NORMALIZED_IDENTIFIER_VALUE);
        }
    }

    private void addIdentifierValue(ObjectTypeContext context, QName identifierName, String identifierValue, PrismObject<ShadowType> shadow) {

        Map<String, Set<String>> valueMap = context.getIdentifierValueMap()
                .computeIfAbsent(identifierName, k -> new HashMap<>());
        Set<String> existingShadowOids = valueMap.get(identifierValue);
        if (existingShadowOids == null) {
            // all is well
            existingShadowOids = new HashSet<>();
            existingShadowOids.add(shadow.getOid());
            valueMap.put(identifierValue, existingShadowOids);
        } else {
            // duplicate shadows statistics are collected in a special way
            activityRun.duplicateShadowDetected(shadow.getOid());
            LOGGER.error("Multiple shadows with the value of identifier attribute {} = {}: existing one(s): {}, duplicate: {}",
                    identifierName, identifierValue, existingShadowOids, ObjectTypeUtil.toShortString(shadow.asObjectable()));
            existingShadowOids.add(shadow.getOid());
        }
    }

    // adapted from ProvisioningUtil
    private void checkOrFixShadowActivationConsistency(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow) {
        if (shadow == null) {        // just for sure
            return;
        }
        ActivationType activation = shadow.asObjectable().getActivation();
        if (activation == null) {
            return;
        }

        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_ADMINISTRATIVE_STATUS);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_EFFECTIVE_STATUS);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_VALID_FROM);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_VALID_TO);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_VALIDITY_STATUS);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_LOCKOUT_STATUS);
        checkOrFixActivationItem(checkResult, shadow, activation.asPrismContainerValue(), ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
    }

    private void checkOrFixActivationItem(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow, PrismContainerValue<?> activation, ItemName itemName) {
        PrismProperty<?> property = activation.findProperty(itemName);
        if (property == null || property.isEmpty()) {
            return;
        }
        checkResult.recordWarning(ShadowStatistics.EXTRA_ACTIVATION_DATA, "Unexpected activation item: " + property);
        if (getConfiguration().fixExtraData) {
            PropertyDelta<?> delta = prismContext.deltaFactory().property().createReplaceEmptyDelta(shadow.getDefinition(),
                    ItemPath.create(ShadowType.F_ACTIVATION, itemName));
            checkResult.addFixDelta(delta, ShadowStatistics.EXTRA_ACTIVATION_DATA);
        }
    }

    private ShadowCheckConfiguration getConfiguration() {
        return activityRun.getConfiguration();
    }

    private ShadowStatistics getStats() {
        return activityRun.getStatistics();
    }

    private ProvisioningService getProvisioningService() {
        return getModelBeans().provisioningService;
    }

    private RepositoryService getRepositoryService() {
        return getModelBeans().cacheRepositoryService;
    }

    private @NotNull ModelBeans getModelBeans() {
        return activityRun.getActivityHandler().getModelBeans();
    }
}
