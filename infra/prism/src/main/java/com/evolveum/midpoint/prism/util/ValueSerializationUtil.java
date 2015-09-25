package com.evolveum.midpoint.prism.util;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ValueSerializationUtil {
	
	public static <T> String serializeValue(T value, ItemDefinition def, QName itemName, PrismContext prismContext, String language) throws SchemaException{
		return serializeValue(value, def, itemName, null, prismContext, language);
	}
	
	public static <T> String serializeValue(T value, ItemDefinition def, QName itemName, QName parentName, PrismContext prismContext, String language) throws SchemaException{
//		System.out.println("value serialization");
		if (value == null){
			return null;
		}
		
		XNodeSerializer serializer = prismContext.getXnodeProcessor().createSerializer();
		
		if (value instanceof List){
			List<T> values = (List<T>) value;
			if (values.isEmpty()){
				return null;
			}
			
			if (def instanceof PrismPropertyDefinition){
				PrismProperty prop = (PrismProperty) def.instantiate(itemName);
				for (T val : values){
					PrismPropertyValue<T> pValue = new PrismPropertyValue<T>(val);
					prop.add(pValue);
				}
		
				XNode node = serializer.serializeItem(prop);
				if (node instanceof ListXNode){
					ListXNode xList = (ListXNode) node;
					if (xList.size() == 1){
						XNode sub = xList.iterator().next();
						if (!(sub instanceof MapXNode)){
							throw new IllegalArgumentException("must be a map");
						}
						
						String s = prismContext.getParserDom().serializeToString(sub, parentName);
//						System.out.println("serialized: " + s);
						return s;
						
					}else{
						MapXNode xmap = new MapXNode();
						xmap.put(itemName, xList);
						String s = prismContext.getParserDom().serializeToString(xmap, parentName);
//						System.out.println("serialized: " + s);
						return s;
//						throw new IllegalArgumentException("Check your data.");
					}
					
//					MapXNode xmap = xList.new MapXNode();
//					xmap.put(def.getName(), xList);
					
				}
				String s = prismContext.getParserDom().serializeToString(node, def.getName());
//				System.out.println("serialized: " + s);
				return s;
			} else if (def instanceof PrismContainerDefinition){
				PrismContainer pc = (PrismContainer) def.instantiate();
				for (T val : values){
//					PrismContainerValue pcVal = new PrismContainerValue<Containerable>((Containerable) val);
					PrismContainerValue pcVal = ((Containerable) val).asPrismContainerValue();
					pc.add(pcVal.clone());
				}
				XNode node = serializer.serializeItem(pc);
				if (node instanceof ListXNode){
					ListXNode xList = (ListXNode) node;
					MapXNode xmap = new MapXNode();
					xmap.put(def.getName(), xList);
					String s = prismContext.getParserDom().serializeToString(xmap, parentName);
//					System.out.println("serialized: " + s);
					return s;
				}
				String s = prismContext.getParserDom().serializeToString(node, def.getName());
//				System.out.println("serialized: " + s);
				return s;
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
		
//		QName itemName = null;
		if (def != null){
			itemName = def.getName();
		}
		
		
		XNode node = serializer.serializeItemValue(pVal, def);
		String s = prismContext.getParserDom().serializeToString(node, itemName);
//		System.out.println("serialized: " + s);
		return s;
//		throw new UnsupportedOperationException("need to be implemented");
	}
	
	public static <T> String serializeValue(T value, QName itemName, PrismContext prismContext, String language) throws SchemaException{
//		System.out.println("value serialization");
		if (value == null){
			return null;
		}
		
		XNodeSerializer serializer = prismContext.getXnodeProcessor().createSerializer();
		
		if (value instanceof List){
			List<T> values = (List<T>) value;
			if (values.isEmpty()){
				return null;
			}
			throw new UnsupportedOperationException("take your chance.");			
		}
		
		PrismValue pVal = null;
		
		if (value instanceof Containerable){
			pVal = ((Containerable) value).asPrismContainerValue();
		} else if (value instanceof Referencable){
			pVal = ((Referencable) value).asReferenceValue();
		} else {
			pVal = new PrismPropertyValue<T>(value);
		}		
		
		XNode node = serializer.serializeItemValue(pVal, null);
		String s = prismContext.getParserDom().serializeToString(node, itemName);
//		System.out.println("serialized: " + s);
		return s;
//		throw new UnsupportedOperationException("need to be implemented");
	}
	
//	public static String serializeItemValue(QName itemName, ItemDefinition def, PrismValue value, PrismContext prismContext, String language) throws SchemaException{
//		XNodeSerializer serializer = prismContext.getXnodeProcessor().createSerializer();
//		XNode node = serializer.serializeItemValue(value, def);
//		String s = prismContext.getParserDom().serializeToString(node, itemName);
//		//System.out.println("serialized ITEM VALUE: " + s);
//		return s;
//	}
	
//	public static String serializeFilter(SearchFilterType query, PrismContext prismContext, String language){
//		//System.out.println("query serialization");
//		throw new UnsupportedOperationException("need to be implemented");
//	}

//	public static <T> T deserializeValue(String value, Class clazz, QName itemName, ItemDefinition itemDef, PrismContext prismContext, String language) throws SchemaException{
//		//System.out.println("item value deserialization");
//
//		XNode xnode = prismContext.getParserDom().parse(value);
//
////		System.out.println("xnode: " + xnode.debugDump());
//
//		XNode xmap = null;
//		if (xnode instanceof RootXNode){
//			xmap = ((RootXNode) xnode).getSubnode();
//		}
//
////		System.out.println("xmap: " + xmap);
////		else if (xnode instanceof MapXNode){
////			xmap = (MapXNode) xnode;
////		} else if (xnode instanceof PrimitiveXNode){
////			xmap = new MapXNode();
////			xmap.put(itemName, xnode);
////		}
//
//		Item item = prismContext.getXnodeProcessor().parseItem(xmap, itemName, itemDef);
//
////		System.out.println("item: " + item.debugDump());
//
//		if (item instanceof PrismProperty){
//			PrismProperty prop = (PrismProperty) item;
//
//			if (prop.isSingleValue()){
//				return (T) prop.getRealValue();
//			}
//			return (T) prop.getRealValues();
//		} else if (item instanceof PrismContainer){
//			PrismContainer cont = (PrismContainer) item;
//			return (T) cont.getValue().asContainerable();
//		} else if (item instanceof PrismReference){
//			PrismReference ref = (PrismReference) item;
//			return (T) ref.getValue();
//		}
//		if (item != null){
//			return (T) item.getValue(0);
//		}
////		if (prismContext.getBeanConverter().canConvert(clazz)){
////			prismContext.getBeanConverter().unmarshall(xmap, clazz);
////		} else{
////			prismContext.getXnodeProcessor().parseContainer(xnode, clazz);
////		}
//
//		throw new UnsupportedOperationException("need to be implemented");
//	}
	
//	public static Collection<? extends PrismValue> deserializeItemValues(String value, Item item, String language) throws SchemaException{
//		//System.out.println("item value deserialization");
//		PrismContext prismContext = item.getPrismContext();
//		XNode xnode = prismContext.getParserDom().parse(value);
//		if (xnode instanceof RootXNode){
//			xnode = ((RootXNode) xnode).getSubnode();
//		}
//		//System.out.println("value: " + value);
//		Item parsedItem = prismContext.getXnodeProcessor().parseItem(xnode, item.getElementName(), item.getDefinition());
//		return parsedItem.getValues();
////		throw new UnsupportedOperationException("need to be implemented");
//	}
	
//	public static SearchFilterType deserializeFilter(String query, String language){
//		//System.out.println("query deserialization");
//		throw new UnsupportedOperationException("need to be implemented");
//	}

//	public static <T> String serializeValue(T value, PrismPropertyDefinition def,
//			QName itemName, PrismContext prismContext, String langXml) throws SchemaException{
////		System.out.println("value serialization");
//		if (value == null){
//			return null;
//		}
//
//		XNodeSerializer serializer = prismContext.getXnodeProcessor().createSerializer();
//
//		if (value instanceof List){
//			List<T> values = (List<T>) value;
//			if (values.isEmpty()){
//				return null;
//			}
//
//			if (def instanceof PrismPropertyDefinition){
//				PrismProperty prop = (PrismProperty) def.instantiate();
//				for (T val : values){
//					PrismPropertyValue<T> pValue = new PrismPropertyValue<T>(val);
//					prop.add(pValue);
//				}
//				XNode node = serializer.serializeItem(prop);
//				if (node instanceof ListXNode){
//					ListXNode xList = (ListXNode) node;
//					MapXNode xmap = new MapXNode();
//					xmap.put(def.getName(), xList);
//					String s = prismContext.getParserDom().serializeToString(xmap, def.getName());
////					System.out.println("serialized: " + s);
//					return s;
//				}
//				String s = prismContext.getParserDom().serializeToString(node, def.getName());
////				System.out.println("serialized: " + s);
//				return s;
//			}
//
//		}
//
//		PrismValue pVal = null;
//
//		if (value instanceof Containerable){
//			pVal = ((Containerable) value).asPrismContainerValue();
//		} else if (value instanceof Referencable){
//			pVal = ((Referencable) value).asReferenceValue();
//		} else {
//			PrismProperty pp = def.instantiate();
//			pVal = new PrismPropertyValue<T>(value);
//			pp.add(pVal);
//			XNode xnode = serializer.serializeItemValue(pVal, def);
//			if (xnode == null){
//				throw new IllegalArgumentException("null node after serialization");
//			}
//			MapXNode xmap = null;
//			if (xnode instanceof RootXNode){
//				XNode sub = ((RootXNode) xnode).getSubnode();
//				if (!(sub instanceof MapXNode)){
//					throw new IllegalArgumentException("not uspported yet");
//				}
//				xmap = (MapXNode) sub;
//			} else if (xnode instanceof MapXNode){
//				xmap = (MapXNode) xnode;
//			} else if (xnode instanceof PrimitiveXNode){
//				String s = ((PrimitiveXNode) xnode).getStringValue();
//				return s;
////
//			} else{
//				throw new IllegalStateException("hmmm");
//			}
//
//			XNode node = xmap.get(itemName);
//			String s = prismContext.getParserDom().serializeToString(node, itemName);
//		}
//
//
//
//		XNode node = serializer.serializeItemValue(pVal, def);
//		String s = prismContext.getParserDom().serializeToString(node, itemName);
////		System.out.println("serialized: " + s);
//		return s;
////		throw new UnsupportedOperationException("need to be implemented");
//
//	}
	
	
}
