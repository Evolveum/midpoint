/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.api.query;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;


/**
 * @author lazyman
 */
public class SimpleFilter<T> implements QueryFilter {

    private QName type;
    private ItemPath propertyPath;
    private SimpleFilterType filterType;
    private PrismPropertyValue<T> value;

    public SimpleFilter() {
    }

    public SimpleFilter(SimpleFilterType filterType, ItemPath propertyPath) {
        this(filterType, propertyPath, null);
    }

    public SimpleFilter(SimpleFilterType filterType, ItemPath propertyPath, PrismPropertyValue<T> value) {
        this.filterType = filterType;
        this.propertyPath = propertyPath;
        this.value = value;
    }

    public SimpleFilterType getFilterType() {
        return filterType;
    }

    public ItemPath getPropertyPath() {
        return propertyPath;
    }

    public PrismPropertyValue<T> getValue() {
        return value;
    }

    public void setFilterType(SimpleFilterType filterType) {
        this.filterType = filterType;
    }

    public void setPropertyPath(ItemPath propertyPath) {
        this.propertyPath = propertyPath;
    }

    public void setValue(PrismPropertyValue<T> value) {
        this.value = value;
    }

    @Override
    public QName getType() {
        return type;
    }

    @Override
    public void setType(QName type) {
        this.type = type;
    }

    @Override
    public void toDOM(Element parent) {
        Document document = parent.getOwnerDocument();
        Element element = document.createElementNS(SchemaConstantsGenerated.NS_COMMON, getFilterType().getElementName());
        parent.appendChild(element);

        //todo implement
    }
}
