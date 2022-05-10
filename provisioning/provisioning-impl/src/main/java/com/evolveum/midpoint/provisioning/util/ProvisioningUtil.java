/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectPattern;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProvisioningUtil {

    private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");
    public static final Duration DEFAULT_OPERATION_RETRY_PERIOD_DURATION = XmlTypeConverter.createDuration("PT30M");
    public static final int DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_PENDING_OPERATION_RETENTION_PERIOD_DURATION = XmlTypeConverter.createDuration("P1D");
    public static final Duration DEFAULT_DEAD_SHADOW_RETENTION_PERIOD_DURATION = XmlTypeConverter.createDuration("P7D");

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningUtil.class);

    public static PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition(
            PrismContext prismContext) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
    }

    public static ExecuteProvisioningScriptOperation convertToScriptOperation(
            ProvisioningScriptType scriptType, String desc, PrismContext prismContext) throws SchemaException {
        ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

        MutablePrismPropertyDefinition scriptArgumentDefinition = prismContext.definitionFactory().createPropertyDefinition(
                FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING);
        scriptArgumentDefinition.setMinOccurs(0);
        scriptArgumentDefinition.setMaxOccurs(-1);

        for (ProvisioningScriptArgumentType argument : scriptType.getArgument()) {
            ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
                    StaticExpressionUtil.getStaticOutput(argument, scriptArgumentDefinition, desc,
                            ExpressionReturnMultiplicityType.SINGLE, prismContext));
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

    public static AttributesToReturn createAttributesToReturn(ProvisioningContext ctx)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        ResourceObjectDefinition resourceObjectDefinition = ctx.getObjectDefinitionRequired();

        // The following can be empty if the definition is raw
        Collection<? extends ResourceAttributeDefinition<?>> refinedAttributeDefinitions =
                resourceObjectDefinition.getAttributeDefinitions();

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
                ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
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
        ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
                ActivationCapabilityType.class);
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

        if (ctx.isTypeBased()) {
            return ctx.getObjectTypeDefinitionRequired().getAttributeDefinitions().stream()
                    .filter(attributeDefinition -> shouldExplicitlyFetch(attributeDefinition, returnsDefaultAttributes, ctx))
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
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
        if (!ctx.isTypeBased()) {
            // no refined definitions, so no minimal fetch strategy for any
            return false;
        }

        for (ResourceAttributeDefinition<?> attributeDefinition :
                ctx.getObjectTypeDefinitionRequired().getAttributeDefinitions()) {
            if (attributeDefinition.getFetchStrategy() == AttributeFetchStrategyType.MINIMAL) {
                return true;
            }
        }
        return false;
    }

    public static <T> PropertyDelta<T> narrowPropertyDelta(PropertyDelta<T> propertyDelta,
            PrismObject<ShadowType> currentShadow, QName overridingMatchingRuleQName, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        ItemDefinition propertyDef = propertyDelta.getDefinition();

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
        PropertyDelta<T> filteredDelta = propertyDelta.narrow(currentShadow, comparator, comparator, true); // MID-5280
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

//    public static boolean isProtectedShadow(ResourceObjectTypeDefinition objectClassDefinition, PrismObject<ShadowType> shadow,
//            MatchingRuleRegistry matchingRuleRegistry, RelationRegistry relationRegistry) throws SchemaException {
//        boolean isProtected;
//        if (objectClassDefinition == null) {
//            isProtected = false;
//        } else {
//            Collection<ResourceObjectPattern> protectedAccountPatterns = objectClassDefinition.getProtectedObjectPatterns();
//            if (protectedAccountPatterns == null) {
//                isProtected = false;
//            } else {
//                isProtected = ResourceObjectPattern.matches(shadow, protectedAccountPatterns, matchingRuleRegistry, relationRegistry);
//            }
//        }
//        LOGGER.trace("isProtectedShadow: {}: {} = {}", objectClassDefinition, shadow, isProtected);
//        return isProtected;
//    }

    public static boolean isProtectedShadow(Collection<ResourceObjectPattern> protectedAccountPatterns, PrismObject<ShadowType> shadow,
            MatchingRuleRegistry matchingRuleRegistry, RelationRegistry relationRegistry) throws SchemaException {
        boolean isProtected = protectedAccountPatterns != null &&
                ResourceObjectPattern.matches(shadow, protectedAccountPatterns, matchingRuleRegistry, relationRegistry);
        LOGGER.trace("isProtectedShadow: {} = {}", shadow, isProtected);
        return isProtected;
    }

    public static void setProtectedFlag(ProvisioningContext ctx, PrismObject<ShadowType> resourceObjectOrShadow,
            MatchingRuleRegistry matchingRuleRegistry, RelationRegistry relationRegistry, ExpressionFactory factory, OperationResult result) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, SecurityViolationException {
        if (isProtectedShadow(ctx.getProtectedAccountPatterns(factory, result), resourceObjectOrShadow, matchingRuleRegistry, relationRegistry)) {
            resourceObjectOrShadow.asObjectable().setProtectedObject(true);
        }
    }

    // TODO resource or refined?
    public static ResourceSchema getResourceSchema(PrismObject<ResourceType> resource) throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (refinedSchema == null) {
            throw new ConfigurationException("No schema for " + resource);
        }
        return refinedSchema;
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
        opResult.recordFatalErrorNotFinish(message, ex); // We are not the one who created the result, so we shouldn't close it
    }

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

    public static boolean shouldStoreActivationItemInShadow(QName elementName, CachingStrategyType cachingStrategy) {    // MID-2585
        if (cachingStrategy == CachingStrategyType.PASSIVE) {
            return true;
        } else {
            return QNameUtil.match(elementName, ActivationType.F_ARCHIVE_TIMESTAMP) ||
                    QNameUtil.match(elementName, ActivationType.F_DISABLE_TIMESTAMP) ||
                    QNameUtil.match(elementName, ActivationType.F_ENABLE_TIMESTAMP) ||
                    QNameUtil.match(elementName, ActivationType.F_DISABLE_REASON);
        }
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

    // mirrors cleanupShadowActivation
    public static List<ItemDelta<?, ?>> createShadowActivationCleanupDeltas(
            ShadowType repo, PrismContext prismContext) throws SchemaException {
        ActivationType activation = repo.getActivation();
        if (activation == null) {
            return emptyList();
        }
        S_ItemEntry i = prismContext.deltaFor(ShadowType.class);
        if (activation.getAdministrativeStatus() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace();
        }
        if (activation.getEffectiveStatus() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).replace();
        }
        if (activation.getValidFrom() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_VALID_FROM).replace();
        }
        if (activation.getValidTo() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_VALID_TO).replace();
        }
        if (activation.getValidityStatus() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS).replace();
        }
        if (activation.getLockoutStatus() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).replace();
        }
        if (activation.getLockoutExpirationTimestamp() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP).replace();
        }
        if (activation.getValidityChangeTimestamp() != null) {
            i = i.item(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP).replace();
        }
        return i.asItemDeltas();
    }

    /**
     * Identifiers are set.
     * Extra non-identifying attributes are removed.
     */
    public static Collection<? extends ItemDelta<?, ?>> createShadowAttributesReconciliationDeltas(
            PrismObject<ShadowType> sourceShadow,
            PrismObject<ShadowType> repoShadowBefore,
            PrismContext prismContext) throws SchemaException {
        List<ItemDelta<?, ?>> rv = new ArrayList<>();
        ResourceAttributeContainer attributes = ShadowUtil.getAttributesContainer(sourceShadow);
        ResourceAttributeContainerDefinition attributesDefinition = attributes.getDefinition();
        if (attributesDefinition == null) {
            throw new IllegalStateException("No definition for " + attributes);
        }
        List<QName> identifiers = attributesDefinition.getAllIdentifiers().stream().map(ItemDefinition::getItemName).collect(Collectors.toList());
        List<QName> outstandingInRepo;
        PrismContainer<?> repoAttributes = repoShadowBefore.findContainer(ShadowType.F_ATTRIBUTES);
        if (repoAttributes != null) {
            outstandingInRepo = repoAttributes.getValue().getItems().stream()
                    .map(Item::getElementName)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            outstandingInRepo = new ArrayList<>();
        }
        for (ResourceAttribute<?> attribute : attributes.getAttributes()) {
            QName attributeName = attribute.getElementName();
            boolean isIdentifier = QNameUtil.matchAny(attributeName, identifiers);
            List<? extends PrismPropertyValue<?>> valuesToReplace;
            if (isIdentifier) {
                valuesToReplace = CloneUtil.cloneCollectionMembers(attribute.getValues());
                LOGGER.trace("- updating identifier {} value of {}", attributeName, attribute.getValues());
                rv.add(prismContext.deltaFor(ShadowType.class)
                        .item(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName), attribute.getDefinition()).replace(valuesToReplace)
                        .asItemDelta());
                QNameUtil.remove(outstandingInRepo, attributeName);
            }
        }
        for (QName outstanding : outstandingInRepo) {
            boolean isIdentifier = QNameUtil.matchAny(outstanding, identifiers);
            if (!isIdentifier) {
                ResourceAttributeDefinition<?> outstandingDefinition = attributesDefinition.findAttributeDefinition(ItemName.fromQName(outstanding));
                if (outstandingDefinition == null) {
                    continue;       // cannot do anything with this
                }
                LOGGER.trace("- removing non-identifier {} value", outstanding);
                rv.add(prismContext.deltaFor(ShadowType.class)
                        .item(ItemPath.create(ShadowType.F_ATTRIBUTES, outstanding), outstandingDefinition).replace()
                        .asItemDelta());
            }
        }
        return rv;
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
        if (shadow == null) {        // just for sure
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

    public static CachingStrategyType getCachingStrategy(ProvisioningContext ctx) {
        ResourceType resource = ctx.getResource();
        CachingPolicyType caching = resource.getCaching();
        if (caching == null || caching.getCachingStrategy() == null) {
            ReadCapabilityType readCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ReadCapabilityType.class);
            if (readCapabilityType == null) {
                return CachingStrategyType.NONE;
            }
            Boolean cachingOnly = readCapabilityType.isCachingOnly();
            if (cachingOnly == Boolean.TRUE) {
                return CachingStrategyType.PASSIVE;
            }
            return CachingStrategyType.NONE;
        }
        return caching.getCachingStrategy();
    }

    public static boolean isResourceModification(ItemDelta modification) {
        QName firstPathName = modification.getPath().firstName();
        return isAttributeModification(firstPathName) || isNonAttributeResourceModification(firstPathName);
    }

    public static boolean isAttributeModification(ItemDelta modification) {
        QName firstPathName = modification.getPath().firstName();
        return isAttributeModification(firstPathName);
    }

    public static boolean isAttributeModification(QName firstPathName) {
        return QNameUtil.match(firstPathName, ShadowType.F_ATTRIBUTES);
    }

    public static boolean isNonAttributeResourceModification(ItemDelta modification) {
        QName firstPathName = modification.getPath().firstName();
        return isNonAttributeResourceModification(firstPathName);
    }

    public static boolean isNonAttributeResourceModification(QName firstPathName) {
        return QNameUtil.match(firstPathName, ShadowType.F_ACTIVATION) || QNameUtil.match(firstPathName, ShadowType.F_CREDENTIALS) ||
                QNameUtil.match(firstPathName, ShadowType.F_ASSOCIATION) || QNameUtil.match(firstPathName, ShadowType.F_AUXILIARY_OBJECT_CLASS);
    }

    public static boolean resourceReadIsCachingOnly(ResourceType resource) {
        ReadCapabilityType readCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ReadCapabilityType.class);
        if (readCapabilityType == null) {
            return false;        // TODO reconsider this
        }
        return Boolean.TRUE.equals(readCapabilityType.isCachingOnly());
    }

    public static Duration getGracePeriod(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Duration gracePeriod = null;
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency != null) {
            gracePeriod = consistency.getPendingOperationGracePeriod();
        }
        return gracePeriod;
    }

    public static Duration getPendingOperationRetentionPeriod(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

    public static boolean isOverPeriod(XMLGregorianCalendar now, Duration period, PendingOperationType pendingOperation) {
        if (!isCompleted(pendingOperation.getResultStatus())) {
            return false;
        }
        XMLGregorianCalendar completionTimestamp = pendingOperation.getCompletionTimestamp();
        if (completionTimestamp == null) {
            return false;
        }
        return period == null || XmlTypeConverter.isAfterInterval(completionTimestamp, period, now);
    }

    public static Duration getRetryPeriod(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

    public static Duration getDeadShadowRetentionPeriod(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Duration period = null;
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency != null) {
            period = consistency.getDeadShadowRetentionPeriod();
        }
        if (period == null) {
            period = DEFAULT_DEAD_SHADOW_RETENTION_PERIOD_DURATION;
        }
        return period;
    }

    public static int getMaxRetryAttempts(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ResourceConsistencyType consistency = ctx.getResource().getConsistency();
        if (consistency == null) {
            return DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS;
        }
        Integer operationRetryMaxAttempts = consistency.getOperationRetryMaxAttempts();
        if (operationRetryMaxAttempts == null) {
            return DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS;
        }
        return operationRetryMaxAttempts;
    }

    public static boolean isCompleted(OperationResultStatusType statusType) {
        return statusType != null && statusType != OperationResultStatusType.IN_PROGRESS && statusType != OperationResultStatusType.UNKNOWN;
    }

    public static boolean hasPendingAddOperation(PrismObject<ShadowType> shadow) {
        return shadow.asObjectable().getPendingOperation().stream()
                .anyMatch(ProvisioningUtil::isPendingAddOperation);
    }

    public static boolean hasPendingDeleteOperation(PrismObject<ShadowType> shadow) {
        return shadow.asObjectable().getPendingOperation().stream()
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

    /**
     * Explicitly check the capability of the resource (primary connector), not capabilities of additional connectors
     */
    public static boolean isPrimaryCachingOnly(ResourceType resource) {
        ReadCapabilityType readCapabilityType = CapabilityUtil.getEffectiveCapability(resource.getCapabilities(), ReadCapabilityType.class);
        if (readCapabilityType == null) {
            return false;
        }
        if (!CapabilityUtil.isCapabilityEnabled(readCapabilityType)) {
            return false;
        }
        return Boolean.TRUE.equals(readCapabilityType.isCachingOnly());
    }

    public static boolean isFuturePointInTime(Collection<SelectorOptions<GetOperationOptions>> options) {
        PointInTimeType pit = GetOperationOptions.getPointInTimeType(SelectorOptions.findRootOptions(options));
        return pit == PointInTimeType.FUTURE;
    }

    public static ResourceOperationDescription createResourceFailureDescription(
            PrismObject<ShadowType> conflictedShadow, ResourceType resource, ObjectDelta<ShadowType> delta, OperationResult parentResult) {
        ResourceOperationDescription failureDesc = new ResourceOperationDescription();
        failureDesc.setCurrentShadow(conflictedShadow);
        failureDesc.setObjectDelta(delta);
        failureDesc.setResource(resource.asPrismObject());
        failureDesc.setResult(parentResult);
        failureDesc.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_DISCOVERY));

        return failureDesc;
    }

    public static boolean isDoDiscovery(ResourceType resource, GetOperationOptions rootOptions) {
        return !GetOperationOptions.isDoNotDiscovery(rootOptions) && isDoDiscovery(resource);
    }

    public static boolean isDoDiscovery(ResourceType resource, ProvisioningOperationOptions options) {
        return !ProvisioningOperationOptions.isDoNotDiscovery(options) && isDoDiscovery(resource);
    }

    public static boolean isDoDiscovery(ResourceType resource) {
        return resource == null
                || resource.getConsistency() == null
                || resource.getConsistency().isDiscovery() == null
                || resource.getConsistency().isDiscovery();
    }

    public static OperationResultStatus postponeModify(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            OperationResult failedOperationResult,
            OperationResult result) {
        LOGGER.trace("Postponing MODIFY operation for {}", repoShadow);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTING);
        AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>> asyncResult = new AsynchronousOperationReturnValue<>();
        asyncResult.setOperationResult(failedOperationResult);
        asyncResult.setOperationType(PendingOperationTypeType.RETRY);
        opState.setAsyncResult(asyncResult);
        if (opState.getAttemptNumber() == null) {
            opState.setAttemptNumber(1);
        }
        result.setInProgress();
        return OperationResultStatus.IN_PROGRESS;
    }

    // TODO better place?
    @Nullable
    public static PrismObject<ShadowType> selectSingleShadow(@NotNull List<PrismObject<ShadowType>> shadows, Object context) {
        LOGGER.trace("Selecting from {} objects", shadows.size());

        if (shadows.size() == 0) {
            return null;
        } else if (shadows.size() > 1) {
            LOGGER.error("Too many shadows ({}) for {}", shadows.size(), context);
            LOGGER.debug("Shadows:\n{}", DebugUtil.debugDumpLazily(shadows));
            throw new IllegalStateException("More than one shadow for " + context);
        } else {
            return shadows.get(0);
        }
    }

    // TODO better place?
    @Nullable
    public static PrismObject<ShadowType> selectLiveShadow(List<PrismObject<ShadowType>> shadows) {
        if (shadows == null || shadows.isEmpty()) {
            return null;
        }

        List<PrismObject<ShadowType>> liveShadows = shadows.stream()
                .filter(ShadowUtil::isNotDead)
                .collect(Collectors.toList());

        if (liveShadows.isEmpty()) {
            return null;
        } else if (liveShadows.size() > 1) {
            LOGGER.trace("More than one live shadow found ({} out of {}):\n{}",
                    liveShadows.size(), shadows.size(), DebugUtil.debugDumpLazily(shadows, 1));
            // TODO: handle "more than one shadow" case for conflicting shadows - MID-4490
            throw new IllegalStateException("Found more than one live shadow: " + liveShadows);
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
    public static PrismObject<ShadowType> selectLiveOrAnyShadow(List<PrismObject<ShadowType>> shadows) {
        PrismObject<ShadowType> liveShadow = ProvisioningUtil.selectLiveShadow(shadows);
        if (liveShadow != null) {
            return liveShadow;
        } else if (shadows.isEmpty()) {
            return null;
        } else {
            return shadows.get(0);
        }
    }

    // TODO better place?
    @Nullable
    public static PrismProperty<?> getSingleValuedPrimaryIdentifier(PrismObject<ShadowType> resourceObject) {
        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObject);
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
