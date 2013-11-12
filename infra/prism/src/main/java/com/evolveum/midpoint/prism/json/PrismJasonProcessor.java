package com.evolveum.midpoint.prism.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class PrismJasonProcessor {
	
	private SchemaRegistry schemaRegistry;
	private PrismContext prismContext;
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	public void setSchemaRegistry(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}
	
	public SchemaRegistry getSchemaRegistry() {
		return schemaRegistry;
	}
	
	public <T extends Objectable> PrismObject<T> parseObject(InputStream inputStream, Class<T> valueType) throws IOException, SchemaException{
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JsonNode obj = null;
		try {
			obj = mapper.readValue(inputStream, JsonNode.class);
			
			
			JsonNode ns = obj.findValue("_ns");
			String defaultNamespace = ns.getTextValue();
			
			PrismSchema schema = schemaRegistry.findSchemaByNamespace(defaultNamespace);
			if (schema == null) {
				throw new SchemaException("No schema for namespace " + defaultNamespace);
			}
			
			JsonNode type = obj.get("_type");
			String typeValue = null;
			
			PrismObjectDefinition<T> objectDefinition = null;
			if (type != null){
				typeValue = type.getTextValue();
				QName objQname = new QName(defaultNamespace, typeValue);
				objectDefinition = schema.findObjectDefinitionByElementName(objQname);
			} else {
				objectDefinition = schema.findObjectDefinitionByCompileTimeClass(valueType);
			}
			
			
				if (objectDefinition == null) {
					throw new SchemaException("No definition found for type " + new QName(defaultNamespace, typeValue) + " (as defined by xsi:type)");
				}
			 PrismObject object = parseObject(obj, objectDefinition.getName(), defaultNamespace, objectDefinition);
			 return object;
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		
//		return null;
		
	}
	
	private <T extends Objectable> PrismObject<T> parseObject(JsonNode jsonObject, QName itemName, String defaultNamespace, 
			PrismObjectDefinition<T> objectDefinition) throws SchemaException {
		PrismObject<T> object = (PrismObject<T>) parsePrismContainer(jsonObject, itemName, defaultNamespace, objectDefinition);
		
		if (jsonObject.findValue("_oid") != null){
		String oid = jsonObject.findValue("_oid").getTextValue();
		object.setOid(oid);
		}
	
		if (jsonObject.findValue("_version") != null){
		
		String version = jsonObject.findValue("_version").getTextValue();
		object.setVersion(version);
		
		}
		return object;
	}
	
	  private <T extends Containerable> PrismContainer<T> parsePrismContainer(JsonNode jsonObject, QName itemName, String defaultNamespace, PrismContainerDefinition<T> containerDefinition) throws SchemaException {
			PrismContainer<T> container = containerDefinition.instantiate(itemName);
			
			PrismContainerValue<T> pval = new PrismContainerValue<T>(null, null, container, null);
					
			Collection<? extends Item> newContainerItems = parsePrismContainerItems(jsonObject,
							containerDefinition, defaultNamespace);
			addValuesToContainerValue(pval, newContainerItems);
			container.add(pval);
			
			return container;
		}
	  
	  private <T extends Containerable> void addValuesToContainerValue(PrismContainerValue<T> containerValue,
				Collection<? extends Item> newContainerItems) throws SchemaException {
			for (Item<?> newItem : newContainerItems) {
				if (newItem == null){
					System.out.println("new item name null");
					continue;
				}
				Item existingItem = containerValue.findItem(newItem.getName());
				if (existingItem == null) {
					containerValue.add(newItem);
				} else {
					for (PrismValue newPVal : newItem.getValues()) {
						existingItem.add(newPVal.clone());
					}
				}
			}
		}
	  
	  protected <T extends Containerable> Collection<? extends Item<?>> parsePrismContainerItems(JsonNode jsonObject,
				PrismContainerDefinition<T> containerDefinition, String defaultNamespace)
				throws SchemaException {

			// TODO: more robustness in handling schema violations (min/max
			// constraints, etc.)

			Collection<Item<?>> props = new HashSet<Item<?>>();

			// Iterate over all the XML elements there. Each element is
			// an item value.
			Iterator<Entry<String, JsonNode>> items = jsonObject.getFields();
			
//			String namespace = null;
			JsonNode defaultNs = jsonObject.findValue("_ns");
			if (defaultNs != null){
				defaultNamespace = defaultNs.getTextValue();
			}
			while (items.hasNext()){
				Entry<String, JsonNode> item = items.next();
				
				//not items, will be handled differently
				if (item.getKey() != null && item.getKey().startsWith("_")){
					continue;
				}
				
				JsonNode ns = item.getValue().findValue("_ns");
				JsonNode nsw = item.getValue().get("_ns");
				
				if (nsw != null){
					defaultNamespace = nsw.getTextValue();
				}
				
				QName itemQName = new QName(defaultNamespace, item.getKey());
				
				ItemDefinition def = locateItemDefinition(containerDefinition, itemQName);
				
				if (def == null) {
					if (containerDefinition.isRuntimeSchema()) {
						// No definition for item, but the schema is runtime. the
						// definition may come later.
						// Null is OK here.
					} else {
						throw new SchemaException("Item " + itemQName + " has no definition", itemQName);
					}
				}


			

				Item<?> parsedItem = parseItem(item.getValue(), itemQName, defaultNamespace, def);
				props.add(parsedItem);
			}
			
			return props;
		}

	  
	  private <T extends Containerable> ItemDefinition locateItemDefinition(
				PrismContainerDefinition<T> containerDefinition, QName itemQname)
				throws SchemaException {
			ItemDefinition def = containerDefinition.findItemDefinition(itemQname);
			if (def != null) {
				return def;
			}

			if (containerDefinition.isRuntimeSchema()) {
				// Try to locate global definition in any of the schemas
				def = getPrismContext().getSchemaRegistry().resolveGlobalItemDefinition(itemQname);
			}
			return def;
		}
	  
	  public <V extends PrismValue> Item<V> parseItem(JsonNode values, QName itemName, String defaultNamespace, ItemDefinition def)
				throws SchemaException {
			if (def == null) {
				// Assume property in a container with runtime definition
				return (Item<V>) parsePrismPropertyRaw(values, itemName);
			}
			if (def instanceof PrismContainerDefinition) {
				return (Item<V>) parsePrismContainer(values, itemName, defaultNamespace, (PrismContainerDefinition<?>) def);
			} else if (def instanceof PrismPropertyDefinition) {
				return (Item<V>) parsePrismProperty(values, itemName, (PrismPropertyDefinition) def);
			}
			if (def instanceof PrismReferenceDefinition) {
				return (Item<V>) parsePrismReference(values, itemName, (PrismReferenceDefinition) def);
			} else {
				throw new IllegalArgumentException("Attempt to parse unknown definition type " + def.getClass().getName());
			}
		}
	  
	  private <T> PrismProperty<T> parsePrismPropertyRaw(JsonNode values, QName itemName)
				throws SchemaException {
			return null;
		}
	  
	  public <T> PrismProperty<T> parsePrismProperty(JsonNode values, QName propName,
				PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
			if (values == null) {
				return null;
			}
			PrismProperty<T> prop = propertyDefinition.instantiate(propName);

			if (!propertyDefinition.isMultiValue() && values.isArray()) {
				throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
			}
			if (values.isArray()){
				Iterator<JsonNode> vals = values.getElements();
				while (vals.hasNext()){
					JsonNode val = vals.next();
					if (val != null){
						T realValue = parsePrismPropertyRealValue(val.getTextValue(), propertyDefinition);
						if (realValue != null) {
							prop.add(new PrismPropertyValue<T>(realValue));
						}
					}
				}
			} else {
				if (values != null){
					T realValue = parsePrismPropertyRealValue(values.getTextValue(), propertyDefinition);
					if (realValue != null) {
						prop.add(new PrismPropertyValue<T>(realValue));
					}
				}
			}

			return prop;
		}

		public <T> T parsePrismPropertyRealValue(String valueElement, PrismPropertyDefinition<T> propertyDefinition)
				throws SchemaException {
			QName typeName = propertyDefinition.getTypeName();
//			PrismJaxbProcessor jaxbProcessor = getPrismContext().getPrismJaxbProcessor();
			Object realValue = null;
			
			
			Class<?> expectedJavaType = XsdTypeMapper.toJavaType(propertyDefinition.getTypeName());
	    	if (expectedJavaType == null) {
	    		expectedJavaType = schemaRegistry.determineCompileTimeClass(propertyDefinition.getTypeName());
	    	}
	    	
	    
					realValue = JavaTypeConverter.convert(expectedJavaType, valueElement);
				 
			postProcessPropertyRealValue((T) realValue);
			return (T) realValue;
		}

		private <T> void postProcessPropertyRealValue(T realValue) {
			if (realValue == null) {
				return;
			}
			if (realValue instanceof PolyString) {
				PolyString polyString = (PolyString) realValue;
				if (!polyString.isComputed()) {
					polyString.recompute(getPrismContext().getDefaultPolyStringNormalizer());
				}
			}
		}
		
		public PrismReference parsePrismReference(JsonNode values, QName propName,
				PrismReferenceDefinition referenceDefinition) throws SchemaException {
			if (values == null) {
				return null;
			}
			PrismReference ref = referenceDefinition.instantiate();

			if (!referenceDefinition.isMultiValue() && values.isArray()) {
				throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
			}

				if (propName.equals(referenceDefinition.getName())) {
					// This is "real" reference (oid type and nothing more)
						ref.add(parseReferenceValue(values, referenceDefinition.getNamespace()));
				} else {
					// This is a composite object (complete object stored inside
					// reference)
					ref.add(parseReferenceAsCompositeObject(values, referenceDefinition));
			}
			return ref;
		}
		
		public PrismReferenceValue parseReferenceValue(JsonNode value, String defaultNs) {
			String oid = null;
			if (value.get("_oid") != null){
				oid = value.get("_oid").getTextValue();
			}
			
			
			QName type = null;
			if (value.get("_type") != null){
				type = new QName(defaultNs, value.get("_type").getTextValue());
			}
//			if (type != null) {
//				DOMUtil.validateNonEmptyQName(type, " in reference type in " + DOMUtil.getQName(element));
//			}
			PrismReferenceValue refVal = new PrismReferenceValue(oid);
			refVal.setTargetType(type);
			
			String description = null;
			if (value.get("description") != null){
				description = value.get("description").getTextValue();
			}

			refVal.setDescription(description);
//			QName relationAttribute = DOMUtil.getQNameAttribute(element, PrismConstants.ATTRIBUTE_RELATION_LOCAL_NAME);
//			if (relationAttribute != null) {
//				DOMUtil.validateNonEmptyQName(relationAttribute, " in reference type in " + DOMUtil.getQName(element));
//			}
//			refVal.setRelation(relationAttribute);

//			Element descriptionElement = DOMUtil.getChildElement(element, PrismConstants.ELEMENT_DESCRIPTION_LOCAL_NAME);
//			if (descriptionElement != null) {
//				refVal.setDescription(descriptionElement.getTextContent());
//			}
//			Element filterElement = DOMUtil.getChildElement(element, PrismConstants.ELEMENT_FILTER_LOCAL_NAME);
//			if (filterElement != null) {
//				refVal.setFilter(DOMUtil.getFirstChildElement(filterElement));
//			}

			return refVal;
		}

		private PrismReferenceValue parseReferenceAsCompositeObject(JsonNode valueElement,
				PrismReferenceDefinition referenceDefinition) throws SchemaException {
			QName targetTypeName = referenceDefinition.getTargetTypeName();
			PrismObjectDefinition<Objectable> schemaObjectDefinition = null;
			if (targetTypeName != null) {
				schemaObjectDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByType(targetTypeName);
			}

			PrismObject<Objectable> compositeObject = null;
			try {
//				if (valueElement instanceof Element) {
//					Element valueDomElement = (Element) valueElement;

				JsonNode type = valueElement.get("_type");
				String typeVal = null;
				if (type != null){
					typeVal = type.getTextValue();
				}
				
				if (typeVal != null){
				QName itemName = new QName(referenceDefinition.getNamespace(), typeVal);
	                PrismObjectDefinition<Objectable> schemaObjectDefinitionFromXsiType = schemaRegistry.findObjectDefinitionByElementName(new QName(referenceDefinition.getNamespace(), typeVal));
	                if (schemaObjectDefinitionFromXsiType != null) {
	                    schemaObjectDefinition = schemaObjectDefinitionFromXsiType;
	                }
				}

//					if (schemaObjectDefinition == null) {
//						compositeObject = parseObject(valueElement);
//					} else {
						compositeObject = parseObject(valueElement, targetTypeName, referenceDefinition.getNamespace(), schemaObjectDefinition);
//					}
//				} else if (valueElement instanceof JAXBElement) {
//					// This must be complete JAXB object
//					JAXBElement<Objectable> jaxbElement = (JAXBElement<Objectable>) valueElement;
//					Objectable objectable = jaxbElement.getValue();
//					compositeObject = objectable.asPrismObject();
//
//	                PrismObjectDefinition<Objectable> schemaObjectDefinitionFromJaxbType = findDefinitionFromJaxbType(jaxbElement);
//	                if (schemaObjectDefinitionFromJaxbType != null) {
//	                    schemaObjectDefinition = schemaObjectDefinitionFromJaxbType;
//	                }
//
//					if (schemaObjectDefinition == null) {
//						getPrismContext().adopt(objectable);
//					} else {
//						compositeObject.revive(getPrismContext());
//						compositeObject.applyDefinition(schemaObjectDefinition);
//					}
//				}
			} catch (SchemaException e) {
				throw new SchemaException(e.getMessage() + " while parsing composite object in reference element "
						+ referenceDefinition.getCompositeObjectElementName(), e);
			}

			PrismReferenceValue refVal = new PrismReferenceValue();
			refVal.setObject(compositeObject);
			return refVal;
		}
}
