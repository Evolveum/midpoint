package com.evolveum.midpoint.prism.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
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
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.util.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class RawTypeUtil {

	
	public static <V extends PrismValue> Item<V> getParsedItem(ItemDefinition itemDefinition, List<RawType> values, QName elementQName, PrismContainerDefinition containerDef) throws SchemaException{
		
		Item<V> subItem = null;
		
		List<V> parsedValues = new ArrayList<V>();
		for (RawType rawValue : values){
			if (itemDefinition == null && containerDef != null){
				itemDefinition = containerDef.getPrismContext().getXnodeProcessor().locateItemDefinition(containerDef, elementQName, rawValue.getXnode());
			}
			V parsed = rawValue.getParsedValue(itemDefinition, elementQName);
			if (parsed != null){
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
				for (V val : parsedValues){
					property.add((PrismPropertyValue) val.clone());
				}
				subItem = (Item<V>) property;
				
			} else if (itemDefinition instanceof PrismContainerDefinition<?>) {
					PrismContainer<?> container = ((PrismContainerDefinition<?>) itemDefinition)
							.instantiate();
					for (V val : parsedValues){
						container.add((PrismContainerValue) val.clone());
					}
					subItem = (Item<V>) container;
			} else if (itemDefinition instanceof PrismReferenceDefinition) {
				// TODO
					PrismReference reference = ((PrismReferenceDefinition) itemDefinition).instantiate();
					for (V val : parsedValues){
						reference.merge((PrismReferenceValue) val.clone());
					}
					subItem = (Item<V>) reference;

			} else {
				throw new IllegalArgumentException("Unsupported definition type " + itemDefinition.getClass());
			}
			
		return subItem;
	}
	
//	public static Object toAny(PrismValue value, Document document, PrismContext prismContext) throws SchemaException{
//		DomParser domParser = prismContext.getParserDom();
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
//					return domParser.serializeXMapToElement((MapXNode)rawElement, elementName);
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
