/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.mel;

import com.evolveum.midpoint.model.common.expression.script.mel.value.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.binding.TypeSafeEnum;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import dev.cel.common.types.*;
import dev.cel.common.values.CelValue;
import dev.cel.common.values.NullValue;
import dev.cel.common.values.OpaqueValue;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.*;

/**
 * Maintains mapping of XSD types (qnames) and Java types to CEL types
 *
 * @author Radovan Semancik
 */
public class CelTypeMapper implements CelTypeProvider  {

    public static final CelType PROTECTED_STRING_CEL_TYPE = OpaqueType.create(ProtectedStringType.class.getName());

    private static final Map<CelType, QName> CEL_TO_XSD_TYPE_MAP = new HashMap<>();
    private static final Map<QName, CelType> XSD_TO_CEL_TYPE_MAP = new HashMap<>();
    private static final Map<CelType, Class<?>> CEL_TO_JAVA_TYPE_MAP = new HashMap<>();
    private static final Map<Class<?>, CelType> JAVA_TO_CEL_TYPE_MAP = new HashMap<>();

    private static final Trace LOGGER = TraceManager.getTrace(CelTypeMapper.class);

    private final PrismContext prismContext;
    private final ImmutableList<CelType> types;

    public CelTypeMapper(PrismContext prismContext) {
        this.prismContext = prismContext;
        types = ImmutableList.of(
                PolyStringCelValue.CEL_TYPE,
                ReferenceCelValue.CEL_TYPE,
                ObjectCelValue.CEL_TYPE,
                ContainerValueCelValue.CEL_TYPE,
                QNameCelValue.CEL_TYPE,
                PROTECTED_STRING_CEL_TYPE
        );
    }

    @Override
    public ImmutableCollection<CelType> types() {
        return types;
    }

    @Override
    public Optional<CelType> findType(String typeName) {
        return types.stream().filter(type -> type.name().equals(typeName)).findAny();
    }

    private static void initXsdTypeMap() {
        addXsdMapping(SimpleType.STRING, DOMUtil.XSD_STRING, true);
        addXsdMapping(SimpleType.STRING, DOMUtil.XSD_ANYURI, false);
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
        addXsdMapping(SimpleType.DYN, DOMUtil.XSD_ANYTYPE, true);

//        addMapping(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);
//        addMapping(UniformItemPath.class, ItemPathType.COMPLEX_TYPE, false);
//        addMapping(ItemPath.class, ItemPathType.COMPLEX_TYPE, false);

        addXsdMapping(QNameCelValue.CEL_TYPE, DOMUtil.XSD_QNAME, true);
        addXsdMapping(PolyStringCelValue.CEL_TYPE, PrismConstants.POLYSTRING_TYPE_QNAME, true);
        addXsdMapping(PROTECTED_STRING_CEL_TYPE, ProtectedStringType.COMPLEX_TYPE, true);

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

        addJavaMapping(ContainerValueCelValue.CEL_TYPE, Containerable.class, false);
        addJavaMapping(ContainerValueCelValue.CEL_TYPE, PrismContainerValue.class, true);
        addJavaMapping(ObjectCelValue.CEL_TYPE, Objectable.class, true);

        addJavaMapping(PolyStringCelValue.CEL_TYPE, PolyString.class, true);
        addJavaMapping(PolyStringCelValue.CEL_TYPE, PolyStringType.class, false);
        addJavaMapping(QNameCelValue.CEL_TYPE, QName.class, true);
        addJavaMapping(PROTECTED_STRING_CEL_TYPE, ProtectedStringType.class, false);

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
    public static CelType toCelType(@NotNull ItemDefinition<?> def) {
        if (def instanceof PrismPropertyDefinition<?> propDef) {
            if (propDef.isEnum()) {
                // Special treatment. If we would try to find mapping for this type by QName, nothing could be found.
                // We just translate all enums to strings.
                return SimpleType.STRING;
            }
            return CelTypeMapper.toCelType(propDef.getTypeName());
        } else if (def instanceof PrismObjectDefinition<?>) {
            return ObjectCelValue.CEL_TYPE;
        } else if (def instanceof PrismContainerDefinition<?>) {
            return ContainerValueCelValue.CEL_TYPE;
        } else if (def instanceof PrismReferenceDefinition) {
            return ReferenceCelValue.CEL_TYPE;
        }
        throw new NotImplementedException("Cannot convert "+def+" to CEL");
    }

    @NotNull
    public static CelType toCelType(@NotNull TypedValue<?> typedValue) {
        ItemDefinition<?> def = typedValue.getDefinition();
        if (def == null) {
            Class<?> typeClass = typedValue.getTypeClass();
            if (typeClass == null) {
                throw new IllegalStateException("Typed value " + typedValue + " does not have neither definition nor class");
            }
            return Objects.requireNonNullElse(getCelType(typeClass), SimpleType.DYN);
//            throw new NotImplementedException("Cannot convert class "+typeClass.getSimpleName()+" to CEL");
            // TODO: convert based on class
        } else {
            return toCelType(def);
        }
    }

    @NotNull
    public static CelType toCelNullableType(@NotNull TypedValue<?> typedValue) {
        CelType celType = toCelType(typedValue);
        if (celType instanceof NullableType) {
            return celType;
        } else {
            return NullableType.create(celType);
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
            } else if (args[i] instanceof List) {
                javaArgs[i] = toJavaValueList((List<?>)args[i]);
            } else {
                javaArgs[i] = toJavaValue(args[i]);
            }
        }
        return javaArgs;
    }

    public static List<?> toJavaValues(Collection<?> args) {
        if (args.isEmpty()) {
            return ImmutableList.of();
        }
        List<Object> javaArgs = new ArrayList<>(args.size());
        for (Object next : args) {
            Object nextJava;
            if (next == null) {
                nextJava = null;
            } else if (next instanceof List) {
                nextJava = toJavaValueList((List<?>)next);
            } else {
                nextJava = toJavaValue(next);
            }
            javaArgs.add(nextJava);
        }
        return javaArgs;
    }

    public static <K> Map<K,Object> toJavaValueMap(Map<K,Object> celArgs) {
        Map<K, Object> javaArgs = new HashMap<>();
        for (var entry : celArgs.entrySet()) {
            javaArgs.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return javaArgs;
    }

    @NotNull
    private static List<?> toJavaValueList(@NotNull List<?> celArgs) {
        List<Object> javaValues = new ArrayList<Object>(celArgs.size());
        for (Object celArg : celArgs) {
            javaValues.add(toJavaValue(celArg));
        }
        return javaValues;
    }

    @Nullable
    public static Object toJavaValue(@Nullable Object celValue) {
        if (isCellNull(celValue)) {
            return null;
        }
        while (celValue instanceof Optional<?> optional) {
            if (optional.isEmpty()) {
                return null;
            } else {
                celValue = optional.get();
            }
        }
        if (celValue instanceof Collection<?> col) {
            return toJavaValues(col);
        }
        if (celValue instanceof CelValue) {
            return toJavaValue((CelValue) celValue);
        } else if (celValue instanceof Instant i) {
            return toXmlGregorianCalendar(i);
        } else if (celValue instanceof com.google.protobuf.Duration gDurantion) {
            return toXmlDuration(gDurantion);
        } else {
            return celValue;
        }
    }

    @Nullable
    public static Object toJavaValue(@Nullable CelValue celValue) {
        if (isCellNull(celValue)) {
            return null;
        }
        if (celValue instanceof MidPointValueProducer<?> mpCelValue) {
            return mpCelValue.getJavaValue();
        } else if (celValue instanceof OpaqueValue) {
            return celValue.value();
        } else if (celValue instanceof List) {
            return toJavaValueList((List)celValue);
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
        if (javaValue instanceof XMLGregorianCalendar xmlCal) {
            return toInstant(xmlCal);
        }
        if (javaValue instanceof Duration xmlDuration) {
            return toGoogleDuration(xmlDuration);
        }
        if (javaValue instanceof PrismObject<?> o) {
            return ObjectCelValue.create(o);
        }
        if (javaValue instanceof PrismContainerValue<?> cval) {
            return ContainerValueCelValue.create(cval);
        }
        if (javaValue instanceof PrismReferenceValue rval) {
            return ReferenceCelValue.create(rval);
        }
        return javaValue;
    }

    public static boolean isCellNull(@Nullable Object object) {
        return object == null || object instanceof NullValue
                || object instanceof com.google.protobuf.NullValue
                || (object instanceof Optional<?> opt && opt.isEmpty());
    }

    public static <IV extends PrismValue, ID extends ItemDefinition<?>> Object toListMapValue(Item<IV, ID> item) {
        if (item == null) {
            return null;
        }
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
                return ReferenceCelValue.create(reference.getValue());
            }
        }
        return null;
    }


    static <T> Object convertVariableValue(TypedValue<T> typedValue) {
        if (typedValue == null) {
            // CEL has special type and value for null
            return NullValue.NULL_VALUE;
        }
        Object value = typedValue.getValue();
//        if (value == null) {
//            // CEL has special type and value for null
//            return NullValue.NULL_VALUE;
//        }
        ItemDefinition def = typedValue.getDefinition();
        if (def == null) {
            if (typedValue.getTypeClass() != null && Objectable.class.isAssignableFrom(typedValue.getTypeClass())) {
                // Some (legacy) code is using typeClass instead of definition.
                // E.g. this happens when resolving references (see TestMelExpressions.testExpressionObjectRefVariablesNonExistingObject()
                if (value == null) {
                    return Optional.empty();
                }
                if (value instanceof PrismObject<?> o) {
                    return ObjectCelValue.create(o);
                } else if (value instanceof Objectable o) {
                    return ObjectCelValue.create((PrismObject<?>)o.asPrismObject());
                }
            }
            return CelTypeMapper.toCelValue(value);
        }
        if (def instanceof PrismPropertyDefinition<?> propDef) {
            if (value == null) {
                return NullValue.NULL_VALUE;
            }
            if (propDef.isEnum()) {
                if (value instanceof TypeSafeEnum tse) {
                    return tse.value();
                } else {
                    return value.toString();
                }
            }
            if (QNameUtil.match(((PrismPropertyDefinition<?>)def).getTypeName(), PrismConstants.POLYSTRING_TYPE_QNAME)) {
                if (value instanceof PolyString pval) {
                    return PolyStringCelValue.create(pval);
                }
                if (value instanceof PolyStringType) {
                    PolyStringType polystringtype = (PolyStringType) typedValue.getValue();
                    return PolyStringCelValue.create(polystringtype.toPolyString());
                }
            }
        }
        if (def instanceof PrismObjectDefinition<?>) {
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof PrismObject<?> o) {
                return ObjectCelValue.create(o);
            } else if (value instanceof Objectable o) {
                return ObjectCelValue.create((PrismObject<?>)o.asPrismObject());
            }
        }
        if (def instanceof PrismContainerDefinition<?>) {
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof PrismContainerValue<?> cval) {
                return ContainerValueCelValue.create(cval);
            } else if (value instanceof Containerable c) {
                return ContainerValueCelValue.create((PrismContainerValue<?>)c.asPrismContainerValue());
            }
        }
        if (def instanceof PrismReferenceDefinition) {
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof PrismReferenceValue rval) {
                return ReferenceCelValue.create(rval);
            } else if (value instanceof Referencable r) {
                return ReferenceCelValue.create(r.asReferenceValue());
            }
        }
        return CelTypeMapper.toCelValue(value);
    }

    public static long toMillis(@NotNull Instant instant) {
        return instant.toEpochMilli();
    }

    public static com.google.protobuf.Duration toGoogleDuration(Duration xmlDuration) {
        long totalMillis = xmlDuration.getTimeInMillis(new GregorianCalendar(1970, Calendar.JANUARY, 1));
        return com.google.protobuf.Duration.newBuilder()
                .setSeconds(totalMillis / 1000)
                .setNanos((int) (totalMillis % 1000) * 1000000)
                .build();
    }

    public static Instant toInstant(XMLGregorianCalendar xmlCal) {
        return xmlCal.toGregorianCalendar().toInstant();
    }

    public static @NotNull Duration toXmlDuration(@NotNull com.google.protobuf.Duration gDurantion) {
        long millis = (gDurantion.getSeconds() * 1000) + (gDurantion.getNanos() / 1_000_000);
        return XmlTypeConverter.createDuration(millis);
    }

    public static @NotNull XMLGregorianCalendar toXmlGregorianCalendar(@NotNull Instant instant) {
        // Protobuf/CEL timestamps are always rooted in UTC ("Google epoch").
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(instant.atZone(java.time.ZoneId.of("UTC")));
        return XmlTypeConverter.createXMLGregorianCalendar(gregorianCalendar);
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
