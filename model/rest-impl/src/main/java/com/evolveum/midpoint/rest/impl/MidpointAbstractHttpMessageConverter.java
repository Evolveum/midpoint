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
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

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

        Object object;
        try {
            if (PrismObject.class.isAssignableFrom(clazz)) {
                object = parser.parse();
            } else {
                object = parser.parseRealValue();
            }

            if (object != null && !clazz.isInstance(object)) {
                // This code covers type migrations, eventually we'd like to drop old type support.
                object = migrateType(object, clazz);
            }
            return clazz.cast(object);
        } catch (SchemaException e) {
            throw new HttpMessageNotReadableException("Failure during read", e, inputMessage);
        }
    }

    // this better be T after this is executed
    private Object migrateType(Object object, Class<? extends T> clazz) {
        if (clazz.equals(ExecuteScriptType.class)) {
            // TODO: deprecate ScriptingExpressionType in favour of ExecuteScriptType (next LTS? 4.4? 5.0?)
            if (object instanceof ExecuteScriptType) {
                return object;
            } else if (object instanceof ScriptingExpressionType) {
                return ScriptingExpressionEvaluator.createExecuteScriptCommand((ScriptingExpressionType) object);
            } else {
                throw new IllegalArgumentException("Wrong input value for ExecuteScriptType: " + object);
            }
        }
        // no other migrations
        return object;
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
