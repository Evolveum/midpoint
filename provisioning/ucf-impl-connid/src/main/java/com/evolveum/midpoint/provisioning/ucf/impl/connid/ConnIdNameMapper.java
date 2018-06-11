package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ConnIdNameMapper {

	private static final String CUSTOM_OBJECTCLASS_PREFIX = "Custom";
	private static final String CUSTOM_OBJECTCLASS_SUFFIX = "ObjectClass";

	private static final Map<String,QName> specialAttributeMapIcf = new HashMap<>();
	private static final Map<QName,String> specialAttributeMapMp = new HashMap<>();

	private ResourceSchema resourceSchema = null;
	// Used when there is no schema (schemaless resource)
	private String resourceSchemaNamespace = null;

	public ConnIdNameMapper(String resourceSchemaNamespace) {
		super();
		this.resourceSchemaNamespace = resourceSchemaNamespace;
	}

	public ResourceSchema getResourceSchema() {
		return resourceSchema;
	}

	public void setResourceSchema(ResourceSchema resourceSchema) {
		this.resourceSchema = resourceSchema;
		if (resourceSchema != null) {
			resourceSchemaNamespace = resourceSchema.getNamespace();
		}
	}

	private static void initialize() {
		addSpecialAttributeMapping(Name.NAME, SchemaConstants.ICFS_NAME);
		addSpecialAttributeMapping(Uid.NAME, SchemaConstants.ICFS_UID);

		addOperationalAttributeMapping(OperationalAttributeInfos.CURRENT_PASSWORD);
		addOperationalAttributeMapping(OperationalAttributeInfos.DISABLE_DATE);
		addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE);
		addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE_DATE);
		addOperationalAttributeMapping(OperationalAttributeInfos.LOCK_OUT);
		addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD);
		addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
		addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRED);

		addOperationalAttributeMapping(SecretIcfOperationalAttributes.DESCRIPTION);
		addOperationalAttributeMapping(SecretIcfOperationalAttributes.GROUPS);
		addOperationalAttributeMapping(SecretIcfOperationalAttributes.LAST_LOGIN_DATE);
	}

	private static void addSpecialAttributeMapping(String icfName, QName qname) {
		specialAttributeMapIcf.put(icfName, qname);
		specialAttributeMapMp.put(qname, icfName);
	}

	private static void addOperationalAttributeMapping(
			SecretIcfOperationalAttributes opAttr) {
		addOperationalAttributeMapping(opAttr.getName());
	}

	private static void addOperationalAttributeMapping(AttributeInfo attrInfo) {
		addOperationalAttributeMapping(attrInfo.getName());
	}

	private static void addOperationalAttributeMapping(String icfName) {
		QName qName = convertUnderscoreAttributeNameToQName(icfName);
		addSpecialAttributeMapping(icfName, qName);
	}


	public QName convertAttributeNameToQName(String icfAttrName, ResourceAttributeContainerDefinition attributesContainerDefinition) {
		return convertAttributeNameToQName(icfAttrName, attributesContainerDefinition.getComplexTypeDefinition());
	}

	public QName convertAttributeNameToQName(String icfAttrName, ObjectClassComplexTypeDefinition ocDef) {
		if (specialAttributeMapIcf.containsKey(icfAttrName)) {
			for (ResourceAttributeDefinition attributeDefinition: ocDef.getAttributeDefinitions()) {
				if (icfAttrName.equals(attributeDefinition.getFrameworkAttributeName())) {
					return attributeDefinition.getName();
				}
			}
			// fallback, compatibility
			return specialAttributeMapIcf.get(icfAttrName);
		}
		QName attrXsdName = new QName(resourceSchemaNamespace, QNameUtil.escapeElementName(icfAttrName),
				MidPointConstants.PREFIX_NS_RI);
		return attrXsdName;
	}

	public QName convertAttributeNameToQName(String icfAttrName, ResourceAttributeDefinition attrDef) {
		if (specialAttributeMapIcf.containsKey(icfAttrName)) {
			if (icfAttrName.equals(attrDef.getFrameworkAttributeName())) {
				return attrDef.getName();
			}
			// fallback, compatibility
			return specialAttributeMapIcf.get(icfAttrName);
		}
		return attrDef.getName();
	}

	public String convertAttributeNameToConnId(PropertyDelta<?> attributeDelta, ObjectClassComplexTypeDefinition ocDef)
			throws SchemaException {
		PrismPropertyDefinition<?> propDef = attributeDelta.getDefinition();
		ResourceAttributeDefinition attrDef;
		if (propDef != null && (propDef instanceof ResourceAttributeDefinition)) {
			attrDef = (ResourceAttributeDefinition)propDef;
		} else {
			attrDef = ocDef.findAttributeDefinition(attributeDelta.getElementName());
			if (attrDef == null) {
				throw new SchemaException("No attribute "+attributeDelta.getElementName()+" in object class "+ocDef.getTypeName());
			}
		}
		return convertAttributeNameToConnId(attrDef);
	}
	
	public String convertAttributeNameToConnId(ResourceAttribute<?> attribute, ObjectClassComplexTypeDefinition ocDef)
				throws SchemaException {
			ResourceAttributeDefinition attrDef = attribute.getDefinition();
			if (attrDef == null) {
				attrDef = ocDef.findAttributeDefinition(attribute.getElementName());
				if (attrDef == null) {
					throw new SchemaException("No attribute "+attribute.getElementName()+" in object class "+ocDef.getTypeName());
				}
			}
			return convertAttributeNameToConnId(attrDef);
		}

	public <T> String convertAttributeNameToConnId(QName attributeName, ObjectClassComplexTypeDefinition ocDef, String desc)
			throws SchemaException {
		ResourceAttributeDefinition<T> attrDef = ocDef.findAttributeDefinition(attributeName);
		if (attrDef == null) {
			throw new SchemaException("No attribute "+attributeName+" in object class "+ocDef.getTypeName() + " " + desc);
		}
		return convertAttributeNameToConnId(attrDef);
	}

	public String convertAttributeNameToConnId(ResourceAttributeDefinition<?> attrDef)
			throws SchemaException {
		if (attrDef.getFrameworkAttributeName() != null) {
			return attrDef.getFrameworkAttributeName();
		}

		QName attrQName = attrDef.getName();
		if (specialAttributeMapMp.containsKey(attrQName)) {
			return specialAttributeMapMp.get(attrQName);
		}

		if (!attrQName.getNamespaceURI().equals(resourceSchemaNamespace)) {
			throw new SchemaException("No mapping from QName " + attrQName + " to an ICF attribute in resource schema namespace: " + resourceSchemaNamespace);
		}

		return attrQName.getLocalPart();

	}

	private boolean isUnderscoreSyntax(String icfAttrName) {
		return icfAttrName.startsWith("__") && icfAttrName.endsWith("__");
	}

	private static QName convertUnderscoreAttributeNameToQName(String icfAttrName) {
		// Strip leading and trailing underscores
		String inside = icfAttrName.substring(2, icfAttrName.length()-2);

		StringBuilder sb = new StringBuilder();
		int lastIndex = 0;
		while (true) {
			int nextIndex = inside.indexOf("_", lastIndex);
			if (nextIndex < 0) {
				String upcase = inside.substring(lastIndex, inside.length());
				sb.append(toCamelCase(upcase, lastIndex == 0));
				break;
			}
			String upcase = inside.substring(lastIndex, nextIndex);
			sb.append(toCamelCase(upcase, lastIndex == 0));
			lastIndex = nextIndex + 1;
		}

		return new QName(SchemaConstants.NS_ICF_SCHEMA, sb.toString());
	}

	private static String toCamelCase(String upcase, boolean lowCase) {
		if (lowCase) {
			return StringUtils.lowerCase(upcase);
		} else {
			return StringUtils.capitalize(StringUtils.lowerCase(upcase));
		}
	}

	/**
	 * Maps ICF native objectclass name to a midPoint QName objctclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	public QName objectClassToQname(ObjectClass icfObjectClass, String schemaNamespace, boolean legacySchema) {
		if (icfObjectClass == null) {
			return null;
		}
		if (icfObjectClass.is(ObjectClass.ALL_NAME)) {
			return null;
		}
		if (legacySchema) {
			if (icfObjectClass.is(ObjectClass.ACCOUNT_NAME)) {
				return new QName(schemaNamespace, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME,
						SchemaConstants.NS_ICF_SCHEMA_PREFIX);
			} else if (icfObjectClass.is(ObjectClass.GROUP_NAME)) {
				return new QName(schemaNamespace, SchemaConstants.GROUP_OBJECT_CLASS_LOCAL_NAME,
						SchemaConstants.NS_ICF_SCHEMA_PREFIX);
			} else {
				return new QName(schemaNamespace, CUSTOM_OBJECTCLASS_PREFIX + icfObjectClass.getObjectClassValue()
						+ CUSTOM_OBJECTCLASS_SUFFIX, MidPointConstants.PREFIX_NS_RI);
			}
		} else {
			return new QName(schemaNamespace, icfObjectClass.getObjectClassValue());
		}
	}

	public ObjectClass objectClassToIcf(PrismObject<? extends ShadowType> shadow, String schemaNamespace, ConnectorType connectorType, boolean legacySchema) {

		ShadowType shadowType = shadow.asObjectable();
		QName qnameObjectClass = shadowType.getObjectClass();
		if (qnameObjectClass == null) {
			ResourceAttributeContainer attrContainer = ShadowUtil
					.getAttributesContainer(shadowType);
			if (attrContainer == null) {
				return null;
			}
			ResourceAttributeContainerDefinition objectClassDefinition = attrContainer.getDefinition();
			qnameObjectClass = objectClassDefinition.getTypeName();
		}

		return objectClassToIcf(qnameObjectClass, schemaNamespace, connectorType, legacySchema);
	}

	/**
	 * Maps a midPoint QName objctclass to the ICF native objectclass name.
	 * <p/>
	 * The mapping is "stateless" - it does not keep any mapping database or any
	 * other state. There is a bi-directional mapping algorithm.
	 * <p/>
	 * TODO: mind the special characters in the ICF objectclass names.
	 */
	public ObjectClass objectClassToIcf(ObjectClassComplexTypeDefinition objectClassDefinition, String schemaNamespace, ConnectorType connectorType, boolean legacySchema) {
		QName qnameObjectClass = objectClassDefinition.getTypeName();
		return objectClassToIcf(qnameObjectClass, schemaNamespace, connectorType, legacySchema);
	}

	public ObjectClass objectClassToIcf(QName qnameObjectClass, String schemaNamespace, ConnectorType connectorType, boolean legacySchema) {
		if (!schemaNamespace.equals(qnameObjectClass.getNamespaceURI())) {
			throw new IllegalArgumentException("ObjectClass QName " + qnameObjectClass
					+ " is not in the appropriate namespace for "
					+ connectorType + ", expected: " + schemaNamespace);
		}

		String lname = qnameObjectClass.getLocalPart();
		if (legacySchema) {
			if (SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME.equals(lname)) {
				return ObjectClass.ACCOUNT;
			} else if (SchemaConstants.GROUP_OBJECT_CLASS_LOCAL_NAME.equals(lname)) {
				return ObjectClass.GROUP;
			} else if (lname.startsWith(CUSTOM_OBJECTCLASS_PREFIX) && lname.endsWith(CUSTOM_OBJECTCLASS_SUFFIX)) {
				String icfObjectClassName = lname.substring(CUSTOM_OBJECTCLASS_PREFIX.length(), lname.length()
						- CUSTOM_OBJECTCLASS_SUFFIX.length());
				return new ObjectClass(icfObjectClassName);
			} else {
				throw new IllegalArgumentException("Cannot recognize objectclass QName " + qnameObjectClass
						+ " for " + ObjectTypeUtil.toShortString(connectorType) + ", expected: "
						+ schemaNamespace);
			}
		} else {
			return new ObjectClass(lname);
		}
	}

	static {
		initialize();
	}

}
