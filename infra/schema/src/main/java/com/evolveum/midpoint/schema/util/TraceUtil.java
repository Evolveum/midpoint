/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnyValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NamedValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

/**
 * Utility methods related to tracing.
 */
public class TraceUtil {

    public static final Integer DEFAULT_RECORD_OBJECTS_FOUND = 5;
    public static final Integer DEFAULT_RECORD_OBJECT_REFERENCES_FOUND = 10;

    public static Collection<AnyValueType> toAnyValueTypeList(Object object) {
        if (object instanceof Collection<?>) {
            return ((Collection<?>) object).stream()
                    .map(o -> toAnyValueType(o))
                    .collect(Collectors.toList());
        } else {
            return singleton(toAnyValueType(object));
        }
    }

    private static AnyValueType toAnyValueType(Object object) {
        AnyValueType rv = new AnyValueType();
        setAnyValueTypeContent(object, rv);
        return rv;
    }

    public static NamedValueType toNamedValueType(Object object, QName name) {
        NamedValueType rv = new NamedValueType();
        rv.setName(name);
        setAnyValueTypeContent(object, rv);
        return rv;
    }

    private static void setAnyValueTypeContent(Object object, AnyValueType anyValue) {
        if (object instanceof PrismValue) {
            PrismValue prismValue = (PrismValue) object;
            boolean emptyEmbeddedValue = prismValue instanceof PrismPropertyValue && ((PrismPropertyValue) prismValue).getValue() == null;
            if (emptyEmbeddedValue) {
                // very strange case - let's simply skip it; there's nothing to store to AnyValueType here
            } else {
                if (prismValue.hasRealClass() && !prismValue.hasValueMetadata()) {
                    setAnyValueReal(prismValue.getRealValue(), anyValue);
                } else {
                    setAnyValueDynamic(prismValue, anyValue);
                }
            }
        } else {
            setAnyValueReal(object, anyValue);
        }
    }

    private static void setAnyValueDynamic(PrismValue prismValue, AnyValueType anyValue) {
        anyValue.setValue(new RawType(prismValue, prismValue.getTypeName()));
    }

    private static void setAnyValueReal(Object object, AnyValueType anyValue) {
        if (object != null) {
            QName typeName = PrismContext.get().getSchemaRegistry().determineTypeForClass(object.getClass());
            if (typeName != null) {
                // assuming we'll be able to serialize this value
                anyValue.setValue(object);
            } else if (object instanceof DebugDumpable) {
                anyValue.setTextValue(((DebugDumpable) object).debugDump());
            } else {
                anyValue.setTextValue(PrettyPrinter.prettyPrint(object));
            }
        }
    }

    public static boolean isAtLeastMinimal(TracingLevelType level) {
        return isAtLeast(level, TracingLevelType.MINIMAL);
    }

    public static boolean isAtLeastNormal(TracingLevelType level) {
        return isAtLeast(level, TracingLevelType.NORMAL);
    }

    public static boolean isAtLeast(@Nullable TracingLevelType level, @Nullable TracingLevelType threshold) {
        return MoreObjects.firstNonNull(level, TracingLevelType.OFF).ordinal() >=
                MoreObjects.firstNonNull(threshold, TracingLevelType.OFF).ordinal();
    }
}
