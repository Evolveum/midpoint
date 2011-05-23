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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author semancik
 */
public class QueryUtil {

    public static Element createTypeFilter(Document doc, String uri) {
        Validate.notNull(doc);
        Validate.notNull(uri);
        Validate.notEmpty(uri);

        Element type = doc.createElementNS(SchemaConstants.C_FILTER_TYPE.getNamespaceURI(), SchemaConstants.C_FILTER_TYPE.getLocalPart());
        type.setAttributeNS(SchemaConstants.C_FILTER_TYPE_URI.getNamespaceURI(), SchemaConstants.C_FILTER_TYPE_URI.getLocalPart(), uri);
        return type;
    }

    public static Element createEqualFilter(Document doc, XPathType xpath, List<Element> values) {
        Validate.notNull(doc);
        Validate.notNull(values);
        Validate.notEmpty(values);

        Element equal = doc.createElementNS(SchemaConstants.C_FILTER_EQUAL.getNamespaceURI(), SchemaConstants.C_FILTER_EQUAL.getLocalPart());
        Element value = doc.createElementNS(SchemaConstants.C_FILTER_VALUE.getNamespaceURI(), SchemaConstants.C_FILTER_VALUE.getLocalPart());
        for (Element val : values) {
            value.appendChild(doc.importNode(val, true));
        }
        if (xpath != null) {
            Element path = xpath.toElement(SchemaConstants.C_FILTER_PATH, doc);
            equal.appendChild(doc.importNode(path, true));
        }
        equal.appendChild(doc.importNode(value, true));
        return equal;
    }

    public static Element createEqualFilter(Document doc, XPathType xpath, Element value) {
        Validate.notNull(doc);
        Validate.notNull(value);

        List<Element> values = new ArrayList<Element>();
        values.add(value);
        return createEqualFilter(doc, xpath, values);
    }

    public static Element createAndFilter(Document doc, Element el1, Element el2) {
        Validate.notNull(doc);
        Validate.notNull(el1);
        Validate.notNull(el2);

        Element and = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(), SchemaConstants.C_FILTER_AND.getLocalPart());
        and.appendChild(el1);
        and.appendChild(el2);
        return and;
    }
}
