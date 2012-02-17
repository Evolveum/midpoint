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

package com.evolveum.midpoint.repo.api.query;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.SchemaConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;


/**
 * @author lazyman
 */
public class SimpleFilter<T> implements QueryFilter {

    private QName type;
    private PropertyPath propertyPath;
    private SimpleFilterType filterType;
    private PrismPropertyValue<T> value;

    public SimpleFilter() {
    }

    public SimpleFilter(SimpleFilterType filterType, PropertyPath propertyPath) {
        this(filterType, propertyPath, null);
    }

    public SimpleFilter(SimpleFilterType filterType, PropertyPath propertyPath, PrismPropertyValue<T> value) {
        this.filterType = filterType;
        this.propertyPath = propertyPath;
        this.value = value;
    }

    public SimpleFilterType getFilterType() {
        return filterType;
    }

    public PropertyPath getPropertyPath() {
        return propertyPath;
    }

    public PrismPropertyValue<T> getValue() {
        return value;
    }

    public void setFilterType(SimpleFilterType filterType) {
        this.filterType = filterType;
    }

    public void setPropertyPath(PropertyPath propertyPath) {
        this.propertyPath = propertyPath;
    }

    public void setValue(PrismPropertyValue<T> value) {
        this.value = value;
    }

    @Override
    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    @Override
    public void toDOM(Element parent) {
        Document document = parent.getOwnerDocument();
        Element element = document.createElementNS(SchemaConstants.NS_COMMON, getFilterType().getElementName());
        parent.appendChild(element);

        //todo implement
    }
}
