/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowCreator;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

public class ProvisioningUtil {

    private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");
    private static final Duration DEFAULT_OPERATION_RETRY_PERIOD_DURATION = XmlTypeConverter.createDuration("PT30M");
    private static final int DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_PENDING_OPERATION_RETENTION_PERIOD_DURATION = XmlTypeConverter.createDuration("P1D");
    private static final Duration DEFAULT_DEAD_SHADOW_RETENTION_PERIOD_DURATION = XmlTypeConverter.createDuration("P7D");

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningUtil.class);

    public static ExecuteProvisioningScriptOperation convertToScriptOperation(
            ProvisioningScriptType scriptType, String desc, PrismContext prismContext) throws SchemaException {
        ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

        MutablePrismPropertyDefinition<?> scriptArgumentDefinition =
                prismContext.definitionFactory().createPropertyDefinition(
                        FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING);
        scriptArgumentDefinition.setMinOccurs(0);
        scriptArgumentDefinition.setMaxOccurs(-1);

        for (ProvisioningScriptArgumentType argument : scriptType.getArgument()) {
            ExecuteScriptArgument arg = new ExecuteScriptArgument(
                    argument.getName(),
                    StaticExpressionUtil.getStaticOutput(
                            argument, scriptArgumentDefinition, desc, ExpressionReturnMultiplicityType.SINGLE));
            scriptOperation.getArgument().add(arg);
        }

        scriptOperation.setLanguage(scriptType.getLanguage());
        scriptOperation.setTextCode(scriptType.getCode());

        if (scriptType.getHost() != null && scriptType.getHost().equals(ProvisioningScriptHostType.CONNECTOR)) {
            scriptOperation.setConnectorHost(true);
            scriptOperation.setResourceHost(false);
        }
        if (scriptType.getHost() == null || scriptType.getHost().equals(ProvisioningScriptHostType.RESOURCE)) {
            scriptOperation.setConnectorHost(false);
            scriptOperation.setResourceHost(true);
        }

        scriptOperation.setCriticality(scriptType.getCriticality());

        return scriptOperation;
    }

    // To be called via ProvisioningContext
    public static AttributesToReturn createAttributesToReturn(ProvisioningContext ctx) {

        ResourceObjectDefinition resourceObjectDefinition = ctx.getObjectDefinitionRequired();

        ResourceType resource = ctx.getResource();

        boolean apply = false;
        AttributesToReturn attributesToReturn = new AttributesToReturn();

        // Attributes

        boolean hasMinimal = checkForMinimalFetchStrategy(ctx);
        attributesToReturn.setReturnDefaultAttributes(!hasMinimal);

        Collection<ResourceAttributeDefinition<?>> explicit = getExplicitlyFetchedAttributes(ctx, !hasMinimal);

        if (!explicit.isEmpty()) {
            attributesToReturn.setAttributesToReturn(explicit);
            apply = true;
        }

        // Password
        CredentialsCapabilityType credentialsCapability =
                ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
        if (credentialsCapability != null) {
            if (ctx.isFetchingNotDisabled(SchemaConstants.PATH_PASSWORD_VALUE)) {
                attributesToReturn.setReturnPasswordExplicit(true);
                apply = true;
            } else {
                if (!CapabilityUtil.isPasswordReturnedByDefault(credentialsCapability)) {
                    // The resource is capable of returning password but it does not do it by default.
                    AttributeFetchStrategyType passwordFetchStrategy = resourceObjectDefinition.getPasswordFetchStrategy();
                    if (passwordFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                        attributesToReturn.setReturnPasswordExplicit(true);
                        apply = true;
                    }
                }
            }
        }

        // Activation
        ActivationCapabilityType activationCapability =
                ResourceTypeUtil.getEnabledCapability(resource, ActivationCapabilityType.class);
        if (activationCapability != null) {
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getStatus())) {
                if (!CapabilityUtil.isActivationStatusReturnedByDefault(activationCapability)) {
                    // The resource is capable of returning enable flag but it does not do it by default.
                    if (ctx.isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
                        attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = resourceObjectDefinition
                                .getActivationFetchStrategy(ActivationType.F_ADMINISTRATIVE_STATUS);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getValidFrom())) {
                if (!CapabilityUtil.isActivationValidFromReturnedByDefault(activationCapability)) {
                    if (ctx.isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_VALID_FROM)) {
                        attributesToReturn.setReturnValidFromExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = resourceObjectDefinition
                                .getActivationFetchStrategy(ActivationType.F_VALID_FROM);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnValidFromExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getValidTo())) {
                if (!CapabilityUtil.isActivationValidToReturnedByDefault(activationCapability)) {
                    if (ctx.isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_VALID_TO)) {
                        attributesToReturn.setReturnValidToExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = resourceObjectDefinition
                                .getActivationFetchStrategy(ActivationType.F_VALID_TO);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnValidToExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getLockoutStatus())) {
                if (!CapabilityUtil.isActivationLockoutStatusReturnedByDefault(activationCapability)) {
                    // The resource is capable of returning lockout flag but it does not do it by default.
                    if (ctx.isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS)) {
                        attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType statusFetchStrategy = resourceObjectDefinition
                                .getActivationFetchStrategy(ActivationType.F_LOCKOUT_STATUS);
                        if (statusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnLockoutStatusExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
        }

        if (apply) {
            return attributesToReturn;
        } else {
            return null;
        }
    }

    @NotNull
    private static Collection<ResourceAttributeDefinition<?>> getExplicitlyFetchedAttributes(
            ProvisioningContext ctx, boolean returnsDefaultAttributes) {
        return ctx.getObjectDefinitionRequired().getAttributeDefinitions().stream()
                .filter(attributeDefinition -> shouldExplicitlyFetch(attributeDefinition, returnsDefaultAttributes, ctx))
                .collect(Collectors.toList());
    }

    private static boolean shouldExplicitlyFetch(
            ResourceAttributeDefinition<?> attributeDefinition,
            boolean returnsDefaultAttributes, ProvisioningContext ctx) {
        AttributeFetchStrategyType fetchStrategy =
                Objects.requireNonNullElse(attributeDefinition.getFetchStrategy(), AttributeFetchStrategyType.IMPLICIT);
        if (fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
            return true;
        }
        if (returnsDefaultAttributes && attributeDefinition.isReturnedByDefault()) {
            // Normal attributes are returned by default. So there's no need to explicitly request this attribute.
            return false;
        }
        if (isFetchingRequested(ctx, attributeDefinition)) {
            // Client wants this attribute.
            return true;
        }
        if (fetchStrategy == AttributeFetchStrategyType.MINIMAL) {
            // This attribute is configured to be NOT fetched by default. So be it.
            return false;
        }
        assert fetchStrategy == AttributeFetchStrategyType.IMPLICIT;
        // If returned by default: WILL fetch explicitly, because we are asking to not return default attributes
        // If not returned by default: WILL NOT fetch explicitly, because it's not returned by default anyway
        return attributeDefinition.isReturnedByDefault();
    }

    private static boolean isFetchingRequested(ProvisioningContext ctx, ResourceAttributeDefinition<?> attributeDefinition) {
        ItemPath attributePath = ShadowType.F_ATTRIBUTES.append(attributeDefinition.getItemName());
        ItemPath legacyPath = attributeDefinition.getItemName(); // See MID-5838
        return ctx.isFetchingRequested(attributePath) || ctx.isFetchingRequested(legacyPath);
    }

    private static boolean checkForMinimalFetchStrategy(ProvisioningContext ctx) {
        for (ResourceAttributeDefinition<?> attributeDefinition :
                ctx.getObjectDefinitionRequired().getAttributeDefinitions()) {
            if (attributeDefinition.getFetchStrategy() == AttributeFetchStrategyType.MINIMAL) {
                return true;
            }
        }
        return false;
    }

    public static <T> PropertyDelta<T> narrowPropertyDelta(
            PropertyDelta<T> propertyDelta,
            ShadowType currentShadow,
            QName overridingMatchingRuleQName,
            MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        ItemDefinition<?> propertyDef = propertyDelta.getDefinition();

        QName matchingRuleQName;
        if (overridingMatchingRuleQName != null) {
            matchingRuleQName = overridingMatchingRuleQName;
        } else if (propertyDef instanceof ResourceAttributeDefinition) {
            matchingRuleQName = ((ResourceAttributeDefinition<?>) propertyDef).getMatchingRuleQName();
        } else {
            matchingRuleQName = null;
        }

        MatchingRule<T> matchingRule;
        if (matchingRuleQName != null && propertyDef != null) {
            matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, propertyDef.getTypeName());
        } else {
            matchingRule = null;
        }
        LOGGER.trace("Narrowing attr def={}, matchingRule={} ({})", propertyDef, matchingRule, matchingRuleQName);

        Comparator<PrismPropertyValue<T>> comparator = (o1, o2) -> {
            if (o1.equals(o2, EquivalenceStrategy.REAL_VALUE, matchingRule)) {
                return 0;
            } else {
                return 1;
            }
        };
        // We can safely narrow delta using real values, because we are not interested in value metadata here.
        // Because we are dealing with properties, container IDs are also out of questions, and operational items
        // as well.
        PropertyDelta<T> filteredDelta =
                propertyDelta.narrow(asPrismObject(currentShadow), comparator, comparator, true); // MID-5280
        if (filteredDelta == null || !filteredDelta.equals(propertyDelta)) {
            LOGGER.trace("Narrowed delta: {}", DebugUtil.debugDumpLazily(filteredDelta));
        }
        return filteredDelta;
    }

    public static @NotNull ResourceSchema getResourceSchema(@NotNull ResourceType resource)
            throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (refinedSchema == null) {
            throw new ConfigurationException("No schema for " + resource);
        }
        return refinedSchema;
    }

    public static boolean isAddShadowEnabled(
            Collection<ResourceObjectPattern> protectedAccountPatterns, ShadowType shadow, @NotNull OperationResult result)
            throws SchemaException {
        return getEffectiveProvisioningPolicy(protectedAccountPatterns, shadow, result).getAdd().isEnabled();
    }

    public static boolean isModifyShadowEnabled(
            Collection<ResourceObjectPattern> protectedAccountPatterns, ShadowType shadow, @NotNull OperationResult result)
            throws SchemaException {
        return getEffectiveProvisioningPolicy(protectedAccountPatterns, shadow, result).getModify().isEnabled();
    }

    public static boolean isDeleteShadowEnabled(
            Collection<ResourceObjectPattern> protectedAccountPatterns, ShadowType shadow, @NotNull OperationResult result)
            throws SchemaException {
        return getEffectiveProvisioningPolicy(protectedAccountPatterns, shadow, result).getDelete().isEnabled();
    }

    private static ObjectOperationPolicyType getEffectiveProvisioningPolicy(
            Collection<ResourceObjectPattern> protectedAccountPatterns,
            ShadowType shadow,
            @NotNull OperationResult result) throws SchemaException {
        if (shadow.getEffectiveOperationPolicy() != null) {
            return shadow.getEffectiveOperationPolicy();
        }
        ObjectOperationPolicyHelper.get().updateEffectiveMarksAndPolicies(
                protectedAccountPatterns, shadow, result);
        return shadow.getEffectiveOperationPolicy();
    }

    public static void setEffectiveProvisioningPolicy (
            ProvisioningContext ctx, ShadowType shadow, ExpressionFactory factory, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        ObjectOperationPolicyHelper.get().updateEffectiveMarksAndPolicies(
                ctx.getProtectedAccountPatterns(factory, result), shadow, result);
    }

    public static void recordWarningNotRethrowing(Trace logger, OperationResult result, String message, Exception ex) {
        LoggingUtils.logExceptionAsWarning(logger, message, ex);
        result.muteLastSubresultError();
        result.recordWarningNotFinish(message, ex); // We are not the one that created the operation result
    }

    public static void recordFatalErrorWhileRethrowing(Trace logger, OperationResult opResult, String explicitMessage, Throwable ex) {
        String message = explicitMessage != null ? explicitMessage : ex.getMessage();
        // Should we log the exception? Actually, there's no reason to do it if the exception is rethrown.
        // Therefore we'll log the exception only on debug level here.
        LoggingUtils.logExceptionOnDebugLevel(logger, message, ex);
        opResult.setFatalError(message, ex); // We are not the one who created the result, so we shouldn't close it
        opResult.markExceptionRecorded();
    }

    public static void recordExceptionWhileRethrowing(Trace logger, OperationResult opResult, String explicitMessage, Throwable ex) {
        String message = explicitMessage != null ? explicitMessage : ex.getMessage();
        // Should we log the exception? Actually, there's no reason to do it if the exception is rethrown.
        // Therefore we'll log the exception only on debug level here.
        LoggingUtils.logExceptionOnDebugLevel(logger, message, ex);
        opResult.recordExceptionNotFinish(message, ex); // We are not the one who created the result, so we shouldn't close it
        opResult.markExceptionRecorded();
    }

    /** See {@link ShadowCreator#createShadowForRepoStorage(ProvisioningContext, ShadowType)}. */
    public static boolean shouldStoreAttributeInShadow(ResourceObjectDefinition objectDefinition, QName attributeName,
            CachingStrategyType cachingStrategy) throws ConfigurationException {
        if (cachingStrategy == null || cachingStrategy == CachingStrategyType.NONE) {
            if (objectDefinition.isPrimaryIdentifier(attributeName) || objectDefinition.isSecondaryIdentifier(attributeName)) {
                return true;
            }
            for (ResourceAssociationDefinition associationDef : objectDefinition.getAssociationDefinitions()) {
                if (associationDef.getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
                    QName valueAttributeName = associationDef.getDefinitionBean().getValueAttribute();
                    if (QNameUtil.match(attributeName, valueAttributeName)) {
                        return true;
                    }
                }
            }
            return false;

        } else if (cachingStrategy == CachingStrategyType.PASSIVE) {
            return objectDefinition.findAttributeDefinition(attributeName) != null;

        } else {
            throw new ConfigurationException("Unknown caching strategy " + cachingStrategy);
        }
    }

    // MID-2585
    public static boolean shouldStoreActivationItemInShadow(QName elementName, CachingStrategyType cachingStrategy) {
        return cachingStrategy == CachingStrategyType.PASSIVE
                || QNameUtil.match(elementName, ActivationType.F_ARCHIVE_TIMESTAMP)
                || QNameUtil.match(elementName, ActivationType.F_DISABLE_TIMESTAMP)
                || QNameUtil.match(elementName, ActivationType.F_ENABLE_TIMESTAMP)
                || QNameUtil.match(elementName, ActivationType.F_DISABLE_REASON);
    }

    public static void cleanupShadowActivation(ShadowType repoShadowType) {
        // cleanup activation - we don't want to store these data in repo shadow (MID-2585)
        if (repoShadowType.getActivation() != null) {
            cleanupShadowActivation(repoShadowType.getActivation());
        }
    }

    // mirrors createShadowActivationCleanupDeltas
    public static void cleanupShadowActivation(ActivationType a) {
        a.setAdministrativeStatus(null);
        a.setEffectiveStatus(null);
        a.setValidFrom(null);
        a.setValidTo(null);
        a.setValidityStatus(null);
        a.setLockoutStatus(null);
        a.setLockoutExpirationTimestamp(null);
        a.setValidityChangeTimestamp(null);
    }

    public static void cleanupShadowPassword(PasswordType p) {
        p.setValue(null);
    }

    public static void addPasswordMetadata(PasswordType p, XMLGregorianCalendar now, ObjectReferenceType ownerRef) {
        MetadataType metadata = p.getMetadata();
        if (metadata != null) {
            return;
        }
        // Supply some metadata if they are not present. However the
        // normal thing is that those metadata are provided by model
        metadata = new MetadataType();
        metadata.setCreateTimestamp(now);
        if (ownerRef != null) {
            metadata.creatorRef(ownerRef.getOid(), null);
        }
        p.setMetadata(metadata);
    }

    public static void checkShadowActivationConsistency(PrismObject<ShadowType> shadow) {
        if (shadow == null) { // just for sure
            return;
        }
        ActivationType activation = shadow.asObjectable().getActivation();
        if (activation == null) {
            return;
        }
        if (activation.getAdministrativeStatus() != null ||
                activation.getEffectiveStatus() != null ||
                activation.getValidFrom() != null ||
                activation.getValidTo() != null ||
                activation.getValidityStatus() != null ||
                activation.getLockoutStatus() != null ||
                activation.getLockoutExpirationTimestamp() != null ||
                activation.getValidityChangeTimestamp() != null) {
            String m = "Unexpected content in shadow.activation for " + ObjectTypeUtil.toShortString(shadow) + ": " + activation;
            LOGGER.warn("{}", m);
            //throw new IllegalStateException(m);        // use only for testing
        }
    }

    public static Duration getGracePeriod(ProvisioningContext ctx) {
        Duration gracePeriod = null;
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency != null) {
            gracePeriod = consistency.getPendingOperationGracePeriod();
        }
        return gracePeriod;
    }

    public static Duration getPendingOperationRetentionPeriod(ProvisioningContext ctx) {
        Duration period = null;
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency != null) {
            period = consistency.getPendingOperationRetentionPeriod();
        }
        if (period == null) {
            period = DEFAULT_PENDING_OPERATION_RETENTION_PERIOD_DURATION;
        }
        return period;
    }

    public static boolean isCompletedAndOverPeriod(
            XMLGregorianCalendar now, Duration period, PendingOperationType pendingOperation) {
        if (!isCompleted(pendingOperation.getResultStatus())) {
            return false;
        }
        XMLGregorianCalendar completionTimestamp = pendingOperation.getCompletionTimestamp();
        if (completionTimestamp == null) {
            return false;
        }
        return period == null || XmlTypeConverter.isAfterInterval(completionTimestamp, period, now);
    }

    public static Duration getRetryPeriod(ProvisioningContext ctx) {
        Duration period = null;
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency != null) {
            period = consistency.getOperationRetryPeriod();
        }
        if (period == null) {
            period = DEFAULT_OPERATION_RETRY_PERIOD_DURATION;
        }
        return period;
    }

    public static @NotNull Duration getDeadShadowRetentionPeriod(ProvisioningContext ctx) {
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        Duration period = consistency != null ? consistency.getDeadShadowRetentionPeriod() : null;
        return period != null ? period : DEFAULT_DEAD_SHADOW_RETENTION_PERIOD_DURATION;
    }

    public static int getMaxRetryAttempts(ProvisioningContext ctx) {
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency == null) {
            return DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS;
        }
        return Objects.requireNonNullElse(
                consistency.getOperationRetryMaxAttempts(),
                DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS);
    }

    public static boolean isCompleted(OperationResultStatusType statusType) {
        return statusType != null
                && statusType != OperationResultStatusType.IN_PROGRESS
                && statusType != OperationResultStatusType.UNKNOWN;
    }

    public static boolean hasPendingAddOperation(ShadowType shadow) {
        return shadow.getPendingOperation().stream()
                .anyMatch(ProvisioningUtil::isPendingAddOperation);
    }

    public static boolean hasPendingDeleteOperation(ShadowType shadow) {
        return shadow.getPendingOperation().stream()
                .anyMatch(ProvisioningUtil::isPendingDeleteOperation);
    }

    private static boolean isPendingAddOperation(PendingOperationType pendingOperation) {
        return pendingOperation.getDelta().getChangeType() == ChangeTypeType.ADD
                && pendingOperation.getExecutionStatus() != COMPLETED;
    }

    private static boolean isPendingDeleteOperation(PendingOperationType pendingOperation) {
        return pendingOperation.getDelta().getChangeType() == ChangeTypeType.DELETE
                && pendingOperation.getExecutionStatus() != COMPLETED;
    }

    public static boolean isFuturePointInTime(Collection<SelectorOptions<GetOperationOptions>> options) {
        PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
        return pit == PointInTimeType.FUTURE;
    }

    public static boolean isDoDiscovery(@NotNull ResourceType resource, ProvisioningOperationOptions options) {
        return !ProvisioningOperationOptions.isDoNotDiscovery(options) && ResourceTypeUtil.isDiscoveryAllowed(resource);
    }

    // TODO better place?
    @Nullable
    public static PrismObject<ShadowType> selectSingleShadow(@NotNull List<PrismObject<ShadowType>> shadows, Object context) {
        LOGGER.trace("Selecting from {} objects", shadows.size());

        if (shadows.isEmpty()) {
            return null;
        } else if (shadows.size() > 1) {
            LOGGER.error("Too many shadows ({}) for {}", shadows.size(), context);
            LOGGER.debug("Shadows:\n{}", DebugUtil.debugDumpLazily(shadows));
            throw new IllegalStateException("More than one shadow for " + context);
        } else {
            return shadows.get(0);
        }
    }

    /**
     * As {@link #selectSingleShadow(List, Object)} but allows the existence of multiple dead shadows
     * (if single live shadow exists). Not very nice! Transitional solution until better one is found.
     */
    @Nullable
    public static PrismObject<ShadowType> selectSingleShadowRelaxed(
            @NotNull List<PrismObject<ShadowType>> shadows, Object context) {
        var singleLive = selectLiveShadow(shadows, context);
        if (singleLive != null) {
            return singleLive;
        }

        // all remaining shadows (if any) are dead
        if (shadows.isEmpty()) {
            return null;
        } else if (shadows.size() > 1) {
            LOGGER.error("Cannot select from dead shadows ({}) for {}", shadows.size(), context);
            LOGGER.debug("Shadows:\n{}", DebugUtil.debugDumpLazily(shadows));
            throw new IllegalStateException("More than one [dead] shadow for " + context);
        } else {
            return shadows.get(0);
        }
    }

    // TODO better place?
    @Nullable
    public static PrismObject<ShadowType> selectLiveShadow(List<PrismObject<ShadowType>> shadows, Object context) {
        if (shadows == null || shadows.isEmpty()) {
            return null;
        }

        List<PrismObject<ShadowType>> liveShadows = shadows.stream()
                .filter(ShadowUtil::isNotDead)
                .toList();

        if (liveShadows.isEmpty()) {
            return null;
        } else if (liveShadows.size() > 1) {
            LOGGER.trace("More than one live shadow found ({} out of {}) {}\n{}",
                    liveShadows.size(), shadows.size(), context, DebugUtil.debugDumpLazily(shadows, 1));
            // TODO: handle "more than one shadow" case for conflicting shadows - MID-4490
            throw new IllegalStateException(
                    "Found more than one live shadow %s: %s".formatted(
                            context, MiscUtil.getDiagInfo(liveShadows, 10, 1000)));
        } else {
            return liveShadows.get(0);
        }
    }

    /**
     * @return Live shadow (if there's one) or any of the dead ones.
     * (We ignore the possibility of conflicting information in shadows.)
     *
     * TODO better place?
     */
    public static ShadowType selectLiveOrAnyShadow(List<PrismObject<ShadowType>> shadows) {
        PrismObject<ShadowType> liveShadow = ProvisioningUtil.selectLiveShadow(shadows, "");
        if (liveShadow != null) {
            return liveShadow.asObjectable();
        } else if (shadows.isEmpty()) {
            return null;
        } else {
            return shadows.get(0).asObjectable();
        }
    }

    // TODO better place?
    @Nullable
    public static PrismProperty<?> getSingleValuedPrimaryIdentifier(ShadowType shadow) {
        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
        PrismProperty<?> identifier = attributesContainer.getPrimaryIdentifier();
        if (identifier == null) {
            return null;
        }

        checkSingleIdentifierValue(identifier);
        return identifier;
    }

    // TODO better place?
    private static void checkSingleIdentifierValue(PrismProperty<?> identifier) {
        int identifierCount = identifier.getValues().size();
        // Only one value is supported for an identifier
        if (identifierCount > 1) {
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("More than one identifier value is not supported");
        }
        if (identifierCount < 1) {
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("The identifier has no value");
        }
    }

    // TODO better place?
    public static CachingStrategyType getPasswordCachingStrategy(ResourceObjectDefinition objectDefinition) {
        ResourcePasswordDefinitionType passwordDefinition = objectDefinition.getPasswordDefinition();
        if (passwordDefinition == null) {
            return null;
        }
        CachingPolicyType passwordCachingPolicy = passwordDefinition.getCaching();
        if (passwordCachingPolicy == null) {
            return null;
        }
        return passwordCachingPolicy.getCachingStrategy();
    }

    // TODO better place?
    public static void validateShadow(@NotNull ShadowType shadow, boolean requireOid) {
        validateShadow(shadow.asPrismObject(), requireOid);
    }

    public static void validateShadow(PrismObject<ShadowType> shadow, boolean requireOid) {
        if (requireOid) {
            Validate.notNull(shadow.getOid(), "null shadow OID");
        }
        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(shadow);
        }
    }

    public static Collection<ResourceAttribute<?>> selectPrimaryIdentifiers(
            Collection<ResourceAttribute<?>> identifiers, ResourceObjectDefinition def) {

        Collection<ItemName> primaryIdentifiers = def.getPrimaryIdentifiers().stream()
                .map(ItemDefinition::getItemName)
                .collect(Collectors.toSet());

        return identifiers.stream()
                .filter(attr -> QNameUtil.matchAny(attr.getElementName(), primaryIdentifiers))
                .collect(Collectors.toList());
    }
}
