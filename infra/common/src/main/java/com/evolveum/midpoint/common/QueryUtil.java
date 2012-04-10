/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author semancik
 */
public class QueryUtil {

    @Deprecated
    public static Element createTypeFilter(Document doc, String uri) {
        Validate.notNull(doc);
        Validate.notNull(uri);
        Validate.notEmpty(uri);

        Element type = doc.createElementNS(SchemaConstants.C_FILTER_TYPE.getNamespaceURI(), SchemaConstants.C_FILTER_TYPE.getLocalPart());
        type.setAttributeNS(SchemaConstants.C_FILTER_TYPE_URI.getNamespaceURI(), SchemaConstants.C_FILTER_TYPE_URI.getLocalPart(), uri);
        return type;
    }

    /**
     * Creates "equal" filter segment for multi-valued properties based on DOM representation.
     *
     * @param doc
     * @param xpath  property container xpath. may be null.
     * @param values
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException
     */
    public static Element createEqualFilterFromElements(Document doc, XPathHolder xpath, List<?> values, PrismContext prismContext) throws
            SchemaException {
        Validate.notNull(doc);
        Validate.notNull(values);
        Validate.notEmpty(values);

        Element equal = doc.createElementNS(SchemaConstants.C_FILTER_EQUAL.getNamespaceURI(), SchemaConstants.C_FILTER_EQUAL.getLocalPart());
        Element value = doc.createElementNS(SchemaConstants.C_FILTER_VALUE.getNamespaceURI(), SchemaConstants.C_FILTER_VALUE.getLocalPart());
        for (Object val : values) {
            Element domElement;
            try {
            	domElement = prismContext.getPrismJaxbProcessor().toDomElement(val, doc);
//                domElement = JAXBUtil.toDomElement(val);
            } catch (JAXBException e) {
                throw new SchemaException("Unexpected JAXB problem while creating search filer for value " + val, e);
            }
            value.appendChild(doc.importNode(domElement, true));
        }
        if (xpath != null) {
            Element path = xpath.toElement(SchemaConstants.C_FILTER_PATH, doc);
            equal.appendChild(doc.importNode(path, true));
        }
        equal.appendChild(doc.importNode(value, true));
        return equal;
    }

    /**
     * Creates "equal" filter segment for single-valued properties based on DOM representation.
     * Parameter object is either DOM or JAXB element
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, Object object) throws SchemaException {
        Validate.notNull(doc);
        Validate.notNull(object);

        //todo this was bad recursion
//        List<Object> values = new ArrayList<Object>();
//        values.add(value);
//        return createEqualFilter(doc, xpath, values);
        
        //todo bad quick fix HACK
        Element equal = doc.createElementNS(SchemaConstants.C_FILTER_EQUAL.getNamespaceURI(), SchemaConstants.C_FILTER_EQUAL.getLocalPart());
        Element value = doc.createElementNS(SchemaConstants.C_FILTER_VALUE.getNamespaceURI(), SchemaConstants.C_FILTER_VALUE.getLocalPart());
        equal.appendChild(value);

        if (object instanceof Element) {
	        Element domElement= (Element)object;
	        value.appendChild(doc.importNode(domElement, true));
        } else {
        	throw new UnsupportedOperationException("Unsupported element type "+object.getClass());
        }
        
        if (xpath != null) {
            Element path = xpath.toElement(SchemaConstants.C_FILTER_PATH, doc);
            equal.appendChild(path);
        }
        
        return equal;
    }

    /**
     * Creates "equal" filter segment for single-valued properties with string content.
     *
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param value
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, QName properyName, String value) throws
            SchemaException {
        Validate.notNull(doc);
        Validate.notNull(properyName);
        Validate.notNull(value);

        Element element = doc.createElementNS(properyName.getNamespaceURI(), properyName.getLocalPart());
        element.setTextContent(value);
        return createEqualFilter(doc, xpath, element);
    }

    /**
     * Creates "equal" filter segment for single-valued properties with QName content.
     *
     * @param doc
     * @param xpath property container xpath. may be null.
     * @param value
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException
     */
    public static Element createEqualFilter(Document doc, XPathHolder xpath, QName properyName, QName value) throws
            SchemaException {
        Validate.notNull(doc);
        Validate.notNull(properyName);
        Validate.notNull(value);

        Element element = doc.createElementNS(properyName.getNamespaceURI(), properyName.getLocalPart());
        DOMUtil.setQNameValue(element, value);
        return createEqualFilter(doc, xpath, element);
    }

    /**
     * Creates "equal" filter for object reference.
     *
     * @param doc
     * @param xpath        property container xpath. may be null.
     * @param propertyName name of the reference property (e.g. "resourceRef")
     * @param oid          OID of the referenced object
     * @return "equal" filter segment (as DOM)
     * @throws JAXBException
     */
    public static Element createEqualRefFilter(Document doc, XPathHolder xpath, QName propertyName, String oid) throws
            SchemaException {
        Element value = doc.createElementNS(propertyName.getNamespaceURI(), propertyName.getLocalPart());
        value.setAttributeNS(SchemaConstants.C_OID_ATTRIBUTE.getNamespaceURI(), SchemaConstants.C_OID_ATTRIBUTE.getLocalPart(), oid);
        return createEqualFilter(doc, xpath, value);
    }

    public static Element createAndFilter(Document doc, Element... conditions) {
        Validate.notNull(doc);
        Validate.notNull(conditions);

        Element and = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(), SchemaConstants.C_FILTER_AND.getLocalPart());
        for (Element condition : conditions) {
            Validate.notNull(condition);
            and.appendChild(condition);
        }

        return and;
    }

//    public static Element createAndFilter(Document doc, Element el1, Element el2) {
//        Validate.notNull(doc);
//        Validate.notNull(el1);
//        Validate.notNull(el2);
//
//        Element and = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(), SchemaConstants.C_FILTER_AND.getLocalPart());
//        and.appendChild(el1);
//        and.appendChild(el2);
//        return and;
//    }
//
//    public static Element createAndFilter(Document doc, Element el1, Element el2, Element el3) {
//        Validate.notNull(doc);
//        Validate.notNull(el1);
//        Validate.notNull(el2);
//        Validate.notNull(el3);
//
//        Element and = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(), SchemaConstants.C_FILTER_AND.getLocalPart());
//        and.appendChild(el1);
//        and.appendChild(el2);
//        and.appendChild(el3);
//        return and;
//    }

	public static QueryType createNameQuery(String name) throws SchemaException {
		Document doc = DOMUtil.getDocument();
        Element filter = QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, name);
        QueryType query = new QueryType();
        query.setFilter(filter);
        return query;
	}
	
	public static QueryType createNameQuery(ObjectType object) throws SchemaException {
		return createNameQuery(object.getName());
	}

    @Deprecated
    public static <T extends ObjectType> Element createNameAndClassFilter(Class<T> type, String name) throws
            SchemaException {
        Document doc = DOMUtil.getDocument();
        return QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, name);
    }

    public static QueryType createQuery(Element filter) {
        QueryType query = new QueryType();
        query.setFilter(filter);
        return query;
    }

	/**
	 * Returns query that returns all objects.
	 */
	public static QueryType createAllObjectsQuery() {
		// Create empty filter. This returns all objects of a type given as an argument to searchObjects.
		return new QueryType();
	}

	public static QueryType createResourceAndAccountQuery(ResourceType resource, QName objectClass, String accountType) throws SchemaException {
		Document doc = DOMUtil.getDocument();
        Element filter =
                QueryUtil.createAndFilter(doc,
                        // TODO: The account type is hardcoded now, it should determined
                        // from the schema later, or maybe we can make it entirely
                        // generic (use ResourceObjectShadowType instead).
                        QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF, resource.getOid()),
                        QueryUtil.createEqualFilter(doc, null, SchemaConstants.I_OBJECT_CLASS, objectClass)
                );

        QueryType query = new QueryType();
        query.setFilter(filter);

        return query;
	}
	
	public static QueryType createAttributeQuery(PrismProperty<?> attribute,
			QName objectClass, ResourceType resourceType, PrismContext prismContext) throws SchemaException {
		// We have all the data, we can construct the filter now
		// TODO: add objectClass to the criteria FIXME
		Document doc = DOMUtil.getDocument();
		XPathHolder xpath = new XPathHolder(ResourceObjectShadowType.F_ATTRIBUTES);
		List<Element> identifierElements = prismContext.getPrismDomProcessor().serializeItemToDom(attribute, doc);
		Element filter = createAndFilter(doc, QueryUtil.createEqualRefFilter(doc, null,
					SchemaConstants.I_RESOURCE_REF, resourceType.getOid()), QueryUtil
					.createEqualFilterFromElements(doc, xpath, identifierElements, prismContext));
		QueryType query = new QueryType();
		query.setFilter(filter);
		return query;
	}


	public static String dump(QueryType query) {
		StringBuilder sb = new StringBuilder("Query(");
		sb.append(query.getDescription()).append("):\n");
		if (query.getFilter() != null)
			sb.append(DOMUtil.serializeDOMToString(query.getFilter()));
		else
			sb.append("(no filter)");
		return sb.toString();
	}

}
