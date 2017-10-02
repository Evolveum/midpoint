/**
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeValueCompleteness;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Uid;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class ConnIdConvertor {

	private static final Trace LOGGER = TraceManager.getTrace(ConnIdConvertor.class);

	private String resourceSchemaNamespace;
	private Protector protector;
	private ConnIdNameMapper icfNameMapper;

	public ConnIdConvertor(Protector protector, String resourceSchemaNamespace) {
		super();
		this.protector = protector;
		this.resourceSchemaNamespace = resourceSchemaNamespace;
	}

	public ConnIdNameMapper getIcfNameMapper() {
		return icfNameMapper;
	}

	public void setIcfNameMapper(ConnIdNameMapper icfNameMapper) {
		this.icfNameMapper = icfNameMapper;
	}

	/**
	 * Converts ICF ConnectorObject to the midPoint ResourceObject.
	 * <p/>
	 * All the attributes are mapped using the same way as they are mapped in
	 * the schema (which is actually no mapping at all now).
	 * <p/>
	 * If an optional ResourceObjectDefinition was provided, the resulting
	 * ResourceObject is schema-aware (getDefinition() method works). If no
	 * ResourceObjectDefinition was provided, the object is schema-less. TODO:
	 * this still needs to be implemented.
	 *
	 * @param co
	 *            ICF ConnectorObject to convert
	 * @param def
	 *            ResourceObjectDefinition (from the schema) or null
	 * @param full
	 *            if true it describes if the returned resource object should
	 *            contain all of the attributes defined in the schema, if false
	 *            the returned resource object will contain only attributed with
	 *            the non-null values.
	 * @return new mapped ResourceObject instance.
	 * @throws SchemaException
	 */
	<T extends ShadowType> PrismObject<T> convertToResourceObject(ConnectorObject co,
			PrismObjectDefinition<T> objectDefinition, boolean full, boolean caseIgnoreAttributeNames,
			boolean legacySchema) throws SchemaException {

		PrismObject<T> shadowPrism = null;
		if (objectDefinition != null) {
			shadowPrism = objectDefinition.instantiate();
		} else {
			throw new SchemaException("No definition");
		}

		// LOGGER.trace("Instantiated prism object {} from connector object.",
		// shadowPrism.debugDump());

		T shadow = shadowPrism.asObjectable();
		ResourceAttributeContainer attributesContainer = (ResourceAttributeContainer) shadowPrism
				.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
		ResourceAttributeContainerDefinition attributesContainerDefinition = attributesContainer.getDefinition();
		shadow.setObjectClass(attributesContainerDefinition.getTypeName());

		List<ObjectClassComplexTypeDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();

		// too loud
//		if (LOGGER.isTraceEnabled()) {
//			LOGGER.trace("Resource attribute container definition {}.", attributesContainerDefinition.debugDump());
//		}

		for (Attribute icfAttr : co.getAttributes()) {
			if (icfAttr.is(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)) {
				List<QName> auxiliaryObjectClasses = shadow.getAuxiliaryObjectClass();
				for (Object auxiliaryIcfObjectClass: icfAttr.getValue()) {
					QName auxiliaryObjectClassQname = icfNameMapper.objectClassToQname(new ObjectClass((String)auxiliaryIcfObjectClass), resourceSchemaNamespace, legacySchema);
					auxiliaryObjectClasses.add(auxiliaryObjectClassQname);
					ObjectClassComplexTypeDefinition auxiliaryObjectClassDefinition = icfNameMapper.getResourceSchema().findObjectClassDefinition(auxiliaryObjectClassQname);
					if (auxiliaryObjectClassDefinition == null) {
						throw new SchemaException("Resource object "+co+" refers to auxiliary object class "+auxiliaryObjectClassQname+" which is not in the schema");
					}
					auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDefinition);
				}
				break;
			}
		}
		
		for (Attribute connIdAttr : co.getAttributes()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Reading ICF attribute {}: {}", connIdAttr.getName(), connIdAttr.getValue());
			}
			if (connIdAttr.getName().equals(Uid.NAME)) {
				// UID is handled specially (see above)
				continue;
			}
			if (connIdAttr.is(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)) {
				// Already processed
				continue;
			}
			if (connIdAttr.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
				// password has to go to the credentials section
				ProtectedStringType password = getSingleValue(connIdAttr, ProtectedStringType.class);
				if (password == null) {
					// equals() instead of == is needed. The AttributeValueCompleteness enum may be loaded by different classloader
					if (!AttributeValueCompleteness.INCOMPLETE.equals(connIdAttr.getAttributeValueCompleteness())) {
						continue;
					}
					// There is no password value in the ConnId attribute. But it was indicated that
					// that attribute is incomplete. Therefore we can assume that there in fact is a value.
					// We just do not know it.
					ShadowUtil.setPasswordIncomplete(shadow);
					LOGGER.trace("Converted password: (incomplete)");
				} else {
					ShadowUtil.setPassword(shadow, password);
					LOGGER.trace("Converted password: {}", password);
				}
				continue;
			}
			if (connIdAttr.getName().equals(OperationalAttributes.ENABLE_NAME)) {
				Boolean enabled = getSingleValue(connIdAttr, Boolean.class);
				if (enabled == null) {
					continue;
				}
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				ActivationStatusType activationStatusType;
				if (enabled) {
					activationStatusType = ActivationStatusType.ENABLED;
				} else {
					activationStatusType = ActivationStatusType.DISABLED;
				}
				activationType.setAdministrativeStatus(activationStatusType);
				activationType.setEffectiveStatus(activationStatusType);
				LOGGER.trace("Converted activation administrativeStatus: {}", activationStatusType);
				continue;
			}
			
			if (connIdAttr.getName().equals(OperationalAttributes.ENABLE_DATE_NAME)) {
				Long millis = getSingleValue(connIdAttr, Long.class);
				if (millis == null) {
					continue;
				}
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				activationType.setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(millis));
				continue;
			}

			if (connIdAttr.getName().equals(OperationalAttributes.DISABLE_DATE_NAME)) {
				Long millis = getSingleValue(connIdAttr, Long.class);
				if (millis == null) {
					continue;
				}
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				activationType.setValidTo(XmlTypeConverter.createXMLGregorianCalendar(millis));
				continue;
			}
			
			if (connIdAttr.getName().equals(OperationalAttributes.LOCK_OUT_NAME)) {
				Boolean lockOut = getSingleValue(connIdAttr, Boolean.class);
				if (lockOut == null) {
					continue;
				}
				ActivationType activationType = ShadowUtil.getOrCreateActivation(shadow);
				LockoutStatusType lockoutStatusType;
				if (lockOut) {
					lockoutStatusType = LockoutStatusType.LOCKED;
				} else {
					lockoutStatusType = LockoutStatusType.NORMAL;
				}
				activationType.setLockoutStatus(lockoutStatusType);
				LOGGER.trace("Converted activation lockoutStatus: {}", lockoutStatusType);
				continue;
			}

			QName qname = icfNameMapper.convertAttributeNameToQName(connIdAttr.getName(), attributesContainerDefinition);
			ResourceAttributeDefinition attributeDefinition = attributesContainerDefinition.findAttributeDefinition(qname, caseIgnoreAttributeNames);

			if (attributeDefinition == null) {
				// Try to locate definition in auxiliary object classes
				for (ObjectClassComplexTypeDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
					attributeDefinition = auxiliaryObjectClassDefinition.findAttributeDefinition(qname, caseIgnoreAttributeNames);
					if (attributeDefinition != null) {
						break;
					}
				}
				if (attributeDefinition == null) {
					throw new SchemaException("Unknown attribute " + qname + " in definition of object class " + attributesContainerDefinition.getTypeName()
					+ ". Original ConnId name: " + connIdAttr.getName() + " in resource object identified by " + co.getName(), qname);
				}
			}

			if (caseIgnoreAttributeNames) {
				qname = attributeDefinition.getName();            // normalized version
			}

			ResourceAttribute<Object> resourceAttribute = attributeDefinition.instantiate(qname);

			// if true, we need to convert whole connector object to the
			// resource object also with the null-values attributes
			if (full) {
				if (connIdAttr.getValue() != null) {
					// Convert the values. While most values do not need
					// conversions, some
					// of them may need it (e.g. GuardedString)
					for (Object connIdValue : connIdAttr.getValue()) {
						Object value = convertValueFromIcf(connIdValue, qname);
						resourceAttribute.add(new PrismPropertyValue<>(value));
					}
				}

				LOGGER.trace("Converted attribute {}", resourceAttribute);
				attributesContainer.getValue().add(resourceAttribute);

				// in this case when false, we need only the attributes with the
				// non-null values.
			} else {
				if (connIdAttr.getValue() != null && !connIdAttr.getValue().isEmpty()) {
					// Convert the values. While most values do not need
					// conversions, some of them may need it (e.g. GuardedString)
					boolean empty = true;
					for (Object connIdValue : connIdAttr.getValue()) {
						if (connIdValue != null) {
							Object value = convertValueFromIcf(connIdValue, qname);
							empty = false;
							resourceAttribute.add(new PrismPropertyValue<>(value));
						}
					}

					if (!empty) {
						LOGGER.trace("Converted attribute {}", resourceAttribute);
						attributesContainer.getValue().add(resourceAttribute);
					}

				}
			}

		}

		// Add Uid if it is not there already. It can be already present,
		// e.g. if Uid and Name represent the same attribute
		Uid uid = co.getUid();
		ObjectClassComplexTypeDefinition ocDef = attributesContainerDefinition.getComplexTypeDefinition();
		ResourceAttributeDefinition<String> uidDefinition = ConnIdUtil.getUidDefinition(ocDef);
		if (uidDefinition == null) {
			throw new SchemaException("No definition for ConnId UID attribute found in definition "
					+ ocDef);
		}
		if (attributesContainer.getValue().findItem(uidDefinition.getName()) == null) {
			ResourceAttribute<String> uidRoa = uidDefinition.instantiate();
			uidRoa.setValue(new PrismPropertyValue<String>(uid.getUidValue()));
			attributesContainer.getValue().add(uidRoa);
		}

		return shadowPrism;
	}

	Set<Attribute> convertFromResourceObject(ResourceAttributeContainer attributesPrism,
			ObjectClassComplexTypeDefinition ocDef) throws SchemaException {
		Collection<ResourceAttribute<?>> resourceAttributes = attributesPrism.getAttributes();
		return convertFromResourceObject(resourceAttributes, ocDef);
	}

	Set<Attribute> convertFromResourceObject(Collection<ResourceAttribute<?>> resourceAttributes,
			ObjectClassComplexTypeDefinition ocDef) throws SchemaException {

		Set<Attribute> attributes = new HashSet<Attribute>();
		if (resourceAttributes == null) {
			// returning empty set
			return attributes;
		}

		for (ResourceAttribute<?> attribute : resourceAttributes) {
			attributes.add(convertToConnIdAttribute(attribute, ocDef));
		}
		return attributes;
	}

	Attribute convertToConnIdAttribute(ResourceAttribute<?> mpAttribute, ObjectClassComplexTypeDefinition ocDef) throws SchemaException {
		QName midPointAttrQName = mpAttribute.getElementName();
		if (midPointAttrQName.equals(SchemaConstants.ICFS_UID)) {
			throw new SchemaException("ICF UID explicitly specified in attributes");
		}

		String connIdAttrName = icfNameMapper.convertAttributeNameToIcf(mpAttribute, ocDef);

		Set<Object> connIdAttributeValues = new HashSet<Object>();
		for (PrismPropertyValue<?> pval: mpAttribute.getValues()) {
			connIdAttributeValues.add(ConnIdUtil.convertValueToIcf(pval, protector, mpAttribute.getElementName()));
		}

		try {
			return AttributeBuilder.build(connIdAttrName, connIdAttributeValues);
		} catch (IllegalArgumentException e) {
			throw new SchemaException(e.getMessage(), e);
		}
	}

	private <T> T getSingleValue(Attribute icfAttr, Class<T> type) throws SchemaException {
		List<Object> values = icfAttr.getValue();
		if (values != null && !values.isEmpty()) {
			if (values.size() > 1) {
				throw new SchemaException("Expected single value for " + icfAttr.getName());
			}
			Object val = convertValueFromIcf(values.get(0), null);
			if (val == null) {
				return null;
			}
			if (type.isAssignableFrom(val.getClass())) {
				return (T) val;
			} else {
				throw new SchemaException("Expected type " + type.getName() + " for " + icfAttr.getName()
						+ " but got " + val.getClass().getName());
			}
		} else {
			return null;
		}

	}

	private Object convertValueFromIcf(Object icfValue, QName propName) {
		if (icfValue == null) {
			return null;
		}
		if (icfValue instanceof GuardedString) {
			return fromGuardedString((GuardedString) icfValue);
		}
		return icfValue;
	}

	private ProtectedStringType fromGuardedString(GuardedString icfValue) {
		final ProtectedStringType ps = new ProtectedStringType();
		icfValue.access(new GuardedString.Accessor() {
			@Override
			public void access(char[] passwordChars) {
				try {
					ps.setClearValue(new String(passwordChars));
					protector.encrypt(ps);
				} catch (EncryptionException e) {
					throw new IllegalStateException("Protector failed to encrypt password");
				}
			}
		});
		return ps;
	}
}
