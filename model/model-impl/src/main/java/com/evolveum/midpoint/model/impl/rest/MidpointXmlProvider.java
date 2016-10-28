package com.evolveum.midpoint.model.impl.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
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
		try {
			if (object instanceof PrismObject) {
				xml = prismContext.xmlSerializer().serialize((PrismObject) object);
			} else if (object instanceof OperationResult) {
				OperationResultType operationResultType = ((OperationResult) object).createOperationResultType();
				xml = prismContext.xmlSerializer().serializeAnyData(operationResultType, fakeQName);
			} else {
				xml = prismContext.xmlSerializer().serializeAnyData(object, fakeQName);
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
		
//		if (entityStream.available() == 0){
//			return null;
//		}
		
		T object = null;
		try {
			
			if (type.isAssignableFrom(PrismObject.class)){
				object = (T) prismContext.parserFor(entityStream).xml().parse();
			} else {
                object = prismContext.parserFor(entityStream).xml().parseRealValue();
				//object = (T) prismContext.getJaxbDomHack().unmarshalObject(entityStream);
			}
			
//			if (object instanceof ObjectModificationType){
//				
//				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications((ObjectModificationType)object, UserType.class, prismContext);
//				return (T) modifications;
//			}
//			
			return object;
		} catch (SchemaException ex){
			
			throw new WebApplicationException(ex);
			
		} catch (IOException ex){
			throw new IOException(ex);
		}
		
//		return object;
	}

}
