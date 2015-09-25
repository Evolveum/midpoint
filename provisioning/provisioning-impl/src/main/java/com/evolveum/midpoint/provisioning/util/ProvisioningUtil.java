/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProvisioningUtil {

	private static final QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningUtil.class);

	public static <T extends ShadowType> void normalizeShadow(T shadow, OperationResult result)
			throws SchemaException {

		if (shadow.getAttemptNumber() != null) {
			shadow.setAttemptNumber(null);
		}

		if (shadow.getFailedOperationType() != null) {
			shadow.setFailedOperationType(null);
		}

		if (shadow.getObjectChange() != null) {
			shadow.setObjectChange(null);
		}

		if (shadow.getResult() != null) {
			shadow.setResult(null);
		}

		if (shadow.getCredentials() != null) {
			shadow.setCredentials(null);
		}

		ResourceAttributeContainer normalizedContainer = ShadowUtil.getAttributesContainer(shadow);
		ResourceAttributeContainer oldContainer = normalizedContainer.clone();

		normalizedContainer.clear();
		Collection<ResourceAttribute<?>> identifiers = oldContainer.getIdentifiers();
		for (PrismProperty<?> p : identifiers) {
			normalizedContainer.getValue().add(p.clone());
		}

		Collection<ResourceAttribute<?>> secondaryIdentifiers = oldContainer.getSecondaryIdentifiers();
		for (PrismProperty<?> p : secondaryIdentifiers) {
			normalizedContainer.getValue().add(p.clone());
		}

	}

	public static <T extends ShadowType> PolyString determineShadowName(ShadowType shadow)
			throws SchemaException {
		return determineShadowName(shadow.asPrismObject());
	}

	public static <T extends ShadowType> PolyString determineShadowName(PrismObject<T> shadow)
			throws SchemaException {
		String stringName = determineShadowStringName(shadow);
		if (stringName == null) {
			return null;
		}
		return new PolyString(stringName);
	}

	public static <T extends ShadowType> String determineShadowStringName(PrismObject<T> shadow)
			throws SchemaException {
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
		ResourceAttribute<String> namingAttribute = attributesContainer.getNamingAttribute();
		if (namingAttribute == null || namingAttribute.isEmpty()) {
			// No naming attribute defined. Try to fall back to identifiers.
			Collection<ResourceAttribute<?>> identifiers = attributesContainer.getIdentifiers();
			// We can use only single identifiers (not composite)
			if (identifiers.size() == 1) {
				PrismProperty<?> identifier = identifiers.iterator().next();
				// Only single-valued identifiers
				Collection<PrismPropertyValue<?>> values = (Collection) identifier.getValues();
				if (values.size() == 1) {
					PrismPropertyValue<?> value = values.iterator().next();
					// and only strings
					if (value.getValue() instanceof String) {
						return (String) value.getValue();
					}
				}
			} else {
				return attributesContainer.findAttribute(ConnectorFactoryIcfImpl.ICFS_NAME)
						.getValue(String.class).getValue();
			}
			// Identifier is not usable as name
			// TODO: better identification of a problem
			throw new SchemaException("No naming attribute defined (and identifier not usable)");
		}
		// TODO: Error handling
		List<PrismPropertyValue<String>> possibleValues = namingAttribute.getValues();

		if (possibleValues.size() > 1) {
			throw new SchemaException(
					"Cannot determine name of shadow. Found more than one value for naming attribute (attr: "
							+ namingAttribute.getElementName() + ", values: {}" + possibleValues + ")");
		}

		PrismPropertyValue<String> value = possibleValues.iterator().next();

		if (value == null) {
			throw new SchemaException("Naming attribute has no value. Could not determine shadow name.");
		}

		return value.getValue();
		// return
		// attributesContainer.getNamingAttribute().getValue().getValue();
	}

	public static PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition(
			PrismContext prismContext) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
	}

	public static ExecuteProvisioningScriptOperation convertToScriptOperation(
			ProvisioningScriptType scriptType, String desc, PrismContext prismContext) throws SchemaException {
		ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

		PrismPropertyDefinition scriptArgumentDefinition = new PrismPropertyDefinition(
				FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, prismContext);

		for (ProvisioningScriptArgumentType argument : scriptType.getArgument()) {
			ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
					StaticExpressionUtil.getStaticOutput(argument, scriptArgumentDefinition, desc,
							ExpressionReturnMultiplicityType.SINGLE, prismContext));
			scriptOperation.getArgument().add(arg);
		}

		scriptOperation.setLanguage(scriptType.getLanguage());
		scriptOperation.setTextCode(scriptType.getCode());

		if (scriptType.getHost().equals(ProvisioningScriptHostType.CONNECTOR)) {
			scriptOperation.setConnectorHost(true);
			scriptOperation.setResourceHost(false);
		}
		if (scriptType.getHost().equals(ProvisioningScriptHostType.RESOURCE)) {
			scriptOperation.setConnectorHost(false);
			scriptOperation.setResourceHost(true);
		}

		return scriptOperation;
	}

	public static AttributesToReturn createAttributesToReturn(ProvisioningContext ctx) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
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
		
		Collection<ResourceAttributeDefinition> explicit = new ArrayList<ResourceAttributeDefinition>();
		for (RefinedAttributeDefinition attributeDefinition : objectClassDefinition.getAttributeDefinitions()) {
			AttributeFetchStrategyType fetchStrategy = attributeDefinition.getFetchStrategy();
			if (fetchStrategy != null && fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				explicit.add(attributeDefinition);
			} else if (hasMinimal && fetchStrategy != AttributeFetchStrategyType.MINIMAL) {
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

		// Activation/administrativeStatus
		ActivationCapabilityType activationCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapabilityType != null) {
			if (!CapabilityUtil.isActivationStatusReturnedByDefault(activationCapabilityType)) {
				// There resource is capable of returning enable flag but it does
				// not do it by default
				AttributeFetchStrategyType administrativeStatusFetchStrategy = objectClassDefinition
						.getActivationFetchStrategy(ActivationType.F_ADMINISTRATIVE_STATUS);
				if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
					attributesToReturn.setReturnAdministrativeStatusExplicit(true);
					apply = true;
				}
			}
			if (!CapabilityUtil.isActivationLockoutStatusReturnedByDefault(activationCapabilityType)) {
				// There resource is capable of returning lockout flag but it does
				// not do it by default
				AttributeFetchStrategyType statusFetchStrategy = objectClassDefinition
						.getActivationFetchStrategy(ActivationType.F_LOCKOUT_STATUS);
				if (statusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
					attributesToReturn.setReturnLockoutStatusExplicit(true);
					apply = true;
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
		if (matchingRuleQName != null) {
			matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, propertyDef.getTypeName());
		}
		LOGGER.trace("Narrowing attr def={}, matchingRule={}", propertyDef, matchingRule);
		PropertyDelta<T> filteredDelta = propertyDelta.narrow(currentShadow, matchingRule);
		if (LOGGER.isTraceEnabled() && !filteredDelta.equals(propertyDelta)) {
			LOGGER.trace("Narrowed delta: {}", filteredDelta.debugDump());
		}
		return filteredDelta;
	}
	
	public static RefinedResourceSchema getRefinedSchema(ResourceType resourceType) throws SchemaException, ConfigurationException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
		if (refinedSchema == null) {
			throw new ConfigurationException("No schema for "+resourceType);
		}
		return refinedSchema;
	}
	
	public static RefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource) throws SchemaException, ConfigurationException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
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

	public static boolean shouldStoreAtributeInShadow(ObjectClassComplexTypeDefinition objectClassDefinition, QName attributeName) {
		return (objectClassDefinition.isIdentifier(attributeName) || objectClassDefinition.isSecondaryIdentifier(attributeName));
	}
}
