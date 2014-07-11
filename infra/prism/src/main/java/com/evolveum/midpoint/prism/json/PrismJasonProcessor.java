package com.evolveum.midpoint.prism.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.prism.parser.json.PolyStringSerializer;
import com.evolveum.midpoint.prism.parser.json.QNameDeserializer;
import com.evolveum.midpoint.prism.parser.json.QNameSerializer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.AnnotationIntrospector.ReferenceProperty.Type;
import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.fasterxml.jackson.module.jaxb.deser.DomElementJsonDeserializer;
import com.fasterxml.jackson.module.jaxb.ser.DataHandlerJsonSerializer;
import com.fasterxml.jackson.module.jaxb.ser.DomElementJsonSerializer;

public class PrismJasonProcessor {
	
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
//		mapper.configure(Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false);
		
//		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
//		module.addDeserializer(QName.class, new QNameDeserializer());
//		module.addSerializer(PolyString.class, new PolyStringSerializer());
////		module.addSerializer(Node.class, new DOMSerializer());
//		module.addSerializer(Element.class, new DOMSerializer());
//		mapper.registerModule(module);
		
//		mapper.configure(Feature.UNWRAP_ROOT_VALUE, true);
		ObjectReader reader = mapper.reader(valueType);
		
		JsonFactory factory = new JsonFactory();
		factory.createJsonParser(file);
//		reader.
		
		T object = reader.readValue(file);
		PrismObjectDefinition def = schemaRegistry.findObjectDefinitionByCompileTimeClass(valueType);
		
		PrismObject<T> prismObj = object.asPrismObject();
//		prismObj.setDefinition(def);
//		prismObj.applyDefinition(def);
//		prismContext.adopt(prismObj.asObjectable());
		
		//System.out.println("object: \n" + prismObj.debugDump());
		
		return prismObj;
	}
	
	private <T extends Objectable> void  serializeToJson(PrismContainerValue<?> val, JsonGenerator generator) throws JsonGenerationException, IOException{
//		generator.writeStartObject();
//		if (val != null && val.getPath() != null && val.getPath().last() != null){
////		generator.writeFieldName(ItemPath.getName(val.getPath().last()).getLocalPart());
//		
//		}else{
//			generator.writeFieldName(val.getContainer().getCompileTimeClass().getSimpleName());
//		}
		generator.writeStartObject();
		
		for (Item<?> item : val.getItems()){
//			generator.writeFieldName(item.getElementName().getLocalPart());
			
			if (item.isEmpty()){
				continue;
			}
			generator.writeFieldName(item.getElementName().getLocalPart());
			
//			PrismConstants.
			if (item.getValues().size() > 1){
				generator.writeStartArray();
				for (PrismValue v : item.getValues()){
					serializeValue(v, generator);
				}
				generator.writeEndArray();
				continue;
			}
//			
			serializeValue(item.getValues().iterator().next(), generator);
				
//			}
//			generator.writeEndArray();
		}
		generator.writeEndObject();
			}
	
	private void serializeValue(PrismValue v, JsonGenerator generator) throws JsonProcessingException, IOException{
		if (v instanceof PrismPropertyValue){
			Object o = ((PrismPropertyValue) v).getValue();
			serializeProperty(o, generator);
//			generator.writeObject(o);
//			generator.writeString("\n");
		}
		if (v instanceof PrismReferenceValue){
			generator.writeStartObject();
			String oid = ((PrismReferenceValue) v).getOid();
			generator.writeFieldName("oid");
			generator.writeString(oid);
//			generator.writeString("\n");
			QName qname = ((PrismReferenceValue) v).getTargetType();
			if (qname != null){
			generator.writeFieldName("taretType");
			generator.writeString(qname.toString());
			
			}
			generator.writeEndObject();
//			generator.writeString("\n");
		}
		if (v instanceof PrismContainerValue){
//			generator.writeString("\n");
			serializeToJson((PrismContainerValue) v, generator);
//			generator.writeEndObject();
		}
	}
	
	private void serializeProperty(Object o, JsonGenerator generator) throws JsonGenerationException, IOException{
//		if (o instanceof PolyString){
//			generator.writeStartObject();
//			generator.writeStringField("orig", ((PolyString) o).getOrig());
//			generator.writeStringField("norm", ((PolyString) o).getNorm());
//			generator.writeEndObject();
//		} else if (o instanceof Element){
//			serializeElementProperty(o, generator);
//		} else if (o instanceof QName){
//			generator.writeStartObject();
//			generator.writeStringField("namespace", ((QName) o).getNamespaceURI());
//			generator.writeStringField("localPart", ((QName) o).getLocalPart());
//			generator.writeEndObject();
//		} else{
////			TypeVariable[] typeVariable = o.getClass().getTypeParameters();
//			if (containsElement(o)){
//				
//			} else{
				generator.writeObject(o);
//			}
//		}
	}
	
	private boolean containsElement(Object o){
		Field[] fields = o.getClass().getFields();
		for (int i = 0; i < fields.length; i++){
			Field field = fields[i];
			if (Element.class.isAssignableFrom(field.getType())){
				return true;
			}
		}
		return false;
	}
	
	private void serializeElementProperty(Object o, JsonGenerator generator) throws JsonGenerationException, IOException {
		Element rootNode = (Element) o;
		QName root = DOMUtil.getQName(rootNode);
		
//		generator.writeFieldName(root.getLocalPart());
		generator.writeStartObject();
		List<Element> children = DOMUtil.getChildElements(rootNode, root); 
		for (Element child : children){
//			Node child = children.item(i);
			
			if (DOMUtil.hasChildElements(child)){
				serializeElementProperty(child, generator);
			} else{
				QName childName = DOMUtil.getQName(child);
//				if (child.getNodeType() != Node.ATTRIBUTE_NODE){
				generator.writeStringField(childName.getLocalPart(), child.getTextContent());
//				}
			}
			
		}
		generator.writeEndObject();
		
	}

	public <T extends Objectable> void serializeToJson(PrismObject<T> object, OutputStream out) throws IOException, SchemaException{
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("MidpointModule", new Version(0, 0, 0, "aa")); 
//		JaxbAnnotationModule module = new JaxbAnnotationModule();
//		SetupContext ctx = 
//		module.setupModule(mapper);
		module.addSerializer(QName.class, new QNameSerializer());
		module.addSerializer(PolyString.class, new PolyStringSerializer());
//		module.addSerializer(Node.class, new DOMSerializer());
//		module.addSerializer(Element.class, new DOMSerializer());
		
//		JaxbElementSerializer jaxbSerializer = new JaxbElementSerializer();
//		module.addSerializer(JAXBElement.class, jaxbSerializer);
//		module.addSerializer(DataHandler.class, new DataHandlerJsonSerializer());
//		module.addSerializer(Element.class, new DomElementJsonSerializer());
		mapper.registerModule(module);
		
//		jaxbModule.addSerializer(new DataHandlerJsonSerializer());
//		jaxbModule.addSerializer(Element.class, new DomElementJsonSerializer());
//		JaxbAnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
//		 mapper.getSerializationConfig().setAnnotationIntrospector(introspector);
		mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
//		mapper.registerModule(jaxbModule);
//		mapper.configure(com.fasterxml.jackson.c, state)
//		Feature.
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
//		mapper.configure(SerializationFeature.WRITE_NULL_PROPERTIES, false);
//		mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS, false);
//		mapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
		
		ObjectWriter writer = mapper.writer();
//		JsonGenerator jsonGenerator = new J
		JsonFactory factory = new JsonFactory();
//		mapper.configure(org.codehaus.jackson.JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, true);
		JsonGenerator generator = factory.createGenerator(out , JsonEncoding.UTF8);
		
		PrettyPrinter pp = new DefaultPrettyPrinter();
//		pp.
		generator.setPrettyPrinter(pp);
		generator.setCodec(mapper);
//		generator.writeObject(object.asObjectable());
		
				
for (PrismContainerValue<?> pcv : object.getValues()){

			
serializeToJson(pcv, generator);

		}
generator.flush();
generator.close();
	}
	
	JsonParser parser = null;
	public <T extends Objectable> PrismObject<T> parseObject(InputStream inputStream, Class<T> valueType) throws IOException, SchemaException{
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode obj = null;
		try {
			
			JsonFactory facotry = new JsonFactory();
			parser =  facotry.createJsonParser(inputStream);
			parser.setCodec(mapper);
			
			obj = parser.readValueAs(JsonNode.class);
			
			PrismSchema schema = getPrismSchema(valueType);
			
			prismSchema = schema;
			if (schema == null) {
				throw new SchemaException("No schema for namespace " + valueType);
			}
			
			JsonNode type = obj.get("_type");
			String typeValue = null;
			
			PrismObjectDefinition<T> objectDefinition = null;
			if (type != null){
				typeValue = type.asText();
//				QName objQname = new QName(defaultNamespace, typeValue);
//				objectDefinition = schema.findObjectDefinitionByElementName(valueType);
			} else {
				objectDefinition = schema.findObjectDefinitionByCompileTimeClass(valueType);
			}
			
			
//				if (objectDefinition == null) {
//					throw new SchemaException("No definition found for type " + new QName(defaultNamespace, typeValue) + " (as defined by xsi:type)");
//				}
			 PrismObject object = parseObject(obj, objectDefinition.getName(), objectDefinition.getNamespace(), objectDefinition);
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
			
			PrismContainerValue<T> pval = new PrismContainerValue<T>(null, null, container, null, null, prismContext); // TODO set concreteType (if this code would be really used)

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
					//System.out.println("new item name null");
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
//						T realValue = parseAtomicValue(val.getValue(), (PrismPropertyDefinition<T>) def);
//						if (realValue != null) {
//							prop.add(new PrismPropertyValue<T>(realValue));
//						}
//						}else{
//							T realValue = parseAtomicValue(values, propertyDefinition);
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
//	    	JaxbElementDeserializer jaxbDeserializer = new JaxbElementDeserializer();
//	    	jaxbDeserializer.setExpectedClass(expectedJavaType);
//	    	jaxbDeserializer.setNode(valueElement);
//	    	jaxbDeserializer.setPrismSchema(prismSchema);
//	    	module.addDeserializer(JAXBElement.class, jaxbDeserializer);
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
	    	
	    	//System.out.println("expected java type: " + expectedJavaType.getSimpleName());
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
}
