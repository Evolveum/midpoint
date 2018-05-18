/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static java.util.Collections.emptyList;

public class ProvisioningUtil {

	private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningUtil.class);

	public static List<ItemDelta<?, ?>> createShadowCleanupAndReconciliationDeltas(PrismObject<ShadowType> currentShadow,
			PrismObject<ShadowType> repoShadowBefore, PrismContext prismContext) throws SchemaException {
		List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();

		S_ItemEntry i = DeltaBuilder.deltaFor(ShadowType.class, prismContext);
		ShadowType repo = repoShadowBefore.asObjectable();
		if (repo.getAttemptNumber() != null) {
			i = i.item(ShadowType.F_ATTEMPT_NUMBER).replace();
		}
		if (repo.getFailedOperationType() != null) {
			i = i.item(ShadowType.F_FAILED_OPERATION_TYPE).replace();
		}
		if (repo.getObjectChange() != null) {
			i = i.item(ShadowType.F_OBJECT_CHANGE).replace();
		}
		if (repo.getResult() != null) {
			i = i.item(ShadowType.F_RESULT).replace();
		}
		if (repo.getCredentials() != null) {
			i = i.item(ShadowType.F_CREDENTIALS).replace();
		}
		itemDeltas.addAll(i.asItemDeltas());
		itemDeltas.addAll(ProvisioningUtil.createShadowAttributesReconciliationDeltas(currentShadow, repoShadowBefore, getPrismContext()));
		itemDeltas.addAll(ProvisioningUtil.createShadowActivationCleanupDeltas(repo, getPrismContext()));
		return itemDeltas;
	}

	public static PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition(
			PrismContext prismContext) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
	}

	public static ExecuteProvisioningScriptOperation convertToScriptOperation(
			ProvisioningScriptType scriptType, String desc, PrismContext prismContext) throws SchemaException {
		ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

		PrismPropertyDefinitionImpl scriptArgumentDefinition = new PrismPropertyDefinitionImpl(
				FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, prismContext);
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
			if (fetchStrategy != null && fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				explicit.add(attributeDefinition);
			} else if (hasMinimal && (fetchStrategy != AttributeFetchStrategyType.MINIMAL || SelectorOptions.isExplicitlyIncluded(attributeDefinition.getName(), ctx.getGetOperationOptions()))) {
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
			if (SelectorOptions.hasToLoadPath(SchemaConstants.PATH_PASSWORD_VALUE, ctx.getGetOperationOptions())) {
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
					if (SelectorOptions.hasToLoadPath(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ctx.getGetOperationOptions())) {
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
					if (SelectorOptions.hasToLoadPath(SchemaConstants.PATH_ACTIVATION_VALID_FROM, ctx.getGetOperationOptions())) {
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
					if (SelectorOptions.hasToLoadPath(SchemaConstants.PATH_ACTIVATION_VALID_TO, ctx.getGetOperationOptions())) {
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
					if (SelectorOptions.hasToLoadPath(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, ctx.getGetOperationOptions())) {
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
		PropertyDelta<T> filteredDelta = propertyDelta.narrow(currentShadow, matchingRule);
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
	
	public static boolean isProtectedShadow(RefinedObjectClassDefinition objectClassDefinition, PrismObject<ShadowType> shadow, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		boolean isProtected = false;
		if (objectClassDefinition == null) {
			isProtected = false;
		} else {
			Collection<ResourceObjectPattern> protectedAccountPatterns = objectClassDefinition.getProtectedObjectPatterns();
			if (protectedAccountPatterns == null) {
				isProtected = false;
			} else {
				isProtected = ResourceObjectPattern.matches(shadow, protectedAccountPatterns, matchingRuleRegistry);
			}
		}
		LOGGER.trace("isProtectedShadow: {}: {} = {}", new Object[] { objectClassDefinition,
				shadow, isProtected });
		return isProtected;
	}
	
	public static void setProtectedFlag(ProvisioningContext ctx, PrismObject<ShadowType> resourceObject, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		if (isProtectedShadow(ctx.getObjectClassDefinition(), resourceObject, matchingRuleRegistry)) {
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
		logger.error(message, ex);
		opResult.recordFatalError(message, ex);
		opResult.cleanupResult(ex);
	}

	public static void logWarning(Trace logger, OperationResult opResult, String message, Exception ex) {
		logger.error(message, ex);
		opResult.recordWarning(message, ex);
	}

	public static boolean shouldStoreAtributeInShadow(RefinedObjectClassDefinition objectClassDefinition, QName attributeName,
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

	public static boolean shouldStoreActivationItemInShadow(QName elementName, CachingStategyType cachingStrategy) {	// MID-2585
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
		S_ItemEntry i = DeltaBuilder.deltaFor(ShadowType.class, prismContext);
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
		List<QName> identifiers = attributesDefinition.getAllIdentifiers().stream().map(ItemDefinition::getName).collect(Collectors.toList());
		List<QName> outstandingInRepo;
		PrismContainer<?> repoAttributes = repoShadowBefore.findContainer(ShadowType.F_ATTRIBUTES);
		if (repoAttributes != null && repoAttributes.getValue() != null && repoAttributes.getValue().getItems() != null) {
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
				rv.add(DeltaBuilder.deltaFor(ShadowType.class, prismContext)
						.item(new ItemPath(ShadowType.F_ATTRIBUTES, attributeName), attribute.getDefinition()).replace(valuesToReplace)
						.asItemDelta());
				QNameUtil.remove(outstandingInRepo, attributeName);
			}
		}
		for (QName outstanding : outstandingInRepo) {
			boolean isIdentifier = QNameUtil.matchAny(outstanding, identifiers);
			if (!isIdentifier) {
				ResourceAttributeDefinition<?> outstandingDefinition = attributesDefinition.findAttributeDefinition(outstanding);
				if (outstandingDefinition == null) {
					continue;       // cannot do anything with this
				}
				LOGGER.trace("- removing non-identifier {} value", outstanding);
				rv.add(DeltaBuilder.deltaFor(ShadowType.class, prismContext)
						.item(new ItemPath(ShadowType.F_ATTRIBUTES, outstanding), outstandingDefinition).replace()
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
		if (shadow == null) {		// just for sure
			return;
		}
		ActivationType activation = shadow.asObjectable().getActivation();
		if (activation == null) {
			return;
		}
		FailedOperationTypeType failedOperation = shadow.asObjectable().getFailedOperationType();
		if (failedOperation == FailedOperationTypeType.ADD) {
			return;		// in this case it's ok to have activation present
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
			//throw new IllegalStateException(m);		// use only for testing
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
		ItemPath path = modification.getPath();
		QName firstPathName = ItemPath.getFirstName(path);
		return isAttributeModification(firstPathName) || isNonAttributeResourceModification(firstPathName);
	}

	public static boolean isAttributeModification(ItemDelta modification) {
		ItemPath path = modification.getPath();
		QName firstPathName = ItemPath.getFirstName(path);
		return isAttributeModification(firstPathName);
	}

	public static boolean isAttributeModification(QName firstPathName) {
		return QNameUtil.match(firstPathName, ShadowType.F_ATTRIBUTES);
	}

	public static boolean isNonAttributeResourceModification(ItemDelta modification) {
		ItemPath path = modification.getPath();
		QName firstPathName = ItemPath.getFirstName(path);
		return isNonAttributeResourceModification(firstPathName);
	}

	public static boolean isNonAttributeResourceModification(QName firstPathName) {
		return QNameUtil.match(firstPathName, ShadowType.F_ACTIVATION) || QNameUtil.match(firstPathName, ShadowType.F_CREDENTIALS) ||
				QNameUtil.match(firstPathName, ShadowType.F_ASSOCIATION) || QNameUtil.match(firstPathName, ShadowType.F_AUXILIARY_OBJECT_CLASS);
	}
	
	public static String shortDumpShadow(PrismObject<ShadowType> shadow) {
		if (shadow == null) {
			return "null";
		}
		PolyString name = shadow.getName();
		if (name != null) {
			return shadow.toString();
		}
		Collection<ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
		if (primaryIdentifiers != null && !primaryIdentifiers.isEmpty()) {
			return shortDumpShadowIdentifiers(shadow, primaryIdentifiers);
		}
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
		if (secondaryIdentifiers != null && !secondaryIdentifiers.isEmpty()) {
			return shortDumpShadowIdentifiers(shadow, secondaryIdentifiers);
		}
		return shadow.toString();
	}

	private static String shortDumpShadowIdentifiers(PrismObject<ShadowType> shadow, Collection<ResourceAttribute<?>> identifiers) {
		StringBuilder sb = new StringBuilder("shadow:");
		sb.append(shadow.getOid()).append("(");
		Iterator<ResourceAttribute<?>> iterator = identifiers.iterator();
		while (iterator.hasNext()) {
			ResourceAttribute<?> identifier = iterator.next();
			sb.append(identifier.getElementName().getLocalPart());
			sb.append("=");
			sb.append(identifier.getRealValue());
			if (iterator.hasNext()) {
				sb.append(";");
			}
		}
		sb.append(")");
		return sb.toString();
	};
}
