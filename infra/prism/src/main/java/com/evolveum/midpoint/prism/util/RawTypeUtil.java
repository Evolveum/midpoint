/*
 * Copyright (c) 2013-2015 Evolveum
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

package com.evolveum.midpoint.prism.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class RawTypeUtil {


	public static <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getParsedItem(ID itemDefinition, List<RawType> values, QName elementQName, PrismContainerDefinition containerDef) throws SchemaException{

		Item<IV,ID> subItem = null;

		List<IV> parsedValues = new ArrayList<IV>();
		for (RawType rawValue : values){
			if (itemDefinition == null && containerDef != null){
				itemDefinition = (ID) ((PrismContextImpl) containerDef.getPrismContext()).getPrismUnmarshaller().locateItemDefinition(containerDef, elementQName, rawValue.getXnode());
			}
			IV parsed = rawValue.getParsedValue(itemDefinition, elementQName);
			if (parsed != null) {
				parsedValues.add(parsed);
			}
		}

        PrismContext prismContext = null;
        if (containerDef != null) {
            prismContext = containerDef.getPrismContext();
        }
        if (prismContext == null && itemDefinition != null) {
            prismContext = itemDefinition.getPrismContext();
        }

		if (itemDefinition == null){
			PrismProperty property = new PrismProperty(elementQName, prismContext);
            property.addAll(PrismValue.cloneCollection(parsedValues));
            return property;
		}


			if (itemDefinition instanceof PrismPropertyDefinition<?>) {
				// property
				PrismProperty<?> property = ((PrismPropertyDefinition<?>) itemDefinition).instantiate();
				for (IV val : parsedValues){
					property.add((PrismPropertyValue) val.clone());
				}
				subItem = (Item<IV,ID>) property;

			} else if (itemDefinition instanceof PrismContainerDefinition<?>) {
					PrismContainer<?> container = ((PrismContainerDefinition<?>) itemDefinition)
							.instantiate();
					for (IV val : parsedValues){
						container.add((PrismContainerValue) val.clone());
					}
					subItem = (Item<IV,ID>) container;
			} else if (itemDefinition instanceof PrismReferenceDefinition) {
				// TODO
					PrismReference reference = ((PrismReferenceDefinition) itemDefinition).instantiate();
					for (IV val : parsedValues) {
						PrismReferenceValue ref;
						if (val instanceof PrismReferenceValue) {
							ref = (PrismReferenceValue) val.clone();
						} else if (val instanceof PrismContainerValue) {
							// this is embedded (full) object
							Containerable c = ((PrismContainerValue) val).asContainerable();
							if (!(c instanceof Objectable)) {
								throw new IllegalStateException("Content of " + itemDefinition
									+ " is a Containerable but not Objectable: " + c);
							}
							Objectable o = (Objectable) c;
							ref = new PrismReferenceValue();
							ref.setObject(o.asPrismObject());
						} else {
							throw new IllegalStateException("Content of " + itemDefinition
									+ " is neither PrismReferenceValue nor PrismContainerValue: " + val);
						}
						reference.merge(ref);
					}
					subItem = (Item<IV,ID>) reference;

			} else {
				throw new IllegalArgumentException("Unsupported definition type " + itemDefinition.getClass());
			}

		return subItem;
	}

//	public static Object toAny(PrismValue value, Document document, PrismContext prismContext) throws SchemaException{
//		DomParser domProcessor = prismContext.getParserDom();
////		Document document = DOMUtil.getDocument();
//		if (value == null) {
//			return value;
//		}
//		QName elementName = value.getParent().getElementName();
//		Object xmlValue;
//		if (value instanceof PrismPropertyValue) {
//			PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
//			if (pval.isRaw() && (pval.getParent() == null || pval.getParent().getDefinition() == null)) {
//				Object rawElement = pval.getRawElement();
//				if (rawElement instanceof Element) {
//					return ((Element)rawElement).cloneNode(true);
//				} else if (rawElement instanceof MapXNode) {
//					return domProcessor.serializeXMapToElement((MapXNode)rawElement, elementName);
//				} else if (rawElement instanceof PrimitiveXNode<?>) {
//					PrimitiveXNode<?> xprim = (PrimitiveXNode<?>)rawElement;
//					String stringValue = xprim.getStringValue();
////					Element element = DOMUtil.createElement(document, elementName);
////					element.setTextContent(stringValue);
//					return stringValue;
//				} else {
//					throw new IllegalArgumentException("Cannot convert raw element "+rawElement+" to xsd:any");
//				}
//			}
//			Object realValue = pval.getValue();
//        	xmlValue = realValue;
//        	if (realValue instanceof PolyString){
//        		PolyStringType polyString = new PolyStringType((PolyString) realValue);
//        		xmlValue = polyString;
//        	}
//
//        	if (XmlTypeConverter.canConvert(realValue.getClass())) {
////        		// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
//        		Element e = (Element) XmlTypeConverter.toXsdElement(realValue, elementName, document, true);
//        		return e.getTextContent();
//        	}
//		} else if (value instanceof PrismReferenceValue) {
//			PrismReferenceValue rval = (PrismReferenceValue)value;
//			xmlValue = rval.asReferencable();
////			xmlValue = prismContext.serializeValueToDom(rval, elementName, document);
//
//		} else if (value instanceof PrismContainerValue<?>) {
//			PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
////			if (pval.getParent().getCompileTimeClass() == null) {
//				// This has to be runtime schema without a compile-time representation.
//				// We need to convert it to DOM
////				xmlValue =  prismContext.serializeValueToDom(pval, elementName, document);
////			} else {
//				xmlValue = pval.asContainerable();
////			}
//		} else {
//			throw new IllegalArgumentException("Unknown type "+value);
//		}
//		if (!(xmlValue instanceof Element) && !(xmlValue instanceof JAXBElement) && !(xmlValue instanceof String) && !xmlValue.getClass().isPrimitive() ) {
//			if (xmlValue.getClass().isEnum()){
//				String enumValue = XNodeProcessorUtil.findEnumFieldValue(xmlValue.getClass(), xmlValue);
//				if (enumValue == null){
//					enumValue = xmlValue.toString();
//				}
//				return enumValue;
//			}
//    		xmlValue = new JAXBElement(elementName, xmlValue.getClass(), xmlValue);
//    	}
//        return xmlValue;
//	}
}
