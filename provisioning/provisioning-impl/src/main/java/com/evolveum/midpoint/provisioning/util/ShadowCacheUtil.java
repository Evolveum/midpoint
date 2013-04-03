/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ShadowCacheUtil {

	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheUtil.class);

	

	private static boolean isSimulatedActivationAttribute(ResourceAttribute attribute, ResourceObjectShadowType shadow,
			ResourceType resource) {
		if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {

			ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
					ActivationCapabilityType.class);

			if (activationCapability == null) {
				// TODO: maybe the warning message is needed that the resource
				// does not have either simulater or native capabilities
				return false;
			}

			ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
			ResourceAttribute activationProperty = attributesContainer.findAttribute(activationCapability
					.getEnableDisable().getAttribute());

			if (activationProperty != null && activationProperty.equals(attribute)) {
				return true;
			}
		}
		return false;

	}

	public static <T extends ResourceObjectShadowType> void normalizeShadow(T shadow, OperationResult result)
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

		ResourceAttributeContainer normalizedContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
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
	
	public static <T extends ResourceObjectShadowType> PolyStringType determineShadowName(PrismObject<T> shadow) throws SchemaException {
		String stringName = determineShadowStringName(shadow);
		if (stringName == null) {
			return null;
		}
		return new PolyStringType(stringName);
	}

	public static <T extends ResourceObjectShadowType> String determineShadowStringName(PrismObject<T> shadow) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
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

	

	public static PrismObjectDefinition<ResourceObjectShadowType> getResourceObjectShadowDefinition(
			PrismContext prismContext) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceObjectShadowType.class);
	}
	
	@SuppressWarnings("unchecked")
	public static String getResourceOidFromFilter(List<? extends ObjectFilter> conditions) throws SchemaException{
			
			for (ObjectFilter f : conditions){
				if (f instanceof RefFilter && ResourceObjectShadowType.F_RESOURCE_REF.equals(((RefFilter) f).getDefinition().getName())){
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
					return getResourceOidFromFilter(((NaryLogicalFilter) f).getCondition());
				}
			}
			
			return null;
		
	}
	
	@SuppressWarnings("rawtypes")
	public static <T> T getValueFromFilter(List<? extends ObjectFilter> conditions, QName propertyName) throws SchemaException{
		
			for (ObjectFilter f : conditions){
				if (f instanceof EqualsFilter && propertyName.equals(((EqualsFilter) f).getDefinition().getName())){
					List<? extends PrismValue> values = ((EqualsFilter) f).getValues();
					if (values.size() > 1){
						throw new SchemaException("More than one "+propertyName+" defined in the search query.");
					}
					if (values.size() < 1){
						throw new SchemaException("Search query does not have specified "+propertyName+".");
					}
					
					return (T) ((PrismPropertyValue)values.get(0)).getValue();
				}
				if (NaryLogicalFilter.class.isAssignableFrom(f.getClass())){
					return getValueFromFilter(((NaryLogicalFilter) f).getCondition(), propertyName);
				}
			}
			
			return null;
	}

}
