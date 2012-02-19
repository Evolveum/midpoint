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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.provisioning.impl.ShadowConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

public class ShadowCacheUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheUtil.class);

	/**
	 * Make sure that the shadow is complete, e.g. that all the mandatory fields are filled (e.g name, resourceRef, ...)
	 * Also transforms the shadow with respect to simulated capabilities.
	 */
    public static <T extends ResourceObjectShadowType> T completeShadow(T resourceShadow, T repoShadow,
    		ResourceType resource, OperationResult parentResult) throws SchemaException {
    	
    	// repoShadow is a result, we need to copy there everything that needs to be there
    	
    	// If there is no repo shadow, use resource shadow instead
    	if (repoShadow == null) {
    		repoShadow = resourceShadow;
    	}
    	ResourceAttributeContainer resourceAttributesContainer = ResourceObjectShadowUtil.getAttributesContainer(resourceShadow);
    	ResourceAttributeContainer repoAttributesContainer = ResourceObjectShadowUtil.getAttributesContainer(repoShadow);
    	
        if (repoShadow.getObjectClass() == null) {
        	repoShadow.setObjectClass(resourceAttributesContainer.getDefinition().getTypeName());
        }
        if (repoShadow.getName() == null) {
        	repoShadow.setName(determineShadowName(resourceShadow));
        }
        if (repoShadow.getResource() == null) {
        	repoShadow.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
        }
        
        // Attributes
        // If the shadows are the same then no copy is needed.
        if (repoShadow != resourceShadow) {
        	repoAttributesContainer.getValue().clear();
        	for (ResourceAttribute resourceAttribute: resourceAttributesContainer.getAttributes()) {
        		repoAttributesContainer.getValue().add(resourceAttribute);
        	}
        }
        
        // Activation
    	if (resourceShadow.getActivation() != null) {
        	ActivationType activationType = completeActivation(resourceShadow, resource, parentResult);
	        repoShadow.setActivation(activationType);
    	} else {
    		repoShadow.setActivation(null);
    	}
    	
    	if (repoShadow instanceof AccountShadowType) {
	    	// Credentials
    		AccountShadowType repoAccountShadow = (AccountShadowType)repoShadow;
    		AccountShadowType resourceAccountShadow = (AccountShadowType)resourceShadow;
    		repoAccountShadow.setCredentials(resourceAccountShadow.getCredentials());
    	}
        
        return repoShadow;
    }
    
	/**
	 * Completes activation state by determinig simulated activation if necessary.
	 * 
	 * TODO: The placement of this method is not correct. It should go back to ShadowConverter
	 */
	public static ActivationType completeActivation(ResourceObjectShadowType shadow, ResourceType resource,
			OperationResult parentResult) {

		// HACK to avoid NPE when called from the ICF layer
		if (resource == null) {
			return shadow.getActivation();
		}
		
		if (ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
			return shadow.getActivation();
		} else if (ResourceTypeUtil.hasActivationCapability(resource)) {
			return convertFromSimulatedActivationAttributes(shadow, resource, parentResult);
		} else {
			// No activation capability, nothing to do
			return null;
		}
	}

	private static ActivationType convertFromSimulatedActivationAttributes(ResourceObjectShadowType shadow, ResourceType resource,
			OperationResult parentResult) {
//		LOGGER.trace("Start converting activation type from simulated activation atribute");
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
		PrismProperty activationProperty = attributesContainer.findProperty(activationCapability.getEnableDisable().getAttribute());
//		if (activationProperty == null) {
//			LOGGER.debug("No simulated activation attribute was defined for the account.");
//			return null;
//		}
		Collection<Object> values = null;
		if (activationProperty != null) {
			values = activationProperty.getRealValues(Object.class);
		}
		ActivationType activation = convertFromSimulatedActivationValues(resource, values, parentResult);
		LOGGER.debug("Detected simulated activation attribute {} on {} with value {}, resolved into {}", new Object[]{
				SchemaDebugUtil.prettyPrint(activationCapability.getEnableDisable().getAttribute()),
				ObjectTypeUtil.toShortString(resource),
				values, activation == null ? "null" : activation.isEnabled()});
		return activation;

	}

	
	private static ActivationType convertFromSimulatedActivationAttributes(ResourceType resource, AccountShadowType shadow,
			OperationResult parentResult) {
//		LOGGER.trace("Start converting activation type from simulated activation atribute");
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		QName enableDisableAttribute = activationCapability.getEnableDisable().getAttribute();
		List<Object> values = ResourceObjectShadowUtil.getAttributeValues(shadow, enableDisableAttribute);
		ActivationType activation = convertFromSimulatedActivationValues(resource, values, parentResult);
		LOGGER.debug("Detected simulated activation attribute {} on {} with value {}, resolved into {}", new Object[]{
				SchemaDebugUtil.prettyPrint(activationCapability.getEnableDisable().getAttribute()),
				ObjectTypeUtil.toShortString(resource),
				values, activation == null ? "null" : activation.isEnabled()});
		return activation;
	}

	
	private static ActivationType convertFromSimulatedActivationValues(ResourceType resource, Collection<Object> activationValues,
		OperationResult parentResult) {
			
			ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
					ActivationCapabilityType.class);			
			if (activationCapability == null) {
				return null;
			}

			List<String> disableValues = activationCapability.getEnableDisable().getDisableValue();
			List<String> enableValues = activationCapability.getEnableDisable().getEnableValue();

			ActivationType activationType = new ActivationType();
			
			if (isNoValue(activationValues)) {

				if (hasNoValue(disableValues)) {
					activationType.setEnabled(false);
					return activationType;
				}

				if (hasNoValue(enableValues)) {
					activationType.setEnabled(true);
					return activationType;
				}

				// No activation information.
				LOGGER.warn("The {} does not provide definition for null value of simulated activation attribute",
						ObjectTypeUtil.toShortString(resource));
				if (parentResult != null) {
					parentResult
						.recordPartialError("The "
								+ ObjectTypeUtil.toShortString(resource)
								+ " has native activation capability but noes not provide value for DISABLE attribute");
				}
				
				return null;
				
			} else {
				if (activationValues.size() > 1) {
					LOGGER.warn("The {} provides {} values for DISABLE attribute, expecting just one value",
							disableValues.size(), ObjectTypeUtil.toShortString(resource));
					if (parentResult != null) {
						parentResult.recordPartialError("The " + ObjectTypeUtil.toShortString(resource)
							+ " provides " + disableValues.size()
							+ " values for DISABLE attribute, expecting just one value");
					}
				}
				Object disableObj = activationValues.iterator().next();

				for (String disable : disableValues) {
					if (disable.equals(String.valueOf(disableObj))) {
						activationType.setEnabled(false);
						return activationType;
					}
				}

				for (String enable : enableValues) {
					if ("".equals(enable) || enable.equals(String.valueOf(disableObj))) {
						activationType.setEnabled(true);
						return activationType;
					}
				}
		}

		return null;
	}


	private static boolean isNoValue(Collection<?> collection) {
		 if (collection == null) return true;
		 if (collection.isEmpty()) return true;
		 for (Object val: collection) {
			 if (val == null) continue;
			 if (val instanceof String && ((String)val).isEmpty()) continue;
			 return false;
		 }
		 return true;
	}

	private static boolean hasNoValue(Collection<?> collection) {
		 if (collection == null) return true;
		 if (collection.isEmpty()) return true;
		 for (Object val: collection) {
			 if (val == null) return true;
			 if (val instanceof String && ((String)val).isEmpty()) return true;
		 }
		 return false;
	}

	private static String determineShadowName(ResourceObjectShadowType shadow) throws SchemaException {
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
        if (attributesContainer.getNamingAttribute() == null) {
            // No naming attribute defined. Try to fall back to identifiers.
            Set<ResourceAttribute> identifiers = attributesContainer.getIdentifiers();
            // We can use only single identifiers (not composite)
            if (identifiers.size() == 1) {
                PrismProperty identifier = identifiers.iterator().next();
                // Only single-valued identifiers
                Collection<PrismPropertyValue<Object>> values = identifier.getValues();
                if (values.size() == 1) {
                    PrismPropertyValue<?> value = values.iterator().next();
                    // and only strings
                    if (value.getValue() instanceof String) {
                        return (String) value.getValue();
                    }
                }
            }
            // Identifier is not usable as name
            // TODO: better identification of a problem
            throw new SchemaException("No naming attribute defined (and identifier not usable)");
        }
        // TODO: Error handling
        return attributesContainer.getNamingAttribute().getValue(String.class).getValue();
    }

	/**
	 * Create a copy of a shadow that is suitable for repository storage.
	 */
    public static ResourceObjectShadowType createRepositoryShadow(ResourceObjectShadowType shadowType,
                                                                  ResourceType resource) throws SchemaException {

    	PrismObject<ResourceObjectShadowType> shadow = shadowType.asPrismObject();
    	ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);
    	PrismObject<ResourceObjectShadowType> repoShadow = shadow.clone();
    	ResourceAttributeContainer repoAttributesContainer = ResourceObjectShadowUtil.getAttributesContainer(repoShadow);
    
    	// Clean all repoShadow attributes and add only those that should be there
    	repoAttributesContainer.getValue().clear();
        Set<ResourceAttribute> identifiers = attributesContainer.getIdentifiers();
        for (PrismProperty p : identifiers) {
        	repoAttributesContainer.getValue().add(p);
        }

        // We don't want to store credentials in the repo
        ResourceObjectShadowType repoShadowType = repoShadow.asObjectable();
        if (repoShadowType instanceof AccountShadowType) {
            ((AccountShadowType) repoShadowType).setCredentials(null);
        }

        return repoShadowType;
    }

    public static QueryType createSearchShadowQuery(Set<ResourceAttribute> identifiers, OperationResult parentResult) throws SchemaException {
        XPathHolder xpath = createXpathHolder();
        Document doc = DOMUtil.getDocument();
        List<Object> values = new ArrayList<Object>();

        for (PrismProperty identifier : identifiers) {
            values.addAll(identifier.serializeToJaxb(doc));
        }
        Element filter;
        try {
            filter = QueryUtil.createEqualFilter(doc, xpath, values);
        } catch (SchemaException e) {
            parentResult.recordFatalError(e);
            throw e;
        }

        QueryType query = new QueryType();
        query.setFilter(filter);
        return query;
    }

    public static QueryType createSearchShadowQuery(ResourceObjectShadowType resourceShadow, ResourceType resource, OperationResult parentResult) throws SchemaException {
        XPathHolder xpath = createXpathHolder();
        ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(resourceShadow);
        PrismProperty identifier = attributesContainer.getIdentifier();

        Collection<PrismPropertyValue<Object>> idValues = identifier.getValues();
        // Only one value is supported for an identifier
        if (idValues.size() > 1) {
//			LOGGER.error("More than one identifier value is not supported");
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("More than one identifier value is not supported");
        }
        if (idValues.size() < 1) {
//			LOGGER.error("The identifier has no value");
            // TODO: This should probably be switched to checked exception later
            throw new IllegalArgumentException("The identifier has no value");
        }

        // We have all the data, we can construct the filter now
        Document doc = DOMUtil.getDocument();
        Element filter;
        try {
            filter = QueryUtil.createAndFilter(
                    doc,
                    QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF,
                            resource.getOid()),
                    QueryUtil.createEqualFilter(doc, xpath, identifier.serializeToJaxb(doc)));
        } catch (SchemaException e) {
//			LOGGER.error("Schema error while creating search filter: {}", e.getMessage(), e);
            throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
        }

        QueryType query = new QueryType();
        query.setFilter(filter);

//		LOGGER.trace("created query " + DOMUtil.printDom(filter));

        return query;
    }

    private static XPathHolder createXpathHolder() {
        XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
        List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
        xpathSegments.add(xpathSegment);
        XPathHolder xpath = new XPathHolder(xpathSegments);
        return xpath;
    }

}
