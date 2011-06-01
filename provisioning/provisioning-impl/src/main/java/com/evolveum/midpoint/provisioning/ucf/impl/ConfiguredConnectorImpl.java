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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.ConfiguredConnector;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.Token;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.HashMap;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 *
 * @author Radovan Semancik
 */
public class ConfiguredConnectorImpl implements ConfiguredConnector {

	private static final String PASSWORD_ATTRIBUTE_NAME = "__PASSWORD__";
	
	ConnectorFacade connector;
	ResourceType resource;
	static Map<Class,QName> xsdTypeMap;

	public ConfiguredConnectorImpl(ConnectorFacade connector, ResourceType resource) {
		this.connector = connector;
		this.resource = resource;
		initTypeMap();
	}
	
	private String getSchemaNamespace() {
		return resource.getNamespace();
	}

	@Override
	public Schema fetchResourceSchema() throws CommunicationException {

		org.identityconnectors.framework.common.objects.Schema icfSchema = connector.schema();
		Schema mpSchema = new Schema(getSchemaNamespace());
		Set<Definition> definitions = mpSchema.getDefinitions();

		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();

		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {

			QName objectClassXsdName;
			// Element names does not really make much sense in Resource
			// Objects as they are not usually used. But for the sake of
			// completeness we are going to generate them.
			QName objectElementName;

			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
				objectClassXsdName = new QName(getSchemaNamespace(), "AccountObjectClass", SchemaConstants.NS_ICF_SCHEMA_PREFIX);
				objectElementName = new QName(getSchemaNamespace(),"account", SchemaConstants.NS_ICF_SCHEMA_PREFIX);
			} else if (ObjectClass.GROUP_NAME.equals(objectClassInfo.getType())) {
				objectClassXsdName = new QName(getSchemaNamespace(), "GroupObjectClass", SchemaConstants.NS_ICF_SCHEMA_PREFIX);
				objectElementName = new QName(getSchemaNamespace(),"group", SchemaConstants.NS_ICF_SCHEMA_PREFIX);
			} else {
				objectClassXsdName = new QName(getSchemaNamespace(), "Custom" + objectClassInfo.getType() + "ObjectClass", SchemaConstants.NS_ICF_RESOURCE_INSTANCE_PREFIX);
				objectElementName = new QName(getSchemaNamespace(), objectClassInfo.getType(), SchemaConstants.NS_ICF_RESOURCE_INSTANCE_PREFIX);
			}

			ResourceObjectDefinition roDefinition = new ResourceObjectDefinition(mpSchema, objectElementName, objectElementName, objectClassXsdName);
			definitions.add(roDefinition);
			
			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
				roDefinition.setAccountType(true);
				roDefinition.setDefaultAccountType(true);
			}
			
			// Every object has UID in ICF, therefore add it right now
			
			ResourceObjectAttributeDefinition uidDefinition = new ResourceObjectAttributeDefinition(roDefinition, SchemaConstants.ICFS_UID, SchemaConstants.ICFS_UID, SchemaConstants.XSD_STRING);
			uidDefinition.setMinOccurs(1);
			uidDefinition.setMaxOccurs(1);
			roDefinition.getDefinitions().add(uidDefinition);
			// TODO: identifier
			
			Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
			for (AttributeInfo attributeInfo : attributeInfoSet) {

				// Default name and type for the attribute
				QName attrXsdName = new QName(getSchemaNamespace(),attributeInfo.getName(),SchemaConstants.NS_ICF_RESOURCE_INSTANCE_PREFIX);
				QName attrXsdType = mapType(attributeInfo.getType());
				
				// Handle special cases
				if (Name.NAME.equals(attributeInfo.getName())) {
					attrXsdName = SchemaConstants.ICFS_NAME;
				}
				if (PASSWORD_ATTRIBUTE_NAME.equals(attributeInfo.getName())) {
					// Temporary hack. Password should go into credentials, not attributes
					attrXsdName = SchemaConstants.ICFS_PASSWORD;
				}
				
				ResourceObjectAttributeDefinition roaDefinition = new ResourceObjectAttributeDefinition(roDefinition, attrXsdName, attrXsdName, attrXsdType);
				roDefinition.getDefinitions().add(roaDefinition);
				
				Set<Flags> flagsSet = attributeInfo.getFlags();

				//System.out.println(flagsSet);

				roaDefinition.setMinOccurs(0);
				roaDefinition.setMaxOccurs(1);				
				for (Flags flags : flagsSet) {
					if (flags == Flags.REQUIRED) {
						roaDefinition.setMinOccurs(1);
					}
					if (flags == Flags.MULTIVALUED) {
						roaDefinition.setMaxOccurs(-1);
					}
				}

			}

		}

		return mpSchema;
	}

	
//			StringBuilder xsdSb = new StringBuilder();
//		xsdSb.append("<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
//				+ "targetNamespace=\"");
//		xsdSb.append(schemaNamespace);
//		xsdSb.append("\" xmlns:tns=\"");
//		xsdSb.append(schemaNamespace);
//		xsdSb.append("\" xmlns:idfs=\"");
//		xsdSb.append(SchemaConstants.NS_ICF_SCHEMA);
//		xsdSb.append("\" xmlns:r=\"");
//		xsdSb.append(SchemaConstants.NS_RESOURCE);
//		xsdSb.append("\" elementFormDefault=\"qualified\">\n");
//
//		xsdSb.append("<xsd:import namespace=\"");
//		xsdSb.append(SchemaConstants.NS_ICF_SCHEMA);
//		xsdSb.append("\"/>\n");
//
//		Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
//
//		for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
//			xsdSb.append("\n\n");
//
//			boolean hasName = false;
//
//			String objectClassXsdName;
//
//			if (ObjectClass.ACCOUNT_NAME.equals(objectClassInfo.getType())) {
//				objectClassXsdName = "AccountObjectClass";
//			} else if (ObjectClass.GROUP_NAME.equals(objectClassInfo.getType())) {
//				objectClassXsdName = "GroupObjectClass";
//			} else {
//				objectClassXsdName = objectClassInfo.getType() + "MiscObjectClass";
//			}
//
//			xsdSb.append("<xsd:complexType name=\"");
//			xsdSb.append(objectClassXsdName);
//			xsdSb.append("\"\n");
//
//			Set<AttributeInfo> attributeInfoSet = objectClassInfo.getAttributeInfo();
//			for (AttributeInfo attributeInfo : attributeInfoSet) {
//
//				if (Name.NAME.equals(attributeInfo.getName())) {
//					hasName = true;
//					continue;
//				}
//
//				String attrXsdName = attributeInfo.getName();
//				String attrXsdType = mapType(attributeInfo.getType());
//				StringBuffer xsdConstraints = new StringBuffer();
//
//				Set<Flags> flagsSet = attributeInfo.getFlags();
//
//				System.out.println(flagsSet);
//
//				boolean required = false;
//				for (Flags flags : flagsSet) {
//					if (flags == Flags.REQUIRED) {
//						required = true;
//					}
//					if (flags == Flags.MULTIVALUED) {
//						xsdConstraints.append("maxOccurs=\"unbounded\" ");
//					}
//				}
//				if (!required) {
//					xsdConstraints.append("minOccurs=\"0\" ");
//				}
//
//				if ("__PASSWORD__".equals(attributeInfo.getName())) {
//					xsdSb.append("        <xsd:element ref=\"idc:password\" " + xsdConstraints + "/>\n");
//				} else {
//					xsdSb.append("        <xsd:element name=\"" + attrXsdName + "\" type=\"" + attrXsdType + "\" " + xsdConstraints + "/>\n");
//				}
//			}
//
//			xsdSb.append("      </xsd:sequence>\n"
//					+ "    </xsd:extension>\n"
//					+ "  </xsd:complexContent>\n"
//					+ "</xsd:complexType>\n");
//
//			System.out.println(objectClassInfo.getType() + " Has name: " + hasName);
//		}
//
//
//		System.out.println("===================================================");
//		System.out.println(xsdSb);
//		System.out.println("===================================================");

	
	private static void initTypeMap() {
		if (xsdTypeMap!=null) { 
			return;
		}
		
        xsdTypeMap = new HashMap();
        xsdTypeMap.put(String.class, SchemaConstants.XSD_STRING);
        xsdTypeMap.put(int.class, SchemaConstants.XSD_INTEGER);
        xsdTypeMap.put(boolean.class, SchemaConstants.XSD_BOOLEAN);
		xsdTypeMap.put(byte[].class, SchemaConstants.XSD_BASE64BINARY);
        xsdTypeMap.put(org.identityconnectors.common.security.GuardedString.class,SchemaConstants.R_PROTECTED_STRING_TYPE);
    }
	
    private QName mapType(Class idConnType) {
        QName xsdType = xsdTypeMap.get(idConnType);
        if (xsdType==null) {
            throw new IllegalArgumentException("No XSD mapping for ICF type "+idConnType.getCanonicalName());
        }
        return xsdType;
    }


	@Override
	public ResourceObject fetchObject(Set<ResourceObjectAttribute> identifiers) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<ResourceObjectAttribute> addObject(ResourceObject object, Set<Operation> additionalOperations) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void modifyObject(Set<ResourceObjectAttribute> identifiers, Set<Operation> changes) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void deleteObject(Set<ResourceObjectAttribute> identifiers) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Token deserializeToken(String serializedToken) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Token fetchCurrentToken() throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<Change> fetchChanges(Token lastToken) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceTestResultType test() {
		ResourceTestResultType result = new ResourceTestResultType();

		TestResultType testResult = new TestResultType();
		result.setConnectorConnection(testResult);
		try {
			connector.test();
			testResult.setSuccess(true);
		} catch (RuntimeException ex) {
			testResult.setSuccess(false);
			List<JAXBElement<DiagnosticsMessageType>> errorOrWarning = testResult.getErrorOrWarning();
			DiagnosticsMessageType message = new DiagnosticsMessageType();
			message.setMessage(ex.getClass().getName() + ": " + ex.getMessage());
			// TODO: message.setDetails();
			JAXBElement<DiagnosticsMessageType> element = new JAXBElement<DiagnosticsMessageType>(SchemaConstants.I_DIAGNOSTICS_MESSAGE_ERROR, DiagnosticsMessageType.class, message);
			errorOrWarning.add(element);
		}

		return result;
	}
}
