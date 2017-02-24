package com.evolveum.midpoint.testing.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;

import com.evolveum.midpoint.model.impl.rest.MidpointXmlProvider;
import com.evolveum.midpoint.prism.PrismObject;

@Consumes("*/*")
@Produces("*/*")
public class TestProvider<T> extends MidpointXmlProvider<T> {
	
	@Override
	public void writeTo(T object, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
					throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		super.writeTo(object, type, genericType, annotations, mediaType, httpHeaders, entityStream);
	}
	
	
	@Override
	public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
					throws IOException, WebApplicationException {
		
		if (String.class.isAssignableFrom(type.getClass())) {
			return (T) IOUtils.toString(entityStream);
		}
		
		T result = super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
		if (result instanceof PrismObject) {
			return (T) ((PrismObject) result).asObjectable();
		}
		
		return result;
	}
	
}