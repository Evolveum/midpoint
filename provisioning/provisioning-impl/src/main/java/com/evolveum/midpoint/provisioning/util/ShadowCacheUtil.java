package com.evolveum.midpoint.provisioning.util;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

public class ShadowCacheUtil {
	
	public static ResourceObjectShadowType createShadow(ResourceObject resourceObject, ResourceType resource,
			ResourceObjectShadowType shadow) throws SchemaException {

		if (shadow == null) {
			// Determine correct type for the shadow
			if (resourceObject.isAccountType()) {
				shadow = new AccountShadowType();
			} else {
				shadow = new ResourceObjectShadowType();
			}
		}

		if (shadow.getObjectClass() == null) {
			shadow.setObjectClass(resourceObject.getDefinition().getTypeName());
		}
		if (shadow.getName() == null) {
			shadow.setName(determineShadowName(resourceObject));
		}
		if (shadow.getResource() == null) {
			shadow.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		}
		if (shadow.getAttributes() == null) {
			Attributes attributes = new Attributes();
			shadow.setAttributes(attributes);
		}

		Document doc = DOMUtil.getDocument();

		// Add all attributes to the shadow
		shadow.getAttributes().getAny().clear();
		for (ResourceObjectAttribute attr : resourceObject.getAttributes()) {
			try {
				List<Object> eList = attr.serializeToJaxb(doc);
				shadow.getAttributes().getAny().addAll(eList);
			} catch (SchemaException e) {
				throw new SchemaException("An error occured while serializing attribute " + attr
						+ " to DOM: " + e.getMessage(), e);
			}
		}

		return shadow;
	}
	
	private static String determineShadowName(ResourceObject resourceObject) throws SchemaException {
		if (resourceObject.getNamingAttribute() == null) {
			// No naming attribute defined. Try to fall back to identifiers.
			Set<ResourceObjectAttribute> identifiers = resourceObject.getIdentifiers();
			// We can use only single identifiers (not composite)
			if (identifiers.size() == 1) {
				Property identifier = identifiers.iterator().next();
				// Only single-valued identifiers
				Set<Object> values = identifier.getValues();
				if (values.size() == 1) {
					Object value = values.iterator().next();
					// and only strings
					if (value instanceof String) {
						return (String) value;
					}
				}
			}
			// Identifier is not usable as name
			// TODO: better identification of a problem
			throw new SchemaException("No naming attribute defined (and identifier not usable)");
		}
		// TODO: Error handling
		return resourceObject.getNamingAttribute().getValue(String.class);
	}
	
	public static ResourceObjectShadowType createRepositoryShadow(ResourceObject resourceObject,
			ResourceType resource, ResourceObjectShadowType shadow) throws SchemaException {
		
		shadow = createShadow(resourceObject, resource, shadow);
		Document doc = DOMUtil.getDocument();

		// Add all attributes to the shadow
		shadow.getAttributes().getAny().clear();
		Set<ResourceObjectAttribute> identifiers = resourceObject.getIdentifiers();
		for (Property p : identifiers) {
			try {
				List<Object> eList = p.serializeToJaxb(doc);
				shadow.getAttributes().getAny().addAll(eList);
			} catch (SchemaException e) {
				throw new SchemaException("An error occured while serializing property " + p + " to DOM: "
						+ e.getMessage(), e);
			}
		}

		if (shadow instanceof AccountShadowType) {
			((AccountShadowType) shadow).setCredentials(null);
		}

		return shadow;

	}

}
