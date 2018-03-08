/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.testing.sanity;

import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * @author Radovan Semancik
 *
 */
public class ModelClientUtil {

	// XML constants
	public static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
	public static final QName COMMON_PATH = new QName(NS_COMMON, "path");
	public static final QName COMMON_VALUE = new QName(NS_COMMON, "value");
    public static final QName COMMON_GIVEN_NAME = new QName(NS_COMMON, "givenName");
	public static final QName COMMON_ASSIGNMENT = new QName(NS_COMMON, "assignment");

	public static final String NS_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";
	private static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");
    public static final QName TYPES_CLEAR_VALUE = new QName(NS_TYPES, "clearValue");

	private static final DocumentBuilder domDocumentBuilder;

	public static JAXBContext instantiateJaxbContext() throws JAXBException {
		return JAXBContext.newInstance("com.evolveum.midpoint.xml.ns._public.common.api_types_3:" +
				"com.evolveum.midpoint.xml.ns._public.common.common_3:" +
				"com.evolveum.midpoint.xml.ns._public.common.fault_3:" +
				"com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3:" +
				"com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_3:" +
				"com.evolveum.midpoint.xml.ns._public.resource.capabilities_3:" +
				"com.evolveum.prism.xml.ns._public.annotation_3:" +
				"com.evolveum.prism.xml.ns._public.query_3:" +
				"com.evolveum.prism.xml.ns._public.types_3:" +
				"org.w3._2000._09.xmldsig:" +
				"org.w3._2001._04.xmlenc");
	}

	public static Element createPathElement(String stringPath, Document doc) {
		String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
		return createTextElement(COMMON_PATH, pathDeclaration, doc);
	}

    public static ItemPathType createItemPathType(String stringPath) {
        String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
        ItemPathType itemPathType = new ItemPathType(pathDeclaration);
        return itemPathType;
    }

//    public static SearchFilterType parseSearchFilterType(String filterClauseAsXml) throws IOException, SAXException {
//        Element filterClauseAsElement = parseElement(filterClauseAsXml);
//        SearchFilterType searchFilterType = new SearchFilterType();
//        searchFilterType.setFilterClause(filterClauseAsElement);
//        return searchFilterType;
//    }

    public static PolyStringType createPolyStringType(String string, Document doc) {
		PolyStringType polyStringType = new PolyStringType();
		polyStringType.setOrig(string);
		return polyStringType;
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
        protectedString.setClearValue(clearValue);
        return protectedString;	}

	public static <T> JAXBElement<T> toJaxbElement(QName name, T value) {
		return new JAXBElement<>(name, (Class<T>) value.getClass(), value);
	}

	public static Document getDocumnent() {
		return domDocumentBuilder.newDocument();
	}

	public static String getTypeUri(Class<? extends ObjectType> type) {
//		QName typeQName = JAXBUtil.getTypeQName(type);
//		String typeUri = QNameUtil.qNameToUri(typeQName);
		String typeUri = NS_COMMON + "#" + type.getSimpleName();
		return typeUri;
	}

	public static QName getTypeQName(Class<? extends ObjectType> type) {
//		QName typeQName = JAXBUtil.getTypeQName(type);
		QName typeQName = new QName(NS_COMMON, type.getSimpleName());
		return typeQName;
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
                    return objectAdded.getOid();
                }
            }
        }
        return null;
    }

	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			domDocumentBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error creating XML document " + ex.getMessage());
		}
	}

}
