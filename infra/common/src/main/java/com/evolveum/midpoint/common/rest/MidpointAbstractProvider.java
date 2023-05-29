/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public abstract class MidpointAbstractProvider<T> extends AbstractConfigurableProvider implements MessageBodyReader<T>, MessageBodyWriter<T>{

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAbstractProvider.class);

    @Autowired protected PrismContext prismContext;
    @Autowired protected LocalizationService localizationService;

    @Override
    public void init(List<ClassResourceInfo> cris) {
        setEnableStreaming(true);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        if (type.getPackage().getName().startsWith("com.evolveum.midpoint") || type.getPackage().getName().startsWith("com.evolveum.prism")){
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
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");
        String serializedForm;

        PrismSerializer<String> serializer = getSerializer()
                .options(SerializationOptions.createSerializeReferenceNames());

        try {
            if (object instanceof ObjectType) {
                ObjectType ot = (ObjectType) object;
                serializedForm = serializer.serialize(ot.asPrismObject());
            } else if (object instanceof PrismObject) {
                serializedForm = serializer.serialize((PrismObject<?>) object);
            } else if (object instanceof OperationResult) {
                Function<LocalizableMessage, String> resolveKeys = msg -> localizationService.translate(msg, Locale.US);
                OperationResultType operationResultType = ((OperationResult) object).createOperationResultType(resolveKeys);
                serializedForm = serializer.serializeAnyData(operationResultType, fakeQName);
            } else {
                serializedForm = serializer.serializeAnyData(object, fakeQName);
            }
            entityStream.write(serializedForm.getBytes(StandardCharsets.UTF_8));
        } catch (SchemaException | RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Couldn't marshal element to string: {}", e, object);
        }
    }

    protected abstract PrismSerializer<String> getSerializer();
    protected abstract PrismParser getParser(InputStream entityStream);

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        if (type.getPackage().getName().startsWith("com.evolveum.midpoint") || type.getPackage().getName().startsWith("com.evolveum.prism")){
            return true;
        }
        return false;
    }

    @Override
    public T readFrom(Class<T> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        if (entityStream == null) {
            return null;
        }

        PrismParser parser = getParser(entityStream);

        T object;
        try {
            if (PrismObject.class.isAssignableFrom(type)) {
                object = (T) parser.parse();
            } else {
                object = parser.parseRealValue();            // TODO consider prescribing type here (if no converter is specified)
            }

            if (object != null && !type.isAssignableFrom(object.getClass())) {    // TODO treat multivalues here
                Optional<Annotation> converterAnnotation = Arrays.stream(annotations).filter(a -> a instanceof Converter).findFirst();
                if (converterAnnotation.isPresent()) {
                    Class<? extends ConverterInterface> converterClass = ((Converter) converterAnnotation.get()).value();
                    ConverterInterface converter;
                    try {
                        converter = converterClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new SystemException("Couldn't instantiate converter class " + converterClass, e);
                    }
                    object = (T) converter.convert(object);
                }
            }
            return object;
        } catch (SchemaException ex) {
            throw new WebApplicationException(ex);
        }
    }
}
