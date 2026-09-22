/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Converts raw result of script or Java method execution into {@link PrismValue} instances.
 */
@NullMarked
public class OutputValuesConvertor {

    private final Protector protector;
    @Nullable private final ItemDefinition<?> outputDefinition;
    @Nullable private final Function<Object, Object> additionalConvertor;
    private final String contextDescription;

    private static final Trace LOGGER = TraceManager.getTrace(OutputValuesConvertor.class);

    public OutputValuesConvertor(
            Protector protector,
            @Nullable ItemDefinition<?> outputDefinition,
            @Nullable Function<Object, Object> additionalConvertor,
            String contextDescription) {
        this.protector = protector;
        this.outputDefinition = outputDefinition;
        this.contextDescription = contextDescription;
        this.additionalConvertor = additionalConvertor;
    }

    public <T, V extends PrismValue> List<V> convertResultToPrismValues(@Nullable Object rawResult)
            throws ExpressionEvaluationException {

        if (outputDefinition == null) {
            // No outputDefinition may mean "void" return type
            // or it can mean that we do not have definition, because this is something non-prism (e.g. report template).
            // Either way we can return immediately, without any value conversion. Just wrap the value in fake PrismPropertyValue.
            // For no value/null we return empty list.
            List<V> convertedResultValues = new ArrayList<>();
            if (rawResult instanceof Collection<?> collection) {
                collection.forEach(rawResultValue -> {
                    if (rawResultValue != null) {
                        //noinspection unchecked
                        convertedResultValues.add((V) toPrismValue(rawResultValue));
                    }
                });
            } else if (rawResult != null) {
                //noinspection unchecked
                convertedResultValues.add((V) toPrismValue(rawResult));
            }
            return convertedResultValues;
        }

        Class<T> javaReturnType = determineJavaReturnType(outputDefinition);
        LOGGER.trace("expected return type: XSD={}, Java={}", outputDefinition.getTypeName(), javaReturnType);

        List<V> values = new ArrayList<>();

        // TODO: what about PrismContainer and PrismReference? Shouldn't they be processed in the same way as PrismProperty?
        if (rawResult instanceof Collection<?> collection) {
            for (Object rawResultValue : collection) {
                T evalResult = convertScalarResult(javaReturnType, rawResultValue);
                values.add(
                        ExpressionUtil.convertToPrismValue(
                                evalResult, outputDefinition, contextDescription));
            }
        } else if (rawResult instanceof PrismProperty<?>) {
            //noinspection unchecked
            values.addAll(
                    (Collection<? extends V>) PrismValueCollectionsUtil.cloneCollection(
                            ((PrismProperty<T>) rawResult).getValues()));
        } else if (rawResult != null) {
            T evalResult = convertScalarResult(javaReturnType, rawResult);
            values.add(
                    ExpressionUtil.convertToPrismValue(evalResult, outputDefinition, contextDescription));
        }

        return values;
    }

    private <T> Class<T> determineJavaReturnType(ItemDefinition<?> outputDefinition) {
        QName xsdReturnType = outputDefinition.getTypeName();

        // Ugly hack. Intended to allow xsd:anyType return type, see MID-6775.
        if (QNameUtil.match(xsdReturnType, DOMUtil.XSD_ANYTYPE)) {
            //noinspection unchecked
            return (Class<T>) Object.class;
        }

        // the most simple types (e.g. xsd:string)
        Class<T> fromMapper = XsdTypeMapper.toJavaType(xsdReturnType);
        if (fromMapper != null) {
            return fromMapper;
        }

        // statically-defined beans (for both complex and simple type definitions)
        Class<T> fromSchemaRegistry = PrismContext.get().getSchemaRegistry().determineCompileTimeClass(xsdReturnType);
        if (fromSchemaRegistry != null) {
            return fromSchemaRegistry;
        }

        if (outputDefinition instanceof PrismContainerDefinition<?>) {
            // This is the case when we need a container, but we do not have compile-time class for that
            // E.g. this may be container in object extension (MID-5080)
            //noinspection unchecked
            return (Class<T>) PrismContainerValue.class;
        }

        // TODO quick and dirty hack - because this could be because of enums defined in schema extension (MID-2399)
        //  ...and enums (xsd:simpleType) are not parsed into ComplexTypeDefinitions
        //noinspection unchecked
        return (Class<T>) String.class;
    }

    private <T> T convertScalarResult(Class<T> expectedType, Object rawValue)
            throws ExpressionEvaluationException {
        try {
            return ExpressionUtil.convertValue(expectedType, additionalConvertor, rawValue, protector);
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription, e);
        }
    }

    // FIXME deduplicate with PrismValue#toPrismValue (there are minor differences)
    private static PrismValue toPrismValue(Object realValue) {
        if (realValue instanceof Objectable objectable) {
            return objectable.asPrismObject().getValue();
        } else if (realValue instanceof Containerable containerable) {
            return containerable.asPrismContainerValue();
        } else if (realValue instanceof Referencable referencable) {
            return referencable.asReferenceValue();
        } else {
            return PrismContext.get().itemFactory().createPropertyValue(realValue);
        }
    }
}
