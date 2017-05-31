/*
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
package com.evolveum.midpoint.model.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class ModelClientUtil {
	
	// XML constants
	public static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    public static final String NS_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";
    public static final String NS_RI = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
    public static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
	
    public static final QName COMMON_PATH = new QName(NS_COMMON, "path");
	public static final QName COMMON_VALUE = new QName(NS_COMMON, "value");
    public static final QName COMMON_GIVEN_NAME = new QName(NS_COMMON, "givenName");
	public static final QName COMMON_ASSIGNMENT = new QName(NS_COMMON, "assignment");
	
	private static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");
    public static final QName TYPES_CLEAR_VALUE = new QName(NS_TYPES, "clearValue");

	private static final DocumentBuilder domDocumentBuilder;
	private static final JAXBContext jaxbContext;
	
	public static JAXBContext instantiateJaxbContext() throws JAXBException {
		return JAXBContext.newInstance("com.evolveum.midpoint.xml.ns._public.common.api_types_3:" +
				"com.evolveum.midpoint.xml.ns._public.common.common_3:" +
				"com.evolveum.midpoint.xml.ns._public.common.fault_3:" +
				"com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3:" +
				"com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_3:" +
				"com.evolveum.midpoint.xml.ns._public.resource.capabilities_3:" +
				"com.evolveum.prism.xml.ns._public.annotation_3:" +
				"com.evolveum.prism.xml.ns._public.query_3:" +
				"com.evolveum.prism.xml.ns._public.types_3");
	}
	
	public static Element createPathElement(String stringPath, Document doc) {
		String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
		return createTextElement(COMMON_PATH, pathDeclaration, doc);
	}

    public static ItemPathType createItemPathType(String stringPath) {
        ItemPathType itemPathType = new ItemPathType();
        String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
        itemPathType.setValue(pathDeclaration);
        return itemPathType;
    }

    public static SearchFilterType parseSearchFilterType(String filterClauseAsXml) throws IOException, SAXException, JAXBException {
        Element filterClauseAsElement = parseElement(filterClauseAsXml);
        SearchFilterType searchFilterType = new SearchFilterType();
        searchFilterType.setFilterClause(filterClauseAsElement);
        return searchFilterType;
    }

    public static PolyStringType createPolyStringType(String string, Document doc) {
		PolyStringType polyStringType = new PolyStringType();
		polyStringType.getContent().add(string);
		return polyStringType;
	}
    
    public static PolyStringType createPolyStringType(String string) {
		return createPolyStringType(string, getDocumnent());
	}
    
    public static String getOrig(PolyStringType polyStringType) {
        if (polyStringType == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object o : polyStringType.getContent()) {
            if (o instanceof String) {
                sb.append(o);
            } else if (o instanceof Element) {
                Element e = (Element) o;
                if ("orig".equals(e.getLocalName())) {
                    return e.getTextContent();
                }
            } else if (o instanceof JAXBElement) {
                JAXBElement je = (JAXBElement) o;
                if ("orig".equals(je.getName().getLocalPart())) {
                    return (String) je.getValue();
                }
            }
        }
        return sb.toString();
    }
	
	public static Element createTextElement(QName qname, String value, Document doc) {
		Element element = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
		element.setTextContent(value);
		return element;
	}
	
	public static CredentialsType createPasswordCredentials(String password) {
		CredentialsType credentialsType = new CredentialsType();
		credentialsType.setPassword(createPasswordType(password));
		return credentialsType;
	}
	
	public static PasswordType createPasswordType(String password) {
		PasswordType passwordType = new PasswordType();
		passwordType.setValue(createProtectedString(password));
		return passwordType;
	}

	public static ProtectedStringType createProtectedString(String clearValue) {
		ProtectedStringType protectedString = new ProtectedStringType();
        // this is a bit of workaround: it should be possible to add clearValue by itself, but there seems to be a parsing bug on the server side that needs to be fixed first (TODO)
		protectedString.getContent().add(toJaxbElement(TYPES_CLEAR_VALUE, clearValue));
		return protectedString;
	}

	public static <T> JAXBElement<T> toJaxbElement(QName name, T value) {
		return new JAXBElement<T>(name, (Class<T>) value.getClass(), value);
	}

	public static Document getDocumnent() {
		return domDocumentBuilder.newDocument();
	}

	public static String getTypeUri(Class<? extends ObjectType> type) {
		String typeUri = NS_COMMON + "#" + type.getSimpleName();
		return typeUri;
	}
	
	public static QName getTypeQName(Class<? extends ObjectType> type) {
		QName typeQName = new QName(NS_COMMON, type.getSimpleName());
		return typeQName;
	}

	public static QName getElementName(Class<? extends ObjectType> type) {
		String local = type.getSimpleName();
		int typeIndex = local.lastIndexOf("Type");
		if (typeIndex > 0) {
			local = local.substring(0, typeIndex);
		}
		if (Character.isUpperCase(local.charAt(0))) {
			local = local.substring(0,1).toLowerCase() + local.substring(1);
		}
		return new QName(NS_COMMON, local);
	}	
	
	public static Element parseElement(String stringXml) throws SAXException, IOException {
		Document document = domDocumentBuilder.parse(IOUtils.toInputStream(stringXml, "utf-8"));
		return getFirstChildElement(document);
	}

	public static Element getFirstChildElement(Node parent) {
		if (parent == null || parent.getChildNodes() == null) {
			return null;
		}

		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) child;
			}
		}

		return null;
	}

    /**
     * Retrieves OID created by model Web Service from the returned list of ObjectDeltaOperations.
     *
     * @param operationListType result of the model web service executeChanges call
     * @param originalDelta original request used to find corresponding ObjectDeltaOperationType instance. Must be of ADD type.
     * @return OID if found
     *
     * PRELIMINARY IMPLEMENTATION. Currently the first returned ADD delta with the same object type as original delta is returned.
     */
    public static String getOidFromDeltaOperationList(ObjectDeltaOperationListType operationListType, ObjectDeltaType originalDelta) {
        ObjectDeltaOperationType odo = findInDeltaOperationList(operationListType, originalDelta);
        return odo != null ? ((ObjectType) odo.getObjectDelta().getObjectToAdd()).getOid() : null;
    }

    public static ObjectDeltaOperationType findInDeltaOperationList(ObjectDeltaOperationListType operationListType, ObjectDeltaType originalDelta) {
        Validate.notNull(operationListType);
        Validate.notNull(originalDelta);
        if (originalDelta.getChangeType() != ChangeTypeType.ADD) {
            throw new IllegalArgumentException("Original delta is not of ADD type");
        }
        if (originalDelta.getObjectToAdd() == null) {
            throw new IllegalArgumentException("Original delta contains no object-to-be-added");
        }
        for (ObjectDeltaOperationType operationType : operationListType.getDeltaOperation()) {
            ObjectDeltaType objectDeltaType = operationType.getObjectDelta();
            if (objectDeltaType.getChangeType() == ChangeTypeType.ADD &&
                    objectDeltaType.getObjectToAdd() != null) {
                ObjectType objectAdded = (ObjectType) objectDeltaType.getObjectToAdd();
                if (objectAdded.getClass().equals(originalDelta.getObjectToAdd().getClass())) {
                    return operationType;
                }
            }
        }
        return null;
    }
    
    public static <O extends ObjectType> ObjectDeltaListType createModificationDeltaList(Class<O> type, String oid, 
    		String path, ModificationTypeType modType, Object... values) {
		ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setObjectType(getTypeQName(type));
    	delta.setChangeType(ChangeTypeType.MODIFY);
    	delta.setOid(oid);
    	ItemDeltaType itemDelta = new ItemDeltaType();
    	itemDelta.setPath(ModelClientUtil.createItemPathType(path));
    	itemDelta.setModificationType(modType);
    	itemDelta.getValue().addAll(Arrays.asList(values));
		delta.getItemDelta().add(itemDelta);
		deltaList.getDelta().add(delta);
		return deltaList;
	}
    
    @Deprecated
    public static <O extends ObjectType, T extends ObjectType> ObjectDeltaListType createAssignDeltaList(Class<O> focusType, String focusOid, 
    		Class<T> targetType, String targetOid) {
    	return createAssignmentDeltaList(focusType, focusOid, targetType, targetOid, ModificationTypeType.ADD);
    }

    @Deprecated
    public static <O extends ObjectType, T extends ObjectType> ObjectDeltaListType createUnassignDeltaList(Class<O> focusType, String focusOid, 
    		Class<T> targetType, String targetOid) {
    	return createAssignmentDeltaList(focusType, focusOid, targetType, targetOid, ModificationTypeType.DELETE);
    }

    @Deprecated
    private static <O extends ObjectType, T extends ObjectType> ObjectDeltaListType createAssignmentDeltaList(Class<O> focusType, String focusOid, 
    		Class<T> targetType, String targetOid, ModificationTypeType modificationType) {
    	AssignmentType assignment = new AssignmentType();
    	ObjectReferenceType targetRef = new ObjectReferenceType();
    	targetRef.setOid(targetOid);
    	targetRef.setType(getTypeQName(targetType));
		assignment.setTargetRef(targetRef);
    	return createModificationDeltaList(focusType, focusOid, "assignment", modificationType, assignment);
    }
    
    public static <O> O unmarshallResource(String path) throws JAXBException, FileNotFoundException {
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller(); 
		 
		InputStream is = null;
		JAXBElement<O> element = null;
		try {
			is = ModelClientUtil.class.getClassLoader().getResourceAsStream(path);
			if (is == null) {
				throw new FileNotFoundException("System resource "+path+" was not found");
			}
			element = (JAXBElement<O>) unmarshaller.unmarshal(is);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
		if (element == null) {
			return null;
		}
		return element.getValue();
	}

    public static <O> O unmarshallFile(File file) throws JAXBException, FileNotFoundException {
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller(); 
		 
		InputStream is = null;
		JAXBElement<O> element = null;
		try {
			is = new FileInputStream(file);
			element = (JAXBElement<O>) unmarshaller.unmarshal(is);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}
		if (element == null) {
			return null;
		}
		return element.getValue();
	}
    
    public static <O extends ObjectType> String marshallToSting(O object) throws JAXBException {
    	return marshallToSting(object, true);
    }
    
    public static <O extends ObjectType> String marshallToSting(O object, boolean formatted) throws JAXBException {
    	Marshaller marshaller = jaxbContext.createMarshaller();
    	marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    	if (formatted) {
    		marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    	}
    	java.io.StringWriter sw = new StringWriter();
    	Class<O> type = (Class<O>) object.getClass();
    	JAXBElement<O> element = new JAXBElement<O>(getElementName(type), type, object);
    	marshaller.marshal(element, sw);
    	return sw.toString();
    }
    
    public static <T> String marshallToSting(QName elementName, T object, boolean formatted) throws JAXBException {
    	Marshaller marshaller = jaxbContext.createMarshaller();
    	marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    	if (formatted) {
    		marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    	}
    	java.io.StringWriter sw = new StringWriter();
    	JAXBElement<T> element = new JAXBElement<T>(elementName, (Class<T>) object.getClass(), object);
    	marshaller.marshal(element, sw);
    	return sw.toString();
    }
    
    public static <O extends ObjectType> ObjectDeltaType createRoleAssignDelta(Class<O> focusType, String focusOid, String... roleOids) {
    	return createRoleAssignmentDelta(focusType, focusOid, ModificationTypeType.ADD, roleOids);
    }
    
    public static <O extends ObjectType> ObjectDeltaType createRoleUnassignDelta(Class<O> focusType, String focusOid, String... roleOids) {
    	return createRoleAssignmentDelta(focusType, focusOid, ModificationTypeType.DELETE, roleOids);
    }
    
    public static <O extends ObjectType> ObjectDeltaType createRoleAssignmentDelta(Class<O> focusType, String focusOid, ModificationTypeType modType, String... roleOids) {
    	ItemDeltaType assignmentDelta = new ItemDeltaType();
    	assignmentDelta.setModificationType(modType);
    	assignmentDelta.setPath(ModelClientUtil.createItemPathType("assignment"));
    	for (String roleOid: roleOids) {
			assignmentDelta.getValue().add(createRoleAssignment(roleOid));
		}
    	ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(focusType));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(focusOid);
        deltaType.getItemDelta().add(assignmentDelta);
        return deltaType;
    }
    
    public static AssignmentType createRoleAssignment(String roleOid) {
		AssignmentType roleAssignment = new AssignmentType();
		ObjectReferenceType roleRef = new ObjectReferenceType();
		roleRef.setOid(roleOid);
		roleRef.setType(ModelClientUtil.getTypeQName(RoleType.class));
		roleAssignment.setTargetRef(roleRef);
		return roleAssignment;
	}
    
    public static <O extends ObjectType> ObjectDeltaType createConstructionAssignDelta(Class<O> focusType, String focusOid, String resourceOid) {
    	return createConstructionAssignmentDelta(focusType, focusOid, ModificationTypeType.ADD, resourceOid, null, null);
    }
    
    public static <O extends ObjectType> ObjectDeltaType createConstructionAssignDelta(Class<O> focusType, String focusOid, String resourceOid, ShadowKindType kind) {
    	return createConstructionAssignmentDelta(focusType, focusOid, ModificationTypeType.ADD, resourceOid, kind, null);
    }
    
    public static <O extends ObjectType> ObjectDeltaType createConstructionAssignDelta(Class<O> focusType, String focusOid, String resourceOid, ShadowKindType kind, String intent) {
    	return createConstructionAssignmentDelta(focusType, focusOid, ModificationTypeType.ADD, resourceOid, kind, intent);
    }

    public static <O extends ObjectType> ObjectDeltaType createConstructionAssignmentDelta(Class<O> focusType, String focusOid, ModificationTypeType modType, String resourceOid, ShadowKindType kind, String intent) {
    	ItemDeltaType assignmentDelta = new ItemDeltaType();
    	assignmentDelta.setModificationType(modType);
    	assignmentDelta.setPath(ModelClientUtil.createItemPathType("assignment"));
		assignmentDelta.getValue().add(createConstructionAssignment(resourceOid, kind, intent));
    	ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(focusType));
        deltaType.setChangeType(ChangeTypeType.MODIFY);
        deltaType.setOid(focusOid);
        deltaType.getItemDelta().add(assignmentDelta);
        return deltaType;
    }
    
    public static AssignmentType createConstructionAssignment(String resourceOid) {
    	return createConstructionAssignment(resourceOid, null, null);
    }

    public static AssignmentType createConstructionAssignment(String resourceOid, ShadowKindType kind) {
    	return createConstructionAssignment(resourceOid, kind, null);
    }
    
    public static AssignmentType createConstructionAssignment(String resourceOid, ShadowKindType kind, String intent) {
		AssignmentType assignment = new AssignmentType();
		ConstructionType construction = new ConstructionType();
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resourceOid);
		construction.setResourceRef(resourceRef);
		if (kind != null) {
			construction.setKind(kind);
		}
		if (intent != null) {
			construction.setIntent(intent);
		}
		assignment.setConstruction(construction);
		return assignment;
	}
    
    public static ObjectDeltaListType createDeltaList(ObjectDeltaType... deltas) {
    	ObjectDeltaListType list = new ObjectDeltaListType();
    	for (ObjectDeltaType delta: deltas) {
    		list.getDelta().add(delta);
    	}
    	return list;
    }
    
	public static SelectorQualifiedGetOptionsType createRootGetOptions(GetOperationOptionsType opt) {
		SelectorQualifiedGetOptionsType rootOpts = new SelectorQualifiedGetOptionsType();
		SelectorQualifiedGetOptionType selOpt = new SelectorQualifiedGetOptionType();
		selOpt.setOptions(opt);
		rootOpts.getOption().add(selOpt);
		return rootOpts;
	}
	
	public static GetOperationOptionsType createRawGetOption() {
		GetOperationOptionsType opts = new GetOperationOptionsType();
		opts.setRaw(Boolean.TRUE);
		return opts;
	}
	
	public static ModelExecuteOptionsType createRawExecuteOption() {
		ModelExecuteOptionsType opts = new ModelExecuteOptionsType();
		opts.setRaw(Boolean.TRUE);
		return opts;
	}

    public static <O extends ObjectType> String toString(O obj) {
		if (obj == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		String className = obj.getClass().getSimpleName();
		if (className.endsWith("Type")) {
			className = className.substring(0, className.lastIndexOf("Type")).toLowerCase();
		}
		sb.append(className);
		sb.append("(");
		sb.append(toString(obj.getName()));
		sb.append(":");
		sb.append(obj.getOid());
		sb.append(")");
		return sb.toString();
	}
	
	public static String toString(PolyStringType poly) {
		if (poly == null) {
			return null;
		}
		return getOrig(poly);
	}

	
	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			domDocumentBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("Error creating XML document " + e.getMessage());
		}
		
		try {
			jaxbContext = ModelClientUtil.instantiateJaxbContext();
		} catch (JAXBException e) {
			throw new IllegalStateException("Error creating JAXB context " + e.getMessage());
		}
	}

	
}
