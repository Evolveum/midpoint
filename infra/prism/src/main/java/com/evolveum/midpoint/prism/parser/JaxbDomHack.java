/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.parser;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class JaxbDomHack {
	
	private DOMParser domParser;

	public JaxbDomHack(DOMParser domParser) {
		super();
		this.domParser = domParser;
	}

	public <V extends PrismValue,C extends Containerable> Item<V> parseRawElement(Object element, PrismContainerDefinition<C> definition) throws SchemaException {
		Validate.notNull(definition, "Attempt to parse raw element in a container without definition");
		
		QName elementName = JAXBUtil.getElementQName(element);
		ItemDefinition itemDefinition = definition.findItemDefinition(elementName);
		if (itemDefinition == null) {
			throw new SchemaException("No definition for item "+elementName+" in container "+definition+" (parsed from raw element)", elementName);
		}
		PrismContext prismContext = definition.getPrismContext();
		Item<V> subItem;
		if (element instanceof Element) {
			// DOM Element
			DOMParser domParser = prismContext.getParserDom();
			XNode xnode = domParser.parseElement((Element)element);
			subItem = prismContext.getXnodeProcessor().parseItem(xnode, elementName, itemDefinition);
		} else if (element instanceof JAXBElement<?>) {
			// JAXB Element
			JAXBElement<?> jaxbElement = (JAXBElement<?>)element;
			Object jaxbBean = jaxbElement.getValue();
			if (itemDefinition instanceof PrismPropertyDefinition<?>) {
				// property
				PrismProperty<?> property = ((PrismPropertyDefinition<?>)itemDefinition).instantiate();
				property.setRealValue(jaxbBean);
				subItem = (Item<V>) property;
			} else if (itemDefinition instanceof PrismContainerDefinition<?>) {
				if (jaxbBean instanceof Containerable) {
					PrismContainer<?> container = ((PrismContainerDefinition<?>)itemDefinition).instantiate();
					PrismContainerValue subValue = ((Containerable)jaxbBean).asPrismContainerValue();
					container.add(subValue);
					subItem = (Item<V>) container;
				} else {
					throw new IllegalArgumentException("Unsupported JAXB bean "+jaxbBean.getClass());
				}
			} else if (itemDefinition instanceof PrismReferenceDefinition) {
			
//				TODO;
				throw new UnsupportedOperationException();
				
			} else {
				throw new IllegalArgumentException("Unsupported definition type "+itemDefinition.getClass());
			}
		} else {
			throw new IllegalArgumentException("Unsupported element type "+element.getClass());
		}	
		return subItem;
	}

	public Object toAny(PrismValue value) throws SchemaException {
		Document document = DOMUtil.getDocument();
		if (value == null) {
			return value;
		}
		QName elementName = value.getParent().getElementName();
		Object xmlValue;
		if (value instanceof PrismPropertyValue) {
			PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
			Object realValue = pval.getValue();
        	xmlValue = realValue;
        	if (XmlTypeConverter.canConvert(realValue.getClass())) {
        		// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
        		xmlValue = XmlTypeConverter.toXsdElement(realValue, elementName, document, true);
        	}
		} else if (value instanceof PrismReferenceValue) {
			PrismReferenceValue rval = (PrismReferenceValue)value;
			xmlValue =  domParser.serializeValueToDom(rval, elementName, document);
		} else if (value instanceof PrismContainerValue<?>) {
			PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
			if (pval.getParent().getCompileTimeClass() == null) {
				// This has to be runtime schema without a compile-time representation.
				// We need to convert it to DOM
				xmlValue =  domParser.serializeValueToDom(pval, elementName, document);
			} else {
				xmlValue = pval.asContainerable();
			}
		} else {
			throw new IllegalArgumentException("Unknown type "+value);
		}
		if (!(xmlValue instanceof Element) && !(xmlValue instanceof JAXBElement)) {
    		xmlValue = new JAXBElement(elementName, xmlValue.getClass(), xmlValue);
    	}
        return xmlValue;
	}
		
}
