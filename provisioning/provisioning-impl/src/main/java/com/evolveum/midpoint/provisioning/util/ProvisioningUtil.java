/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
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

    public static ExecuteProvisioningScriptOperation convertToScriptOperation(ProvisioningScriptType scriptBean, String desc)
            throws SchemaException {
        ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

        MutablePrismPropertyDefinition<?> scriptArgumentDefinition =
                PrismContext.get().definitionFactory().createPropertyDefinition(
                        FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING);
        scriptArgumentDefinition.setMinOccurs(0);
        scriptArgumentDefinition.setMaxOccurs(-1);

        for (ProvisioningScriptArgumentType argument : scriptBean.getArgument()) {
            ExecuteScriptArgument arg = new ExecuteScriptArgument(
                    argument.getName(),
                    StaticExpressionUtil.getStaticOutput(
                            argument, scriptArgumentDefinition, desc, ExpressionReturnMultiplicityType.SINGLE));
            scriptOperation.getArgument().add(arg);
        }

        scriptOperation.setLanguage(scriptBean.getLanguage());
        scriptOperation.setTextCode(scriptBean.getCode());

        if (scriptBean.getHost() != null && scriptBean.getHost().equals(ProvisioningScriptHostType.CONNECTOR)) {
            scriptOperation.setConnectorHost(true);
            scriptOperation.setResourceHost(false);
        }
        if (scriptBean.getHost() == null || scriptBean.getHost().equals(ProvisioningScriptHostType.RESOURCE)) {
            scriptOperation.setConnectorHost(false);
            scriptOperation.setResourceHost(true);
        }

        scriptOperation.setCriticality(scriptBean.getCriticality());

        return scriptOperation;
    }

    // To be called via ProvisioningContext
    public static @Nullable AttributesToReturn createAttributesToReturn(@NotNull ProvisioningContext ctx) {

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
        AttributeFetchStrategyType fetchStrategy = attributeDefinition.getFetchStrategy();
        if (fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
            return true;
        } else if (returnsDefaultAttributes) {
            // Normal attributes are returned by default. So there's no need to explicitly request this attribute.
            return false;
        } else if (isFetchingRequested(ctx, attributeDefinition)) {
            // Client wants this attribute.
            return true;
        } else {
            // If the fetch strategy is MINIMAL, we want to skip. Otherwise, request it.
            return fetchStrategy != AttributeFetchStrategyType.MINIMAL;
        }
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
            @NotNull PropertyDelta<T> propertyDelta,
            @NotNull ResourceObject currentObject,
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
                propertyDelta.narrow(currentObject.getPrismObject(), comparator, comparator, true); // MID-5280
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
            ProvisioningContext ctx, ShadowType shadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        ObjectOperationPolicyHelper.get().updateEffectiveMarksAndPolicies(
                ctx.getProtectedAccountPatterns(result), shadow, result);
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

    // MID-2585
    public static boolean shouldStoreActivationItemInShadow(QName elementName, boolean cachingEnabled) {
        return cachingEnabled
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

    public static void checkShadowActivationConsistency(RepoShadow shadow) {
        if (shadow == null) { // just for sure
            return;
        }
        ActivationType activation = shadow.getBean().getActivation();
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
            String m = "Unexpected content in shadow.activation for " + ObjectTypeUtil.toShortString(shadow.getBean()) + ": " + activation;
            LOGGER.warn("{}", m);
            //throw new IllegalStateException(m);        // use only for testing
        }
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

    public static boolean isDiscoveryAllowed(@NotNull ResourceType resource, ProvisioningOperationOptions options) {
        return !ProvisioningOperationOptions.isDoNotDiscovery(options)
                && ResourceTypeUtil.isDiscoveryAllowed(resource);
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
}
