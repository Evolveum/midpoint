/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class RawTypeUtil {


    public static <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> getParsedItem(ID itemDefinition, List<RawType> values, QName elementQName, PrismContainerDefinition containerDef) throws SchemaException{

        Item<IV,ID> subItem;

        List<IV> parsedValues = new ArrayList<>();
        for (RawType rawValue : values) {
            if (itemDefinition == null && containerDef != null){
                //noinspection unchecked
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
            PrismProperty property = new PrismPropertyImpl(elementQName, prismContext);
            property.addAll(PrismValueCollectionsUtil.cloneCollection(parsedValues));
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
                            ref = new PrismReferenceValueImpl();
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

//    public static Object toAny(PrismValue value, Document document, PrismContext prismContext) throws SchemaException{
//        DomParser domProcessor = prismContext.getParserDom();
////        Document document = DOMUtil.getDocument();
//        if (value == null) {
//            return value;
//        }
//        QName elementName = value.getParent().getElementName();
//        Object xmlValue;
//        if (value instanceof PrismPropertyValue) {
//            PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
//            if (pval.isRaw() && (pval.getParent() == null || pval.getParent().getDefinition() == null)) {
//                Object rawElement = pval.getRawElement();
//                if (rawElement instanceof Element) {
//                    return ((Element)rawElement).cloneNode(true);
//                } else if (rawElement instanceof MapXNode) {
//                    return domProcessor.serializeXMapToElement((MapXNode)rawElement, elementName);
//                } else if (rawElement instanceof PrimitiveXNode<?>) {
//                    PrimitiveXNode<?> xprim = (PrimitiveXNode<?>)rawElement;
//                    String stringValue = xprim.getStringValue();
////                    Element element = DOMUtil.createElement(document, elementName);
////                    element.setTextContent(stringValue);
//                    return stringValue;
//                } else {
//                    throw new IllegalArgumentException("Cannot convert raw element "+rawElement+" to xsd:any");
//                }
//            }
//            Object realValue = pval.getValue();
//            xmlValue = realValue;
//            if (realValue instanceof PolyString){
//                PolyStringType polyString = new PolyStringType((PolyString) realValue);
//                xmlValue = polyString;
//            }
//
//            if (XmlTypeConverter.canConvert(realValue.getClass())) {
////                // Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
//                Element e = (Element) XmlTypeConverter.toXsdElement(realValue, elementName, document, true);
//                return e.getTextContent();
//            }
//        } else if (value instanceof PrismReferenceValue) {
//            PrismReferenceValue rval = (PrismReferenceValue)value;
//            xmlValue = rval.asReferencable();
////            xmlValue = prismContext.serializeValueToDom(rval, elementName, document);
//
//        } else if (value instanceof PrismContainerValue<?>) {
//            PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
////            if (pval.getParent().getCompileTimeClass() == null) {
//                // This has to be runtime schema without a compile-time representation.
//                // We need to convert it to DOM
////                xmlValue =  prismContext.serializeValueToDom(pval, elementName, document);
////            } else {
//                xmlValue = pval.asContainerable();
////            }
//        } else {
//            throw new IllegalArgumentException("Unknown type "+value);
//        }
//        if (!(xmlValue instanceof Element) && !(xmlValue instanceof JAXBElement) && !(xmlValue instanceof String) && !xmlValue.getClass().isPrimitive() ) {
//            if (xmlValue.getClass().isEnum()){
//                String enumValue = XNodeProcessorUtil.findEnumFieldValue(xmlValue.getClass(), xmlValue);
//                if (enumValue == null){
//                    enumValue = xmlValue.toString();
//                }
//                return enumValue;
//            }
//            xmlValue = new JAXBElement(elementName, xmlValue.getClass(), xmlValue);
//        }
//        return xmlValue;
//    }
}
