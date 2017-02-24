package com.evolveum.midpoint.model.impl.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;


@Produces({"application/xml", "application/*+xml", "text/xml", "application/yaml", "application/x-yaml", "text/yaml", "text/x-yaml", "application/json"})
@Consumes({"application/xml", "application/*+xml", "text/xml", "application/yaml", "application/x-yaml", "text/yaml", "text/x-yaml", "application/json"})
@Provider
public class MidpointXmlProvider<T> extends AbstractConfigurableProvider implements MessageBodyReader<T>, MessageBodyWriter<T>{

	private static transient Trace LOGGER = TraceManager.getTrace(MidpointXmlProvider.class);
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
//	@Override
    public void init(List<ClassResourceInfo> cris) {
        setEnableStreaming(true);
    }
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (type.getPackage().getName().startsWith("com.evolveum.midpoint")){
			return true;
		}
		return false;
	}

	@Override
	public long getSize(T t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public void writeTo(T object, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {

		// TODO implement in the standard serializer; also change root name
		QName fakeQName = new QName(PrismConstants.NS_PREFIX + "debug", "debugPrintObject");
		String xml;
		
		PrismSerializer<String> serializer = null;
		String accept = (String) httpHeaders.getFirst("Accept");
		
		if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
			serializer = prismContext.jsonSerializer();
		} else if ("yaml".equals(mediaType.getSubtype())) {
			serializer = prismContext.yamlSerializer();
		} else {
			serializer = prismContext.xmlSerializer();
		}
		
		try {
			if (object instanceof PrismObject) {
				xml = serializer.serialize((PrismObject<?>) object);
			} else if (object instanceof OperationResult) {
				OperationResultType operationResultType = ((OperationResult) object).createOperationResultType();
				xml = serializer.serializeAnyData(operationResultType, fakeQName);
			} else {
				xml = serializer.serializeAnyData(object, fakeQName);
			}
			entityStream.write(xml.getBytes("utf-8"));
		} catch (SchemaException | RuntimeException e) {
			LoggingUtils.logException(LOGGER, "Couldn't marshal element to string: {}", e, object);
		}
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return true;
	}

	@Override
	public T readFrom(Class<T> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		
		if (entityStream == null){
			return null;
		}
		
		String mimeType = (String) httpHeaders.getFirst("Content-Type");
		
		PrismParser parser = prismContext.parserFor(entityStream);
		if (StringUtils.isBlank(mimeType)) {
			parser = parser.xml();
		} else {

			if (MediaType.APPLICATION_JSON.equals(mimeType)) {
				parser = parser.json();
			} else if (mimeType.contains("yaml")) {
				parser = parser.yaml();
			} else {
				parser = parser.xml();
			}
		}
				
		T object = null;
		try {
			LOGGER.info("type of respose: {}", type);
			if (PrismObject.class.isAssignableFrom(type) || type.isAssignableFrom(PrismObject.class) || ObjectType.class.isAssignableFrom(type)){
				object = (T) parser.parse();
			} else {
                object = parser.parseRealValue();
			}
			
			return object;
		} catch (SchemaException ex){
			
			throw new WebApplicationException(ex);
			
		} catch (IOException ex){
			throw new IOException(ex);
		}
		
//		return object;
	}
	

}
