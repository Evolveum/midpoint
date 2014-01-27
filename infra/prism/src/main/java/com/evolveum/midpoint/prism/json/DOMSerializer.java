package com.evolveum.midpoint.prism.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.util.DOMUtil;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class DOMSerializer extends StdSerializer<Element>{

	public DOMSerializer() {
		super(Element.class);// TODO Auto-generated constructor stub
	}
	
	protected DOMSerializer(Class<Element> t) {
		super(t);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void serialize(Element value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
			JsonGenerationException {
		System.out.println("wualaaaa DOM serialization");
		
		
//		   jgen.writeStartObject();
	        jgen.writeObjectFieldStart(value.getTagName());
//	        if (value.getNamespaceURI() != null) {
//	            jgen.writeStringField("namespace", value.getNamespaceURI());
//	        }
	        NamedNodeMap attributes = value.getAttributes();
	        if (attributes != null && attributes.getLength() > 0) {
	        	jgen.writeArrayFieldStart("properties");
	            for (int i = 0; i < attributes.getLength(); i++) {
	                Attr attribute = (Attr) attributes.item(i);
	                
	                if (!attribute.getValue().contains("http://")){
//	                jgen.writeStartObject();
//	                jgen.writeStringField("$", attribute.getValue());
	                jgen.writeStringField(attribute.getName(), attribute.getValue());
	                }
//	                String ns = attribute.getNamespaceURI();
//	                if (ns != null) {
//	                    jgen.writeStringField("namespace", ns);
//	                }
//	                jgen.writeEndObject();
	            }
	            jgen.writeEndArray();
	        }

	        NodeList children = value.getChildNodes();
	        if (children != null && children.getLength() > 0) {
//	            jgen.writeArrayFieldStart("children");
	        	jgen.writeStartObject();
	            for (int i = 0; i < children.getLength(); i++) {
	                Node child = children.item(i);
	                switch (child.getNodeType()) {
	                    case Node.CDATA_SECTION_NODE:
	                    case Node.TEXT_NODE:
	                       if (StringUtils.isNotBlank(child.getNodeValue())){
	                    	jgen.writeStartObject();
	                        
	                        jgen.writeStringField("$", child.getNodeValue());
	                        jgen.writeEndObject();
	                       }
	                        break;
	                    case Node.ELEMENT_NODE:
	                        serialize((Element) child, jgen, provider);
	                        break;
	                }
	            }
	            jgen.writeEndObject();
	        }
	        jgen.writeEndObject();
		
//		((ObjectMapper)jgen.getCodec()).enable(com.fasterxml.jackson.core.JsonParser.Feature.WRITE_NULL_PROPERTIES);
//		serializeElementProperty(value, jgen);
//		((ObjectMapper)jgen.getCodec()).disable(Feature.WRITE_NULL_PROPERTIES);
	}
	
	private void serializeElementProperty(Element rootNode, JsonGenerator generator) throws JsonGenerationException, IOException {
		QName root = DOMUtil.getQName(rootNode);
		
		if (DOMUtil.hasChildElements(rootNode)){
			generator.writeStartObject();
			List<Element> children = DOMUtil.listChildElements(rootNode); 
			for (Element child : children){
				QName childName = DOMUtil.getQName(child);
				generator.writeFieldName(childName.getLocalPart());
				serializeElementProperty(child, generator);
			
		}
		generator.writeEndObject();
		} else{
			System.out.println("DOM root node: " + root.toString());
			System.out.println("DOM value: " + rootNode.getTextContent());
			generator.writeString(rootNode.getTextContent());
		}
		
		
	}
	
	 @Override
	    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
	            throws JsonMappingException
	    {
	        ObjectNode o = createSchemaNode("object", true);
	        o.put("name", createSchemaNode("string"));
	        o.put("namespace", createSchemaNode("string", true));
	        o.put("properties", createSchemaNode("array", true));
//	        o.put("children", createSchemaNode("array", true));
	        return o;
	    }

}
