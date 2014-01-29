package com.evolveum.midpoint.prism.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
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
import com.evolveum.midpoint.prism.parser.Parser;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.deser.DomElementJsonDeserializer;

public class PrismJsonSerializer implements Parser{
	
	private SchemaRegistry schemaRegistry;
	private PrismContext prismContext;
//	private PrismSchema prismSchema;
	
	
	public PrismSchema getPrismSchema(Class clazz) {

		return schemaRegistry.findSchemaByCompileTimeClass(clazz);
//		
//		return prismSchema;
	}
	
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
	
	private PrismSchema prismSchema = null;
	
	public <T extends Objectable> PrismObject<T> parseObject(File file, Class<T> valueType) throws IOException, SchemaException{
		ObjectMapper mapper = new ObjectMapper();
		
		ObjectReader reader = mapper.reader(valueType);
		
		JsonFactory factory = new JsonFactory();
		factory.createJsonParser(file);
		
		T object = reader.readValue(file);
		PrismObjectDefinition def = schemaRegistry.findObjectDefinitionByCompileTimeClass(valueType);
		
		PrismObject<T> prismObj = object.asPrismObject();
		
		System.out.println("object: \n" + prismObj.dump());
		
		return prismObj;
	}
	String globalNamespace = null;
	public String serializeToJson(XNode node, QName rootElement) throws SchemaException{
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
		
		JaxbElementSerializer jaxbSerializer = new JaxbElementSerializer();
		module.addSerializer(JAXBElement.class, jaxbSerializer);
		mapper.registerModule(module);
		
		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		
		ObjectWriter writer = mapper.writer();
		JsonFactory factory = new JsonFactory();
		StringWriter out = new StringWriter();
		try {
		JsonGenerator generator = factory.createGenerator(out);
		
		
		PrettyPrinter pp = new DefaultPrettyPrinter();

		generator.setPrettyPrinter(pp);
		generator.setCodec(mapper);
		globalNamespace = rootElement.getNamespaceURI();
//		objectNs = globalNamespace;
//		generator.writeObjectFieldStart(rootElement.getLocalPart());
//		generator.writeStringField("@ns", rootElement.getNamespaceURI());
		generator.writeStartObject();
		serializeToJson(node, rootElement,  generator);
		generator.writeEndObject();
//		generator.writeEndObject();
		
		generator.flush();
		generator.close();
		} catch (IOException ex){
			throw new SchemaException("Schema error during serializing to JSON.", ex);
		}
		return out.toString();
	}
	
	String objectNs = null;
	private <T> void  serializeToJson(XNode node, QName nodeName, JsonGenerator generator) throws JsonGenerationException, IOException{
		
		
//		if (globalNamespace == null && nodeName != null){
//			globalNamespace = nodeName.getNamespaceURI();
//			objectNs = globalNamespace;
//		}
		
//		if (node instanceof RootXNode){
//			RootXNode root = (RootXNode) node;
//			generator.writeObjectFieldStart(nodeName.getLocalPart());
//			generator.writeStringField("G@ns", globalNamespace);
//			serializeToJson(root.getSubnode(), root.getRootElementName(),  generator);
//			generator.writeEndObject();
//			
//		}
		
		if (node instanceof MapXNode){
			
			
			MapXNode mapNode = (MapXNode) node;
			if (nodeName == null){
//				generator.writeObjectFieldStart("thereShouldBeName");
				generator.writeStartObject();
			} else{
				
			generator.writeObjectFieldStart(nodeName.getLocalPart());
			
			}
			
			if (StringUtils.isBlank(objectNs)){
				objectNs = globalNamespace;
				generator.writeStringField("O@ns", objectNs);
			}
			
			Iterator<Entry<QName, XNode>> subnodes = mapNode.entrySet().iterator();
			while (subnodes.hasNext()){
				Entry<QName, XNode> subNode = subnodes.next();
				
				serializeToJson(subNode.getValue(), subNode. getKey(), generator);
				
			}
			if (nodeName != null && StringUtils.isNotEmpty(nodeName.getNamespaceURI()) && !nodeName.getNamespaceURI().equals(objectNs)){
//				objectNs = nodeName.getNamespaceURI();
				if (!objectNs.equals(globalNamespace)){
				generator.writeStringField("O@ns", objectNs);
				}
			}
			generator.writeEndObject();
		}
		
		if (node instanceof ListXNode){
			ListXNode listNode = (ListXNode) node;
			ListIterator<XNode> sublist = listNode.listIterator();
			generator.writeArrayFieldStart(nodeName.getLocalPart());
			while (sublist.hasNext()){
				serializeToJson(sublist.next(), null, generator);
			}
			generator.writeEndArray();
		}
		
		if (node instanceof PrimitiveXNode){
			PrimitiveXNode<T> primitiveNode = (PrimitiveXNode<T>) node;
			
			if (nodeName == null){
			generator.writeObject(primitiveNode.getValue());
			} else {
				if (StringUtils.isNotBlank(nodeName.getNamespaceURI()) && !nodeName.getNamespaceURI().equals(objectNs)){
					objectNs = nodeName.getNamespaceURI();
//					generator.writeObjectFieldStart(nodeName.getLocalPart());
//					generator.writeStringField("P@ns", nodeName.getNamespaceURI());
////					generator.writeFieldName("name");
//					generator.writeObjectField("value", primitiveNode.getValue());
//					generator.writeEndObject();
				}
//				if (primitiveNode.isAttribute()){
//					generator.writeObjectField("_"+nodeName.getLocalPart(), primitiveNode.getValue());
//				else {
					generator.writeObjectField(nodeName.getLocalPart(), primitiveNode.getValue());
//				}
			}
		}
			}
	
	

	
	
	String globalNs = null;
	public XNode parseObject(InputStream inputStream) throws IOException, SchemaException{
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode obj = null;
		try {
			JsonParser parser = null;
			JsonFactory facotry = new JsonFactory();
			parser =  facotry.createJsonParser(inputStream);
			parser.setCodec(mapper);
			
			obj = parser.readValueAs(JsonNode.class);
			
			RootXNode xmap = new RootXNode();
			
			Iterator<Entry<String, JsonNode>> fields = obj.fields();
			
			while (fields.hasNext()){
				Entry<String, JsonNode> field = fields.next();
				String fieldName = field.getKey();
				
				if (fieldName != null){
					((RootXNode) xmap).setRootElementName(new QName(globalNs, fieldName));
				}
				
				
				parseJsonObject(xmap, globalNs, field.getKey(), field.getValue(), parser);
				
			}
			 return xmap;
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
	
	private void parseJsonObject(XNode xmap, String ns, String fieldName, JsonNode obj, JsonParser parser) throws JsonProcessingException, IOException {
		
//		JsonSchemaFactory f= new JsonSchemaFactory();
//		SchemaFactoryWrapper w = new SchemaFactoryWrapper();
//		w.
//		
//		
//		expectObjectFormat(TypeFactory.defaultInstance().uncheckedSimpleType(QName.class));
//		JsonSchema s = w.finalSchema();
//		s.
		
		
		if (obj.isObject()){
			MapXNode subMap = new MapXNode();
			boolean iterate = true;
			if (xmap instanceof MapXNode){
				if (fieldName == null){
//					((MapXNode) xmap).put(new QName("ReplaceWithName"), subMap);
					subMap = (MapXNode) xmap;
				} else{
//					if (fieldName.startsWith("_")){
//						fieldName = fieldName.replaceFirst("_", "");
//						if (isQName(obj)){
//							PrimitiveXNode primitive = new PrimitiveXNode();
//							primitive.setValue(readQName(obj));
//							primitive.setAttribute(true);
//							((MapXNode) xmap).put(new QName(fieldName), primitive);
//							iterate = false;
//							
//						}
//					}else {
						((MapXNode) xmap).put(new QName(ns, fieldName), subMap);
//					}
				}
			} else if (xmap instanceof ListXNode){
				((ListXNode) xmap).add(subMap);
			} if (xmap instanceof RootXNode){
//				if (fieldName != null){
				JsonNode globalNsNode = obj.get("@ns");
				
				if (globalNsNode != null){
					globalNs = globalNsNode.asText();
				}
					((RootXNode) xmap).setRootElementName(new QName(globalNs, fieldName));
//					((RootXNode) xmap).setSubnode(subMap);
//				}
				((RootXNode) xmap).setSubnode(subMap);
			}
			
			Iterator<Entry<String, JsonNode>> fields = obj.fields();
			JsonNode objNsNode = obj.get("@ns");
			String objNs = null;
			if (objNsNode != null){
				objNs = objNsNode.asText();
			}
			
			String nsToUse = globalNs;
			if (objNs != null && !objNs.equals(ns)){
				nsToUse = objNs;
			} else {
				nsToUse = ns;
			}
			
//			if (glo)
			if (iterate){
				
			while (fields.hasNext()){
				Entry<String, JsonNode> field = fields.next();
				
				
				
				parseJsonObject(subMap, nsToUse, field.getKey(), field.getValue(), parser);
//				subMap.put(new QName(field.getKey()), )
				
			}}
			
					
		} else {
			if (obj.isArray()){
				Iterator<JsonNode> elements = obj.elements();
				ListXNode listNode = new ListXNode();
				if (xmap instanceof MapXNode){
					((MapXNode) xmap).put(new QName(ns, fieldName), listNode);
				} else if (xmap instanceof ListXNode){
					((ListXNode) xmap).add(listNode);
				}
				while (elements.hasNext()){
					JsonNode element = elements.next();
					parseJsonObject(listNode, ns, fieldName, element, parser);
				}
			} else {
//				String text = obj.asText();
				if (fieldName.equals("@ns")){
					return;
				}
				PrimitiveXNode primitive = new PrimitiveXNode();
				
				Object val = getRealValue(obj);
				
				primitive.setValue(val);
				
				if (xmap instanceof MapXNode){
					if (fieldName.startsWith("_")){
						fieldName = fieldName.replaceFirst("_", "");
						primitive.setAttribute(true);
						((MapXNode) xmap).put(new QName(fieldName), primitive);
					} else {
						((MapXNode) xmap).put(new QName(ns, fieldName), primitive);
					}
				} else if (xmap instanceof ListXNode){
					((ListXNode) xmap).add(primitive);
				}
				
			}
		} 
		
		
		
	}

	private Object getRealValue(JsonNode obj) {
		switch (obj.getNodeType()) {
			case BOOLEAN:
				return obj.asBoolean();
			case NUMBER:
					return obj.asLong();
			default:
				return obj.asText();
		}
//		return null;
	}

	private boolean isQName(JsonNode obj) {
		if (obj.isObject()){
			if (obj.size() != 2){
				return false;
			}
			Iterator<String> fieldNames = obj.fieldNames();
			boolean containsNs = false;
			boolean containsLocal = false;
			while (fieldNames.hasNext()){
				String fName = fieldNames.next();
				if (fName.equals("namespace")){
					containsNs = true;
				} 
				if (fName.equals("localPart")){
					containsLocal = true;
				}
			}
			return containsLocal && containsNs;
		}
		return false;
	}

	private QName readQName(JsonNode obj) {
		if (obj.isObject()){
			if (obj.size() != 2){
				return null;
			}
			Iterator<Entry<String, JsonNode>> fieldNames = obj.fields();
			String ns = null;
			String local = null;
			while (fieldNames.hasNext()){
				Entry<String, JsonNode> fName = fieldNames.next();
				if (fName.getKey().equals("namespace")){
					ns = fName.getValue().asText();
				} 
				if (fName.getKey().equals("localPart")){
					local = fName.getValue().asText();
				}
			}
			return new QName(ns, local);
		}
		return null;
	}

	private <T extends Objectable> PrismObject<T> parseObject(JsonNode jsonObject, QName itemName, String defaultNamespace, 
			PrismObjectDefinition<T> objectDefinition) throws SchemaException, JsonParseException, JsonMappingException, IOException {

		PrismObject<T> object = (PrismObject<T>) parsePrismContainer(jsonObject, itemName, defaultNamespace, objectDefinition);
		
		if (jsonObject.findValue("_oid") != null){
		String oid = jsonObject.findValue("_oid").asText();
		object.setOid(oid);
		}
	
		if (jsonObject.findValue("_version") != null){
		
		String version = jsonObject.findValue("_version").asText();
		object.setVersion(version);
		
		}
		return object;
	}
	
	  private <T extends Containerable> PrismContainer<T> parsePrismContainer(JsonNode jsonObject, QName itemName, String defaultNamespace, PrismContainerDefinition<T> containerDefinition) throws SchemaException, JsonParseException, JsonMappingException, IOException {
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
				Item existingItem = containerValue.findItem(newItem.getElementName());
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
				throws SchemaException, JsonParseException, JsonMappingException, IOException {

			// TODO: more robustness in handling schema violations (min/max
			// constraints, etc.)

			Collection<Item<?>> props = new HashSet<Item<?>>();

			// Iterate over all the XML elements there. Each element is
			// an item value.
			Iterator<Entry<String, JsonNode>> items = jsonObject.fields();
			
//			String namespace = null;
//			JsonNode defaultNs = jsonObject.findValue("_ns");
//			if (defaultNs != null){
//				defaultNamespace = defaultNs.getTextValue();
//			}
			while (items.hasNext()){
				Entry<String, JsonNode> item = items.next();
				
				//not items, will be handled differently
//				if (item.getKey() != null && item.getKey().startsWith("_")){
//					continue;
//				}
//				
//				JsonNode ns = item.getValue().findValue("_ns");
//				JsonNode nsw = item.getValue().get("_ns");
//				
//				if (nsw != null){
//					defaultNamespace = nsw.getTextValue();
//				}
//				
//				PrismSchema schema  = getPrismSchema(containerDefinition.get);
//				ItemDefinition def = prismSchema.findItemDefinition(item.getKey(), containerDefinition.getTypeClass());
				
				
				if (item.getKey().equals("id") || item.getKey().equals("any")){
					continue;
				}
				QName itemQName = new QName(defaultNamespace, item.getKey());
				
				ItemDefinition def = locateItemDefinition(containerDefinition, itemQName);
				
				if (def == null) {
					if (containerDefinition.isRuntimeSchema()) {
						// No definition for item, but the schema is runtime. the
						// definition may come later.
						// Null is OK here.
					} else {
//						continue;
//						throw new SchemaException("Item " + itemQName + " has no definition", itemQName);
					}
//					continue;
				}


//				if (item.getValue().isObject()){
//					parsePrismContainerItems(item.getValue(), containerDefinition, defaultNamespace);
//				} else{
					Item<?> parsedItem = parseItem(item.getValue(), itemQName, defaultNamespace, def);
					props.add(parsedItem);
//				}
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

//			if (containerDefinition.isRuntimeSchema()) {
//				// Try to locate global definition in any of the schemas
//				def = getPrismContext().getSchemaRegistry().resolveGlobalItemDefinition(itemQname);
//			}
			return def;
		}
	  
	  private <T extends Containerable> ItemDefinition locateItemDefinition(
				PrismContainerDefinition<T> containerDefinition, ItemPath itemQname)
				throws SchemaException {
			ItemDefinition def = containerDefinition.findItemDefinition(itemQname);
			if (def != null) {
				return def;
			}

//			if (containerDefinition.isRuntimeSchema()) {
//				// Try to locate global definition in any of the schemas
//				def = getPrismContext().getSchemaRegistry().resolveGlobalItemDefinition(itemQname);
//			}
			return def;
		}
	  
	  public <V extends PrismValue> Item<V> parseItem(JsonNode values, QName itemName, String defaultNamespace, ItemDefinition def)
				throws SchemaException, JsonParseException, JsonMappingException, IOException {
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
				PrismPropertyDefinition<T> propertyDefinition) throws SchemaException, JsonParseException, JsonMappingException, IOException {
			if (values.isNull()) {
				return null;
			}
			PrismProperty<T> prop = propertyDefinition.instantiate(propName);

			if (!propertyDefinition.isMultiValue() && values.isArray()) {
				throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
			}
			if (values.isArray()){
				Iterator<JsonNode> vals = values.elements();
				while (vals.hasNext()){
					JsonNode val = vals.next();
					if (val != null){
						T realValue = parsePrismPropertyRealValue(val, propertyDefinition);
						if (realValue != null) {
							prop.add(new PrismPropertyValue<T>(realValue));
						}
					}
				}
//			} else if (values.isObject()){
//				Iterator<Entry<String, JsonNode>> vals = values.getFields();
//				while (vals.hasNext()){
//					Entry<String,JsonNode> val = vals.next();
//				
//					if (val != null){
//						if (val.getValue().isObject()){
//						QName itemQName = new QName(propertyDefinition.getNamespace(), val.getKey());
//						ItemDefinition def = locateItemDefinition(containerDefinition, new ItemPath(prop.getElementName(), itemQName));
//						T realValue = parsePrismPropertyRealValue(val.getValue(), (PrismPropertyDefinition<T>) def);
//						if (realValue != null) {
//							prop.add(new PrismPropertyValue<T>(realValue));
//						}
//						}else{
//							T realValue = parsePrismPropertyRealValue(values, propertyDefinition);
//							if (realValue != null) {
//								prop.add(new PrismPropertyValue<T>(realValue));
//							}
//						}
//					}
//				}
			} else {
				if (values != null){
					T realValue = parsePrismPropertyRealValue(values, propertyDefinition);
					if (realValue != null) {
						prop.add(new PrismPropertyValue<T>(realValue));
					}
				}
			}

			return prop;
		}

		public <T> T parsePrismPropertyRealValue(JsonNode valueElement, PrismPropertyDefinition<T> propertyDefinition)
				throws SchemaException, JsonParseException, JsonMappingException, IOException {
			QName typeName = propertyDefinition.getTypeName();
//			PrismJaxbProcessor jaxbProcessor = getPrismContext().getPrismJaxbProcessor();
			Object realValue = null;
			
			if (propertyDefinition.getTypeName().getLocalPart().equals("any")){
				return null;
			}
			
			Class<T> expectedJavaType = XsdTypeMapper.toJavaType(propertyDefinition.getTypeName());
	    	if (expectedJavaType == null) {
	    		expectedJavaType = (Class<T>) schemaRegistry.determineCompileTimeClass(propertyDefinition.getTypeName());
	    	}
	    	

	    	Object value = null;	
	    	ObjectMapper mapper = new ObjectMapper();
	    	SimpleModule module = new SimpleModule("asd", new Version(0, 0, 0, "vvv"));
	    	module.addDeserializer(QName.class, new QNameDeserializer());
	    	module.addDeserializer(Element.class, new DomElementJsonDeserializer());
//	    	module.addDeserializer(, deser)
	    	JaxbElementDeserializer jaxbDeserializer = new JaxbElementDeserializer();
	    	jaxbDeserializer.setExpectedClass(expectedJavaType);
	    	jaxbDeserializer.setNode(valueElement);
	    	jaxbDeserializer.setPrismSchema(prismSchema);
	    	module.addDeserializer(JAXBElement.class, jaxbDeserializer);
//	    	module.addDeserializer(Element.class, new DOMDeserializer());
	    	mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
	    	mapper.registerModule(module);
//	    	JaxbAnnotationModule jaxbModule = new JaxbAnnotationModule();
//	    	mapper.registerModule(jaxbModule);
//	    mapper.
//	    	module.
	    	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	    	mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, false);
//	    	mapper.configure(Feature.USE_GETTERS_AS_SETTERS, true);
	    	
	    	System.out.println("expected java type: " + expectedJavaType.getSimpleName());
//	    	PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
	    	
//	    	mapper.getTypeFactory().
	    	
	    	
	    	
//	    	if (jaxbProcessor.canConvert(expectedJavaType)){
//	    		ObjectReader r = mapper.reader(JAXBElement.class);
//	    		value = r.readValue(valueElement);
//		    	JAXBElement e = new JAXBElement(typeName, expectedJavaType, value);
//		    	System.out.println("cal convert");
//	    		value = r.readValue(valueElement);
//	    	} else 	
	    		if (typeName.equals(PolyStringType.COMPLEX_TYPE)) {
	    		ObjectReader r = mapper.reader(PolyStringType.class);
		    	
				 value = r.readValue(valueElement);
//	    	} 
//	    	else if (Element.class.isAssignableFrom(expectedJavaType)){
//	    		Document doc = DOMUtil.getDocument();
//	    		readElementValue(doc, valueElement, null);
			} else{
				ObjectReader r = mapper.reader(expectedJavaType);
				value = r.readValue(valueElement);
			}
//					Object value = null;
//					if (expectedJavaType.equals(PolyString.class)){
//						if (valueElement.isObject()){
//							JsonNode orig = valueElement.get("orig");
//							JsonNode norm = valueElement.get("norm");
//							value = new PolyString(orig.asText(), norm.asText());
//							System.out.println("orig: " + orig.asText() +" norm: " + norm.asText());						
//						}
//						
//					}
//					
					realValue = JavaTypeConverter.convert(expectedJavaType, value);
				 
			postProcessPropertyRealValue((T) realValue);
			return (T) realValue;
		}

//		private void readElementValue(Document doc, JsonNode valueElement, Element root) {
//			Iterator<Entry<String, JsonNode>> elements = valueElement.getFields();
//    		while (elements.hasNext()){
//    			Entry<String, JsonNode> element = elements.next();
//    			Element e = doc.createElement(element.getKey());
//    			if (root != null){
//    				root.appendChild(e);
//    			}
//    			if (element.getValue().isObject()){
//    				readElementValue(doc, element.getValue(), e);
//    			} else{
//    				e.setTextContent(valueElement.asText());
//    			}
//    			
//    		}
//			
//		}

		private <T> void postProcessPropertyRealValue(T realValue) {
			if (realValue == null) {
				return;
			}
			PrismUtil.recomputeRealValue(realValue, getPrismContext());
		}
		
		public PrismReference parsePrismReference(JsonNode values, QName propName,
				PrismReferenceDefinition referenceDefinition) throws SchemaException, JsonParseException, JsonMappingException, IOException {
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
				oid = value.get("_oid").asText();
			}
			
			
			QName type = null;
			if (value.get("_type") != null){
				type = new QName(defaultNs, value.get("_type").asText());
			}
//			if (type != null) {
//				DOMUtil.validateNonEmptyQName(type, " in reference type in " + DOMUtil.getQName(element));
//			}
			PrismReferenceValue refVal = new PrismReferenceValue(oid);
			refVal.setTargetType(type);
			
			String description = null;
			if (value.get("description") != null){
				description = value.get("description").asText();
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
				PrismReferenceDefinition referenceDefinition) throws SchemaException, JsonParseException, JsonMappingException, IOException {
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
					typeVal = type.asText();
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

		@Override
		public XNode parse(File file) throws SchemaException, IOException {
			return parseObject(new FileInputStream(file));
//			return null;
		}

		@Override
		public XNode parse(String dataString) throws SchemaException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean canParse(File file) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canParse(String dataString) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String serializeToString(XNode xnode, QName rootElementName) throws SchemaException {
			return serializeToJson(xnode, rootElementName);
		}

		@Override
		public String serializeToString(RootXNode xnode) throws SchemaException {
			// TODO Auto-generated method stub
			return null;
		}
}
