package com.evolveum.midpoint.prism.util;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ValueSerializationUtil {
	
	public static <T> String serializeValue(T value, ItemDefinition def, PrismContext prismContext, String language) throws SchemaException{
		System.out.println("value serialization");
		if (value == null){
			return null;
		}
		
		if (value instanceof List){
			List values = (List) value;
			if (values.isEmpty()){
				return null;
			}
		}
		
		PrismValue pVal = null;
		
		if (value instanceof Containerable){
			pVal = ((Containerable) value).asPrismContainerValue();
		} else if (value instanceof Referencable){
			pVal = ((Referencable) value).asReferenceValue();
		} else {
			pVal = new PrismPropertyValue<T>(value);
		}
		
//		Class clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(itemName);
//		PrismContainerDefinition def = prismContext.getSchemaRegistry().determineDefinitionFromClass(clazz);
//		
//		ItemDefinition def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
		
		QName itemName = null;
		if (def != null){
			itemName = def.getName();
		}
		
		XNodeSerializer serializer = prismContext.getXnodeProcessor().createSerializer();
		XNode node = serializer.serializeItemValue(pVal, def);
		String s = prismContext.getParserDom().serializeToString(node, itemName);
		System.out.println("serialized: " + s);
		return s;
//		throw new UnsupportedOperationException("need to be implemented");
	}
	
	public static String serializeItemValue(PrismValue value, String language){
		System.out.println("item value serialization");
		throw new UnsupportedOperationException("need to be implemented");
	}
	
	public static String serializeFilter(ObjectFilter query, PrismContext prismContext, String language){
		System.out.println("query serialization");
		throw new UnsupportedOperationException("need to be implemented");
	}

	public static <T> T deserializeValue(String value, Class clazz, QName itemName, ItemDefinition itemDef, PrismContext prismContext, String language) throws SchemaException{
		System.out.println("item value deserialization");
		
		XNode xnode = prismContext.getParserDom().parse(value);
		
		System.out.println("xnode: " + xnode.debugDump());
		
		MapXNode xmap = null;
		if (xnode instanceof RootXNode){
			xmap = (MapXNode) ((RootXNode) xnode).getSubnode();
		} else if (xnode instanceof MapXNode){
			xmap = (MapXNode) xnode;
		}
		
		Item item = prismContext.getXnodeProcessor().parseItem(xmap, itemName, itemDef);
		
		System.out.println("item: " + item.debugDump());
		
		if (item instanceof PrismProperty){
			PrismProperty prop = (PrismProperty) item;
			return (T) prop.getRealValue();
		} else if (item instanceof PrismContainer){
			PrismContainer cont = (PrismContainer) item;
			return (T) cont.getValue().asContainerable();
		} else if (item instanceof PrismReference){
			PrismReference ref = (PrismReference) item;
			return (T) ref.getValue();
		}
		if (item != null){
			return (T) item.getValue(0);
		}
//		if (prismContext.getBeanConverter().canConvert(clazz)){
//			prismContext.getBeanConverter().unmarshall(xmap, clazz);
//		} else{
//			prismContext.getXnodeProcessor().parseContainer(xnode, clazz);
//		}
		
		throw new UnsupportedOperationException("need to be implemented");
	}
	
	public static Collection<? extends PrismValue> deserializeItemValues(String value, String language){
		System.out.println("item value deserialization");
		throw new UnsupportedOperationException("need to be implemented");
	}
	
	public static ObjectFilter deserializeFilter(String query, String language){
		System.out.println("query deserialization");
		throw new UnsupportedOperationException("need to be implemented");
	}
	
	
}
