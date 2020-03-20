/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnyValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NamedValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.NotNull;

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

    public static Collection<AnyValueType> toAnyValueTypeList(Object object, PrismContext prismContext) {
        if (object instanceof Collection<?>) {
            return ((Collection<?>) object).stream()
                    .map(o -> toAnyValueType(o, prismContext))
                    .collect(Collectors.toList());
        } else {
            return singleton(toAnyValueType(object, prismContext));
        }
    }

    private static AnyValueType toAnyValueType(Object object, PrismContext prismContext) {
        AnyValueType rv = new AnyValueType();
        setAnyValueTypeContent(object, rv, prismContext);
        return rv;
    }

    public static NamedValueType toNamedValueType(Object object, QName name, PrismContext prismContext) {
        NamedValueType rv = new NamedValueType();
        rv.setName(name);
        setAnyValueTypeContent(object, rv, prismContext);
        return rv;
    }

    private static void setAnyValueTypeContent(Object object, AnyValueType anyValue, PrismContext prismContext) {
        if (object instanceof PrismValue) {
            PrismValue prismValue = (PrismValue) object;
            if (prismValue.hasRealClass()) {
                setAnyValueReal(prismValue.getRealValue(), anyValue, prismContext);
            } else {
                setAnyValueDynamic(prismValue, anyValue, prismContext);
            }
        } else {
            setAnyValueReal(object, anyValue, prismContext);
        }
    }

    private static void setAnyValueDynamic(PrismValue prismValue, AnyValueType anyValue, PrismContext prismContext) {
        anyValue.setValue(new RawType(prismValue, prismValue.getTypeName(), prismContext));
    }

    private static void setAnyValueReal(Object object, AnyValueType anyValue, PrismContext prismContext) {
        if (object != null) {
            QName typeName = prismContext.getSchemaRegistry().determineTypeForClass(object.getClass());
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

    public static boolean isAtLeast(TracingLevelType level, @NotNull TracingLevelType threshold) {
        return level != null && level.ordinal() >= threshold.ordinal();
    }
}
