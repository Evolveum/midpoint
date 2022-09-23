/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
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
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

public class ProvisioningUtil {

    private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");
    public static final Duration DEFAULT_OPERATION_RETRY_PERIOD_DURATION = XmlTypeConverter.createDuration("PT30M");
    public static final int DEFAULT_OPERATION_RETRY_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_PENDING_OPERATION_RETENTION_PERIOD_DURATION = XmlTypeConverter.createDuration("P1D");;
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

    public static AttributesToReturn createAttributesToReturn(ProvisioningContext ctx) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        ResourceType resource = ctx.getResource();

        boolean apply = false;
        AttributesToReturn attributesToReturn = new AttributesToReturn();

        // Attributes

        boolean hasMinimal = false;
        for (RefinedAttributeDefinition attributeDefinition : objectClassDefinition.getAttributeDefinitions()) {
            if (attributeDefinition.getFetchStrategy() == AttributeFetchStrategyType.MINIMAL) {
                hasMinimal = true;
                break;
            }
        }

        attributesToReturn.setReturnDefaultAttributes(!hasMinimal);

        Collection<ResourceAttributeDefinition> explicit = new ArrayList<>();
        for (RefinedAttributeDefinition attributeDefinition : objectClassDefinition.getAttributeDefinitions()) {
            AttributeFetchStrategyType fetchStrategy = attributeDefinition.getFetchStrategy();
            if (fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                explicit.add(attributeDefinition);
            } else if (hasMinimal && (fetchStrategy != AttributeFetchStrategyType.MINIMAL ||
                    (SelectorOptions.hasToLoadPath(ctx.getPrismContext().path(ShadowType.F_ATTRIBUTES, attributeDefinition.getItemName()), ctx.getGetOperationOptions(), false) ||
                    // Following clause is legacy (MID-5838)
                    SelectorOptions.hasToLoadPath(ctx.getPrismContext().toUniformPath(attributeDefinition.getItemName()), ctx.getGetOperationOptions(), false)))) {
                explicit.add(attributeDefinition);
            }
        }

        if (!explicit.isEmpty()) {
            attributesToReturn.setAttributesToReturn(explicit);
            apply = true;
        }

        // Password
        CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(
                resource, CredentialsCapabilityType.class);
        if (credentialsCapabilityType != null) {
            if (SelectorOptions.hasToLoadPath(ctx.getPrismContext().toUniformPath(SchemaConstants.PATH_PASSWORD_VALUE), ctx.getGetOperationOptions())) {
                attributesToReturn.setReturnPasswordExplicit(true);
                apply = true;
            } else {
                if (!CapabilityUtil.isPasswordReturnedByDefault(credentialsCapabilityType)) {
                    // There resource is capable of returning password but it does not
                    // do it by default
                    AttributeFetchStrategyType passwordFetchStrategy = objectClassDefinition
                            .getPasswordFetchStrategy();
                    if (passwordFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                        attributesToReturn.setReturnPasswordExplicit(true);
                        apply = true;
                    }
                }
            }
        }

        // Activation
        ActivationCapabilityType activationCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource,
                ActivationCapabilityType.class);
        if (activationCapabilityType != null) {
            if (CapabilityUtil.isCapabilityEnabled(activationCapabilityType.getStatus())) {
                if (!CapabilityUtil.isActivationStatusReturnedByDefault(activationCapabilityType)) {
                    // There resource is capable of returning enable flag but it does
                    // not do it by default
                    if (SelectorOptions.hasToLoadPath(ctx.path(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS), ctx.getGetOperationOptions())) {
                        attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = objectClassDefinition
                                .getActivationFetchStrategy(ActivationType.F_ADMINISTRATIVE_STATUS);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapabilityType.getValidFrom())) {
                if (!CapabilityUtil.isActivationValidFromReturnedByDefault(activationCapabilityType)) {
                    if (SelectorOptions.hasToLoadPath(ctx.path(SchemaConstants.PATH_ACTIVATION_VALID_FROM), ctx.getGetOperationOptions())) {
                        attributesToReturn.setReturnValidFromExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = objectClassDefinition
                                .getActivationFetchStrategy(ActivationType.F_VALID_FROM);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnValidFromExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapabilityType.getValidTo())) {
                if (!CapabilityUtil.isActivationValidToReturnedByDefault(activationCapabilityType)) {
                    if (SelectorOptions.hasToLoadPath(ctx.path(SchemaConstants.PATH_ACTIVATION_VALID_TO), ctx.getGetOperationOptions())) {
                        attributesToReturn.setReturnValidToExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = objectClassDefinition
                                .getActivationFetchStrategy(ActivationType.F_VALID_TO);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnValidToExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapabilityType.getLockoutStatus())) {
                if (!CapabilityUtil.isActivationLockoutStatusReturnedByDefault(activationCapabilityType)) {
                    // There resource is capable of returning lockout flag but it does
                    // not do it by default
                    if (SelectorOptions.hasToLoadPath(ctx.path(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS), ctx.getGetOperationOptions())) {
                        attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType statusFetchStrategy = objectClassDefinition
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

    public static <T> PropertyDelta<T> narrowPropertyDelta(PropertyDelta<T> propertyDelta,
            PrismObject<ShadowType> currentShadow, QName overridingMatchingRuleQName, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        QName matchingRuleQName = overridingMatchingRuleQName;
        ItemDefinition propertyDef = propertyDelta.getDefinition();
        if (matchingRuleQName == null && propertyDef instanceof RefinedAttributeDefinition) {
            matchingRuleQName = ((RefinedAttributeDefinition)propertyDef).getMatchingRuleQName();
        }
        MatchingRule<T> matchingRule = null;
        if (matchingRuleQName != null && propertyDef != null) {
            matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, propertyDef.getTypeName());
        }
        LOGGER.trace("Narrowing attr def={}, matchingRule={} ({})", propertyDef, matchingRule, matchingRuleQName);
        PropertyDelta<T> filteredDelta = propertyDelta.narrow(currentShadow, matchingRule, true);   // MID-5280
        if (LOGGER.isTraceEnabled() && (filteredDelta == null || !filteredDelta.equals(propertyDelta))) {
            LOGGER.trace("Narrowed delta: {}", filteredDelta==null?null:filteredDelta.debugDump());
        }
        return filteredDelta;
    }

    public static RefinedResourceSchema getRefinedSchema(ResourceType resourceType) throws SchemaException, ConfigurationException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        if (refinedSchema == null) {
            throw new ConfigurationException("No schema for "+resourceType);
        }
        return refinedSchema;
    }

    public static boolean isProtectedShadow(RefinedObjectClassDefinition objectClassDefinition, PrismObject<ShadowType> shadow,
            MatchingRuleRegistry matchingRuleRegistry, RelationRegistry relationRegistry) throws SchemaException {
        boolean isProtected;
        if (objectClassDefinition == null) {
            isProtected = false;
        } else {
            Collection<ResourceObjectPattern> protectedAccountPatterns = objectClassDefinition.getProtectedObjectPatterns();
            if (protectedAccountPatterns == null) {
                isProtected = false;
            } else {
                isProtected = ResourceObjectPattern.matches(shadow, protectedAccountPatterns, matchingRuleRegistry, relationRegistry);
            }
        }
        LOGGER.trace("isProtectedShadow: {}: {} = {}", objectClassDefinition, shadow, isProtected);
        return isProtected;
    }

    public static void setProtectedFlag(ProvisioningContext ctx, PrismObject<ShadowType> resourceObject,
            MatchingRuleRegistry matchingRuleRegistry, RelationRegistry relationRegistry) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        if (isProtectedShadow(ctx.getObjectClassDefinition(), resourceObject, matchingRuleRegistry, relationRegistry)) {
            resourceObject.asObjectable().setProtectedObject(true);
        }
    }

    public static RefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource) throws SchemaException, ConfigurationException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        if (refinedSchema == null) {
            throw new ConfigurationException("No schema for "+resource);
        }
        return refinedSchema;
    }

    public static void recordFatalError(Trace logger, OperationResult opResult, String message, Throwable ex) {
        if (message == null) {
            message = ex.getMessage();
        }
        LoggingUtils.logExceptionAsWarning(logger, message, ex);
        opResult.recordFatalError(message, ex);
        opResult.cleanupResult(ex);
    }

    public static void logWarning(Trace logger, OperationResult opResult, String message, Exception ex) {
        logger.error(message, ex);
        opResult.recordWarning(message, ex);
    }

    public static boolean shouldStoreAttributeInShadow(RefinedObjectClassDefinition objectClassDefinition, QName attributeName,
            CachingStategyType cachingStrategy) throws ConfigurationException {
        if (cachingStrategy == null || cachingStrategy == CachingStategyType.NONE) {
            if (objectClassDefinition.isPrimaryIdentifier(attributeName) || objectClassDefinition.isSecondaryIdentifier(attributeName)) {
                return true;
            }
            for (RefinedAssociationDefinition associationDef: objectClassDefinition.getAssociationDefinitions()) {
                if (associationDef.getResourceObjectAssociationType().getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
                    QName valueAttributeName = associationDef.getResourceObjectAssociationType().getValueAttribute();
                    if (QNameUtil.match(attributeName, valueAttributeName)) {
                        return true;
                    }
                }
            }
            return false;

        } else if (cachingStrategy == CachingStategyType.PASSIVE) {
            RefinedAttributeDefinition<Object> attrDef = objectClassDefinition.findAttributeDefinition(attributeName);
            return attrDef != null;

        } else {
            throw new ConfigurationException("Unknown caching strategy "+cachingStrategy);
        }
    }

    public static boolean shouldStoreActivationItemInShadow(QName elementName, CachingStategyType cachingStrategy) {    // MID-2585
        if (cachingStrategy == CachingStategyType.PASSIVE) {
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

    public static void addPasswordMetadata(PasswordType p, XMLGregorianCalendar now, PrismObject<UserType> owner) {
        MetadataType metadata = p.getMetadata();
        if (metadata != null) {
            return;
        }
        // Supply some metadata if they are not present. However the
        // normal thing is that those metadata are provided by model
        metadata = new MetadataType();
        metadata.setCreateTimestamp(now);
        if (owner != null) {
            metadata.creatorRef(owner.getOid(), null);
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

    public static CachingStategyType getCachingStrategy(ProvisioningContext ctx)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = ctx.getResource();
        CachingPolicyType caching = resource.getCaching();
        if (caching == null || caching.getCachingStategy() == null) {
            ReadCapabilityType readCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ReadCapabilityType.class);
            if (readCapabilityType == null) {
                return CachingStategyType.NONE;
            }
            Boolean cachingOnly = readCapabilityType.isCachingOnly();
            if (cachingOnly == Boolean.TRUE) {
                return CachingStategyType.PASSIVE;
            }
            return CachingStategyType.NONE;
        }
        return caching.getCachingStategy();
    }

    public static boolean shouldDoRepoSearch(GetOperationOptions rootOptions) {
        return GetOperationOptions.isNoFetch(rootOptions) || GetOperationOptions.isMaxStaleness(rootOptions);
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

    public static boolean isOverPeriod(XMLGregorianCalendar now, Duration period, XMLGregorianCalendar lastActivityTimestamp) {
        if (period == null) {
            return true;
        }
        XMLGregorianCalendar expirationTimestamp = XmlTypeConverter.addDuration(lastActivityTimestamp, period);
        return XmlTypeConverter.compare(now, expirationTimestamp) == DatatypeConstants.GREATER;
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
        for (PendingOperationType pendingOperation: shadow.asObjectable().getPendingOperation()) {
            if (isPendingAddOperation(pendingOperation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPendingAddOperation(PendingOperationType pendingOperation) {
        return ChangeTypeType.ADD.equals(pendingOperation.getDelta().getChangeType()) &&
                !PendingOperationExecutionStatusType.COMPLETED.equals(pendingOperation.getExecutionStatus());
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
        return PointInTimeType.FUTURE.equals(pit);
    }

    public static ResourceOperationDescription createResourceFailureDescription(
            PrismObject<ShadowType> conflictedShadow, ResourceType resource, ObjectDelta<ShadowType> delta, OperationResult parentResult) {
        ResourceOperationDescription failureDesc = new ResourceOperationDescription();
        failureDesc.setCurrentShadow(conflictedShadow);
        failureDesc.setObjectDelta(delta);
        failureDesc.setResource(resource.asPrismObject());
        failureDesc.setResult(parentResult);
        failureDesc.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));

        return failureDesc;
    }

    public static boolean isDoDiscovery(ResourceType resource, GetOperationOptions rootOptions) {
        return !GetOperationOptions.isDoNotDiscovery(rootOptions) && isDoDiscovery(resource);
    }

    public static boolean isDoDiscovery(ResourceType resource, ProvisioningOperationOptions options) {
        return !ProvisioningOperationOptions.isDoNotDiscovery(options) && isDoDiscovery(resource);
    }

    public static boolean isDoDiscovery (ResourceType resource) {
        if (resource == null) {
            return true;
        }
        if (resource.getConsistency() == null) {
            return true;
        }

        if (resource.getConsistency().isDiscovery() == null) {
            return true;
        }

        return resource.getConsistency().isDiscovery();
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
        result.recordInProgress();
        return OperationResultStatus.IN_PROGRESS;
    }

}
