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

package com.evolveum.midpoint.schema.holder;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueFilterType;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author semancik
 */
public class ValueAssignmentHolder {

    private Element dom;
    private ExpressionHolder source;
    private XPathHolder target;
    private List<ValueFilterType> filters;

    public ValueAssignmentHolder(Element dom) {
        this.dom = dom;
        source = null;
        target = null;
        filters = null;
    }

    public ExpressionHolder getSource() {
        if (source != null) {
            return source;
        }
        NodeList elements = dom.getElementsByTagNameNS(SchemaConstants.I_VALUE_ASSIGNMENT_SOURCE.getNamespaceURI(), SchemaConstants.I_VALUE_ASSIGNMENT_SOURCE.getLocalPart());
        if (elements.getLength() == 0) {
            return null;
        }
        if (elements.getLength() > 1) {
            throw new IllegalArgumentException("Value assignment has more than one elements <source>");
        }
        Element element = (Element) elements.item(0);
        source = new ExpressionHolder(element);
        return source;
    }

    public XPathHolder getTarget() {
        if (target != null) {
            return target;
        }
        NodeList elements = dom.getElementsByTagNameNS(SchemaConstants.I_VALUE_ASSIGNMENT_TARGET.getNamespaceURI(), SchemaConstants.I_VALUE_ASSIGNMENT_TARGET.getLocalPart());
        if (elements.getLength() == 0) {
            return null;
        }
        if (elements.getLength() > 1) {
            throw new IllegalArgumentException("Value assignment has more than one elements <target>");
        }
        Element element = (Element) elements.item(0);
        target = new XPathHolder(element);
        return target;
    }

    public List<ValueFilterType> getFilter() {
        if (filters != null) {
            return filters;
        }
        NodeList elements = dom.getElementsByTagNameNS(SchemaConstants.I_VALUE_ASSIGNMENT_FILTER.getNamespaceURI(), SchemaConstants.I_VALUE_ASSIGNMENT_FILTER.getLocalPart());
        if (elements.getLength() == 0) {
            return null;
        }

        try {
            JAXBContext jctx = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jctx.createUnmarshaller();

            List<ValueFilterType> newFilters = new ArrayList<ValueFilterType>();
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                Object object = unmarshaller.unmarshal(element);
                if (object instanceof JAXBElement) {
                    JAXBElement jaxbElement = (JAXBElement) object;
                    object = jaxbElement.getValue();
                }
                if (object instanceof ValueFilterType) {
                    newFilters.add((ValueFilterType) object);
                } else {
                    throw new IllegalArgumentException("Filter is not ValueFilterType, it is " + object.getClass().getName());
                }
            }
            filters = newFilters;
            return filters;

        } catch (JAXBException ex) {
            throw new IllegalStateException("Cannot create unmarshaller");
        }
    }
}
