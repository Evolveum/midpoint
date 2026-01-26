/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.model.common.expression.script.cel.value.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.CelValue;
import dev.cel.common.values.NullValue;
import dev.cel.common.values.OpaqueValue;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Maintains mapping of XSD types (qnames) and Java types to CEL types
 *
 * @author Radovan Semancik
 */
public class CelTypeMapper {

    private static final Map<CelType, QName> CEL_TO_XSD_TYPE_MAP = new HashMap<>();
    private static final Map<QName, CelType> XSD_TO_CEL_TYPE_MAP = new HashMap<>();
    private static final Map<CelType, Class<?>> CEL_TO_JAVA_TYPE_MAP = new HashMap<>();
    private static final Map<Class<?>, CelType> JAVA_TO_CEL_TYPE_MAP = new HashMap<>();

    private static final Trace LOGGER = TraceManager.getTrace(CelTypeMapper.class);

    private static void initXsdTypeMap() {
        addXsdMapping(SimpleType.STRING, DOMUtil.XSD_STRING, true);
        addXsdMapping(SimpleType.INT, DOMUtil.XSD_INT, true);
        addXsdMapping(SimpleType.INT, DOMUtil.XSD_INTEGER, false);
        addXsdMapping(SimpleType.DOUBLE, DOMUtil.XSD_DECIMAL, false);
        addXsdMapping(SimpleType.DOUBLE, DOMUtil.XSD_DOUBLE, true);
        addXsdMapping(SimpleType.DOUBLE, DOMUtil.XSD_FLOAT, false);
        addXsdMapping(SimpleType.INT, DOMUtil.XSD_LONG, false);
        addXsdMapping(SimpleType.INT, DOMUtil.XSD_SHORT, false);
        addXsdMapping(SimpleType.INT, DOMUtil.XSD_BYTE, false);
        addXsdMapping(SimpleType.BOOL, DOMUtil.XSD_BOOLEAN, true);
        addXsdMapping(SimpleType.BYTES, DOMUtil.XSD_BASE64BINARY, true);
        addXsdMapping(SimpleType.TIMESTAMP, DOMUtil.XSD_DATETIME, true);
        addXsdMapping(SimpleType.DURATION, DOMUtil.XSD_DURATION, true);

//        addMapping(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);
//        addMapping(UniformItemPath.class, ItemPathType.COMPLEX_TYPE, false);
//        addMapping(ItemPath.class, ItemPathType.COMPLEX_TYPE, false);

        addXsdMapping(QNameCelValue.CEL_TYPE, DOMUtil.XSD_QNAME, true);
        addXsdMapping(PolyStringCelValue.CEL_TYPE, PrismConstants.POLYSTRING_TYPE_QNAME, true);

//        addXsdToCelMapping(DOMUtil.XSD_ANYURI, String.class);
    }

    private static void initJavaTypeMap() {
        addJavaMapping(SimpleType.STRING, String.class, true);
        addJavaMapping(SimpleType.INT, Integer.class, true);
        addJavaMapping(SimpleType.INT, int.class, false);
        addJavaMapping(SimpleType.DOUBLE, Double.class, false);
        addJavaMapping(SimpleType.DOUBLE, double.class, true);
        addJavaMapping(SimpleType.DOUBLE, Float.class, false);
        addJavaMapping(SimpleType.DOUBLE, float.class, false);
        addJavaMapping(SimpleType.INT, Long.class, false);
        addJavaMapping(SimpleType.INT, long.class, false);
        addJavaMapping(SimpleType.BOOL, Boolean.class, true);
        addJavaMapping(SimpleType.BOOL, boolean.class, false);
        addJavaMapping(SimpleType.BYTES, Byte[].class, true);
        addJavaMapping(SimpleType.BYTES, byte[].class, false);
        addJavaMapping(SimpleType.TIMESTAMP, XMLGregorianCalendar.class, true);
        addJavaMapping(SimpleType.DURATION, Duration.class, true);
        addJavaMapping(SimpleType.DYN, Object.class, true);
        addJavaMapping(SimpleType.NULL_TYPE, void.class, true);

        addJavaMapping(ContainerValueCelValue.CEL_TYPE, Containerable.class, true);
        addJavaMapping(ObjectCelValue.CEL_TYPE, Objectable.class, true);

        // TODO: temporary
        addJavaMapping(SimpleType.DYN, PrismContext.class, false);
        addJavaMapping(SimpleType.DYN, ProtectedStringType.class, false);
        addJavaMapping(SimpleType.DYN, Collection.class, false);
        addJavaMapping(SimpleType.DYN, Map.class, false);
        addJavaMapping(SimpleType.DYN, Referencable.class, false);
        addJavaMapping(SimpleType.DYN, ByteBuffer.class, false);
        addJavaMapping(SimpleType.DYN, Class.class, false);
        addJavaMapping(SimpleType.DYN, ItemPathType.class, false);
        addJavaMapping(SimpleType.DYN, PrismContainerValue.class, false);
        addJavaMapping(SimpleType.DYN, PrismValue.class, false);
        addJavaMapping(SimpleType.DYN, PrismProperty.class, false);
        addJavaMapping(SimpleType.DYN, ObjectType.class, false);
        addJavaMapping(SimpleType.DYN, ObjectReferenceType.class, false);
        addJavaMapping(SimpleType.DYN, ResourceType.class, false);
        addJavaMapping(SimpleType.DYN, ShadowType.class, false);
        addJavaMapping(SimpleType.DYN, TaskType.class, false);
        addJavaMapping(SimpleType.DYN, QName.class, false);
        addJavaMapping(SimpleType.DYN, groovy.namespace.QName.class, false);
        addJavaMapping(SimpleType.DYN, Object[].class, false);

//        addMapping(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);
//        addMapping(UniformItemPath.class, ItemPathType.COMPLEX_TYPE, false);
//        addMapping(ItemPath.class, ItemPathType.COMPLEX_TYPE, false);
//        addMapping(QName.class, DOMUtil.XSD_QNAME, true);

        addJavaMapping(PolyStringCelValue.CEL_TYPE, PolyString.class, true);
        addJavaMapping(PolyStringCelValue.CEL_TYPE, PolyStringType.class, false);
        addJavaMapping(QNameCelValue.CEL_TYPE, QName.class, true);
    }


    private static void addXsdMapping(CelType celType, QName xsdType, boolean bidirectional) {
        LOGGER.trace("Adding XSD-CEL type mapping {} {} {} ", celType, bidirectional ? "<->" : " ->", xsdType);
        addXsdToCelMapping(xsdType, celType);
        if (bidirectional) {
            CEL_TO_XSD_TYPE_MAP.put(celType, xsdType);
        }
    }

    private static void addXsdToCelMapping(QName xsdType, CelType celType) {
        XSD_TO_CEL_TYPE_MAP.put(xsdType, celType);
        XSD_TO_CEL_TYPE_MAP.put(QNameUtil.nullNamespace(xsdType), celType);
    }

    private static void addJavaMapping(CelType celType, Class<?> javaType, boolean bidirectional) {
        LOGGER.trace("Adding Java-CEL type mapping {} {} {} ", celType, bidirectional ? "<->" : " ->", javaType);
        JAVA_TO_CEL_TYPE_MAP.put(javaType, celType);
        if (bidirectional) {
            CEL_TO_JAVA_TYPE_MAP.put(celType, javaType);
        }
    }

    @NotNull
    public static QName toXsdType(CelType celType) {
        QName xsdType = getCelToXsdMapping(celType);
        if (xsdType == null) {
            throw new IllegalArgumentException("No XSD mapping for CEL type " + celType);
        } else {
            return xsdType;
        }
    }

    public static QName getCelToXsdMapping(CelType celType) {
        return CEL_TO_XSD_TYPE_MAP.get(celType);
    }

    public static CelType getXsdToCelMapping(QName xsdType) {
        return XSD_TO_CEL_TYPE_MAP.get(xsdType);
    }

    @NotNull
    public static CelType toCelType(@NotNull QName xsdType) {
        CelType celType = getCelType(xsdType);
        if (celType == null) {
            throw new IllegalArgumentException("No CEL mapping for XSD type " + xsdType);
        } else {
            return celType;
        }
    }

    public static CelType toCelType(@NotNull Class<?> javaType) {
        CelType celType = getCelType(javaType);
        if (celType == null) {
            throw new IllegalArgumentException("No CEL mapping for Java type " + javaType);
        } else {
            return celType;
        }
    }

    @NotNull
    public static CelType toCelType(TypedValue<?> typedValue) {
        ItemDefinition<?> def = typedValue.getDefinition();
        if (def == null) {
            Class<?> typeClass = typedValue.getTypeClass();
            if (typeClass == null) {
                throw new IllegalStateException("Typed value " + typedValue + " does not have neither definition nor class");
            }
            return SimpleType.DYN;
//            throw new NotImplementedException("Cannot convert class "+typeClass.getSimpleName()+" to CEL");
            // TODO: convert based on class
        } else {
            if (def instanceof PrismPropertyDefinition<?> propDef) {
                return CelTypeMapper.toCelType(propDef.getTypeName());
            } else if (def instanceof PrismObjectDefinition<?>) {
                // TODO: something more sophisticated? Maybe handled by TypeMapper?
                return ObjectCelValue.CEL_TYPE;
            } else if (def instanceof PrismContainerDefinition<?>) {
                // TODO: something more sophisticated? Maybe handled by TypeMapper?
                return ContainerValueCelValue.CEL_TYPE;
            }
            throw new NotImplementedException("Cannot convert "+def+" to CEL");
        }
    }

    @Nullable
    public static CelType getCelType(@NotNull QName xsdType) {
        return XSD_TO_CEL_TYPE_MAP.get(xsdType);
    }

    @Nullable
    public static CelType getCelType(@NotNull Class<?> javaType) {
        return JAVA_TO_CEL_TYPE_MAP.get(javaType);
    }

    @NotNull
    public static Class<?> toJavaType(CelType celType) {
        Class<?> javaType = getCelToJavaMapping(celType);
        if (javaType == null) {
            throw new IllegalArgumentException("No Java mapping for CEL type " + celType);
        } else {
            return javaType;
        }
    }

    @Nullable
    private static Class<?> getCelToJavaMapping(CelType celType) {
        return CEL_TO_JAVA_TYPE_MAP.get(celType);
    }

    public static Object[] toJavaValues(Object[] args) {
        if (args.length == 0) {
            return args;
        }
        Object[] javaArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                javaArgs[i] = null;
            } else if (args[i] instanceof CelValue) {
                javaArgs[i] = toJavaValue((CelValue) args[i]);
            } else if (args[i] instanceof List) {
                javaArgs[i] = toJavaValueList((List<?>)args[i]);
            } else {
                javaArgs[i] = args[i];
            }
        }
        return javaArgs;
    }

    @NotNull
    private static List<?> toJavaValueList(@NotNull List<?> celArgs) {
        List<Object> javaValues = new ArrayList<Object>(celArgs.size());
        for (Object celArg : celArgs) {
            if (celArg instanceof CelValue) {
                javaValues.add(toJavaValue((CelValue) celArg));
            } else {
                javaValues.add(celArg);
            }
        }
        return javaValues;
    }

    @Nullable
    public static Object toJavaValue(@Nullable  CelValue celValue) {
        if (celValue == null) {
            return null;
        }
        if (celValue == NullValue.NULL_VALUE) {
            return null;
        }
        if (celValue instanceof MidPointCelValue<?> mpCelValue) {
            return mpCelValue.getJavaValue();
        } else if (celValue instanceof OpaqueValue) {
            return ((OpaqueValue) celValue).value();
        } else if (celValue instanceof List) {
            return toJavaValueList((List)celValue);
//        } else if (celValue instanceof Map) {
//            return celValue;
        } else {
            throw new IllegalArgumentException("Unknown CEL value "+celValue+" ("+celValue.getClass().getName()+")");
        }
    }

    public static Object toCelValue(Object javaValue) {
        if (javaValue == null) {
            return NullValue.NULL_VALUE;
        }
        if (javaValue instanceof CelValue) {
            return javaValue;
        }
        if (javaValue instanceof PolyString polyString) {
            return PolyStringCelValue.create(polyString);
        }
        if (javaValue instanceof QName qname) {
            return QNameCelValue.create(qname);
        }
        if (javaValue instanceof Item) {
            //noinspection unchecked,rawtypes
            return toCelValue((Item)javaValue);
        }
        return javaValue;
    }

    public static <IV extends PrismValue, ID extends ItemDefinition<?>> Object toListMapValue(Item<IV, ID> item) {
        if (item.getDefinition().isMultiValue()) {
            return MultivalueCelValue.create(item);
        } else {
            // Single-value items
            if (item instanceof PrismProperty<?> property) {
                return toCelValue(property.getRealValue());
            }
            if (item instanceof PrismContainer<?> container) {
                return ContainerValueCelValue.create(container.getValue());
            }
            if (item instanceof PrismReference reference) {
                return ObjectReferenceCelValue.create(reference.getValue());
            }
        }
        // TODO
        return null;
    }


    static <T> Object convertVariableValue(TypedValue<T> typedValue) {
        if (typedValue == null || typedValue.getValue() == null) {
            // CEL has special type and value for null
            return NullValue.NULL_VALUE;
        }
        ItemDefinition def = typedValue.getDefinition();
        if (def == null) {
            return typedValue.getValue();
        }
        if (def instanceof PrismPropertyDefinition<?>) {
            if (QNameUtil.match(((PrismPropertyDefinition<?>)def).getTypeName(), PrismConstants.POLYSTRING_TYPE_QNAME)) {
                Object value = typedValue.getValue();
                if (value == null) {
                    return PolyStringCelValue.create(null);
                }
                if (value instanceof PolyString) {

                    return PolyStringCelValue.create((PolyString) value);
                }
                if (value instanceof PolyStringType) {
                    PolyStringType polystringtype = (PolyStringType) typedValue.getValue();
                    return PolyStringCelValue.create(polystringtype.toPolyString());
                }
            }
        }
        if (def instanceof PrismObjectDefinition<?>) {
            if (typedValue.getValue() instanceof PrismObject<?>) {
                return ObjectCelValue.create((PrismObject<?>) typedValue.getValue());
            } else if (typedValue.getValue() instanceof Objectable) {
                return ObjectCelValue.create(((Objectable) typedValue.getValue()).asPrismObject());
            }
        }
        return typedValue.getValue();
    }

    static {
        try {
            initXsdTypeMap();
            initJavaTypeMap();
        } catch (Exception e) {
            LOGGER.error("Cannot initialize CEL type mapping: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot initialize CEL type mapping: " + e.getMessage(), e);
        }
    }

}
