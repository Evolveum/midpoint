/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public abstract class MidpointAbstractHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAbstractHttpMessageConverter.class);

    protected final PrismContext prismContext;
    private final LocalizationService localizationService;

    protected MidpointAbstractHttpMessageConverter(PrismContext prismContext,
            LocalizationService localizationService, MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
        this.prismContext = prismContext;
        this.localizationService = localizationService;
    }

    protected abstract PrismSerializer<String> getSerializer();
    protected abstract PrismParser getParser(InputStream entityStream);

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.getName().startsWith("com.evolveum.midpoint")
                || clazz.getName().startsWith("com.evolveum.prism");
    }

    @Override
    protected T readInternal(@NotNull Class<? extends T> clazz, @NotNull HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        PrismParser parser = getParser(inputMessage.getBody());

        T object;
        try {
            if (PrismObject.class.isAssignableFrom(clazz)) {
                object = (T) parser.parse();
            } else {
                object = parser.parseRealValue();            // TODO consider prescribing type here (if no converter is specified)
            }

/*
// TODO treat multivalues here - there is just one @Converter usage and one implementation: ExecuteScriptConverter
// com.evolveum.midpoint.model.impl.ModelRestService.executeScript - will we need argument resolver for this
 if (object != null && !clazz.isAssignableFrom(object.getClass())) {
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
*/
            return object;
        } catch (SchemaException e) {
            throw new HttpMessageNotReadableException("Failure during read", e, inputMessage);
        }
    }

    @Override
    protected void writeInternal(@NotNull T object, @NotNull HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
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
            outputMessage.getBody().write(serializedForm.getBytes(StandardCharsets.UTF_8));
        } catch (SchemaException | RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Couldn't marshal element to string: {}", e, object);
        }
    }
}
