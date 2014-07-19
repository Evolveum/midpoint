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
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.parser.XPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	
	public static <T extends ShadowType> PolyString determineShadowName(PrismObject<T> shadow) throws SchemaException {
		String stringName = determineShadowStringName(shadow);
		if (stringName == null) {
			return null;
		}
		return new PolyString(stringName);
	}

	public static <T extends ShadowType> String determineShadowStringName(PrismObject<T> shadow) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
		if (attributesContainer.getNamingAttribute() == null) {
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
				return attributesContainer.findAttribute(ConnectorFactoryIcfImpl.ICFS_NAME).getValue(String.class)
						.getValue();
			}
			// Identifier is not usable as name
			// TODO: better identification of a problem
			throw new SchemaException("No naming attribute defined (and identifier not usable)");
		}
		// TODO: Error handling
		return attributesContainer.getNamingAttribute().getValue().getValue();
	}

	

	public static PrismObjectDefinition<ShadowType> getResourceObjectShadowDefinition(
			PrismContext prismContext) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
	}
	
	@SuppressWarnings("unchecked")
	public static String getResourceOidFromFilter(List<? extends ObjectFilter> conditions) throws SchemaException{
			
			for (ObjectFilter f : conditions){
				if (f instanceof RefFilter && ShadowType.F_RESOURCE_REF.equals(((RefFilter) f).getDefinition().getName())){
					List<PrismReferenceValue> values = (List<PrismReferenceValue>)((RefFilter) f).getValues();
					if (values.size() > 1){
						throw new SchemaException("More than one resource references defined in the search query.");
					}
					if (values.size() < 1){
						throw new SchemaException("Search query does not have specified resource reference.");
					}
					return values.get(0).getOid();
				}
				if (NaryLogicalFilter.class.isAssignableFrom(f.getClass())){
					return getResourceOidFromFilter(((NaryLogicalFilter) f).getConditions());
				}
			}
			
			return null;
		
	}
	
	@SuppressWarnings("rawtypes")
	public static <T> T getValueFromFilter(List<? extends ObjectFilter> conditions, QName propertyName) throws SchemaException{
			ItemPath propertyPath = new ItemPath(propertyName);
			for (ObjectFilter f : conditions){
				if (f instanceof EqualFilter && propertyPath.equivalent(((EqualFilter) f).getFullPath())){
					List<? extends PrismValue> values = ((EqualFilter) f).getValues();
					if (values.size() > 1){
						throw new SchemaException("More than one "+propertyName+" defined in the search query.");
					}
					if (values.size() < 1){
						throw new SchemaException("Search query does not have specified "+propertyName+".");
					}
					
					return (T) ((PrismPropertyValue)values.get(0)).getValue();
				}
				if (NaryLogicalFilter.class.isAssignableFrom(f.getClass())){
					return getValueFromFilter(((NaryLogicalFilter) f).getConditions(), propertyName);
				}
			}
			
			return null;
	}
	
	public static ExecuteProvisioningScriptOperation convertToScriptOperation(ProvisioningScriptType scriptType, 
			String desc, PrismContext prismContext) throws SchemaException {
		ExecuteProvisioningScriptOperation scriptOperation = new ExecuteProvisioningScriptOperation();

		PrismPropertyDefinition scriptArgumentDefinition = new PrismPropertyDefinition(FAKE_SCRIPT_ARGUMENT_NAME,
				DOMUtil.XSD_STRING, prismContext);
		
		for (ProvisioningScriptArgumentType argument : scriptType.getArgument()) {
			ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
					StaticExpressionUtil.getStaticOutput(argument, scriptArgumentDefinition,
							desc, 
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
	
	public static AttributesToReturn createAttributesToReturn(RefinedObjectClassDefinition objectClassDefinition,
			ResourceType resource) throws SchemaException {
		boolean apply = false;
		AttributesToReturn attributesToReturn = new AttributesToReturn();
		attributesToReturn.setReturnDefaultAttributes(true);
		
		// Attributes
		Collection<ResourceAttributeDefinition> explicit = new ArrayList<ResourceAttributeDefinition>();
		for (RefinedAttributeDefinition attributeDefinition: objectClassDefinition.getAttributeDefinitions()) {
			AttributeFetchStrategyType fetchStrategy = attributeDefinition.getFetchStrategy();
			if (fetchStrategy != null && fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				explicit.add(attributeDefinition);
			}
		}
		
		if (!explicit.isEmpty()) {
			attributesToReturn.setAttributesToReturn(explicit);
			apply = true;
		}
		
		// Password
		CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
		if (CapabilityUtil.isPasswordReturnedByDefault(credentialsCapabilityType)) {
			// There resource is capable of returning password but it does not do it by default
			AttributeFetchStrategyType passwordFetchStrategy = objectClassDefinition.getPasswordFetchStrategy();
			if (passwordFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				attributesToReturn.setReturnPasswordExplicit(true);
				apply = true;
			}
		}
		
		// Activation/administrativeStatus
		ActivationCapabilityType activationCapabilityType = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
		if (CapabilityUtil.isActivationStatusReturnedByDefault(activationCapabilityType)) {
			// There resource is capable of returning enable flag but it does not do it by default
			AttributeFetchStrategyType administrativeStatusFetchStrategy = objectClassDefinition.getActivationFetchStrategy(ActivationType.F_ADMINISTRATIVE_STATUS);
			if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				attributesToReturn.setReturnAdministrativeStatusExplicit(true);
				apply = true;
			}
		}
		
		if (CapabilityUtil.isActivationLockoutStatusReturnedByDefault(activationCapabilityType)) {
			// There resource is capable of returning lockout flag but it does not do it by default
			AttributeFetchStrategyType statusFetchStrategy = objectClassDefinition.getActivationFetchStrategy(ActivationType.F_LOCKOUT_STATUS);
			if (statusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
				attributesToReturn.setReturnLockoutStatusExplicit(true);
				apply = true;
			}
		}
		
		if (apply) {
			return attributesToReturn;
		} else {
			return null;
		}
	}
}
