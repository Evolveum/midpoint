/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowContentDescriptionType.FROM_RESOURCE_COMPLETE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowContentDescriptionType.FROM_RESOURCE_INCOMPLETE;

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

        PrismPropertyDefinition<?> scriptArgumentDefinition =
                PrismContext.get().definitionFactory().newPropertyDefinition(
                        FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, 0, -1);

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

    public static <T> PropertyDelta<T> narrowPropertyDelta(
            @NotNull PropertyDelta<T> propertyDelta,
            @NotNull ResourceObjectShadow currentObject,
            QName overridingMatchingRuleQName,
            MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        ItemDefinition<?> propertyDef = propertyDelta.getDefinition();

        QName matchingRuleQName;
        if (overridingMatchingRuleQName != null) {
            matchingRuleQName = overridingMatchingRuleQName;
        } else if (propertyDef instanceof ShadowSimpleAttributeDefinition) {
            matchingRuleQName = ((ShadowSimpleAttributeDefinition<?>) propertyDef).getMatchingRuleQName();
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

    public static boolean isFuturePointInTime(Collection<SelectorOptions<GetOperationOptions>> options) {
        PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
        return pit == PointInTimeType.FUTURE;
    }

    public static boolean isDiscoveryAllowed(@NotNull ResourceType resource, ProvisioningOperationOptions options) {
        return !ProvisioningOperationOptions.isDoNotDiscovery(options)
                && ResourceTypeUtil.isDiscoveryAllowed(resource);
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

    /**
     * The following situation may happen:
     *
     * A shadow is fetched from the resource. The default definition for its object class contains a definition of
     * a simulated legacy association, but (after application of correct object type) the given object type does not contain it.
     *
     * We should ignore/remove such attributes. This method detects them.
     */
    public static boolean isExtraLegacyReferenceAttribute(
            @NotNull ShadowAttribute<?, ?, ?, ?> attribute, @NotNull ResourceObjectDefinition newDefinition) {
        if (!(attribute instanceof ShadowReferenceAttribute refAttr)) {
            return false;
        }
        var refAttrDef = refAttr.getDefinitionRequired();
        var refAttrName = refAttrDef.getItemName();
        var simulationDefinition = refAttrDef.getSimulationDefinition();
        return simulationDefinition != null
                && simulationDefinition.isLegacy()
                && newDefinition.findAttributeDefinition(refAttrName) == null;
    }

    /** See {@link #isExtraLegacyReferenceAttribute(ShadowAttribute, ResourceObjectDefinition)}. */
    public static void removeExtraLegacyReferenceAttributes(
            @NotNull AbstractShadow shadow, @NotNull ResourceObjectDefinition newDefinition) {
        var attributesContainer = shadow.getAttributesContainer();
        for (var refAttr : attributesContainer.getReferenceAttributes()) {
            if (isExtraLegacyReferenceAttribute(refAttr, newDefinition)) {
                LOGGER.trace("Removing extra simulated legacy reference attribute {}, as it does not exist in {}",
                        refAttr.getElementName(), newDefinition);
                attributesContainer.remove((ShadowAttribute<?, ?, ?, ?>) refAttr);
            }
        }
    }

    public static @NotNull ShadowContentDescriptionType determineContentDescription(
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options, boolean error) {
        if (error || SelectorOptions.excludesSomethingFromRetrieval(options)) {
            return FROM_RESOURCE_INCOMPLETE;
        } else {
            return FROM_RESOURCE_COMPLETE;
        }
    }

    /** Returns the clone only if the object is immutable and the result is not success. */
    public static <T extends ObjectType> T storeFetchResultIfApplicable(T object, OperationResult result) {
        if (result.isSuccess()) {
            // Let's avoid storing SUCCESS result. This is an optimization in case of read-only object retrieval,
            // because it avoids cloning the object.
            return object;
        } else {
            // This must be done after the result is closed.
            //
            // There may be fetch result stored in the object by lower layers. We overwrite it. We assume that
            // this parent result contains its value as one of the children (hence the overwriting does not cause loss
            // of information).
            //
            //noinspection unchecked
            T clone = (T) object.asPrismObject().cloneIfImmutable().asObjectable();
            clone.setFetchResult(result.createBeanReduced());
            return clone;
        }
    }

    public static boolean isFetchAssociations(@Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        return SelectorOptions.hasToIncludePath(ShadowType.F_ASSOCIATIONS, options, true);
    }
}
