/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import dev.cel.common.types.CelType;
import dev.cel.common.types.CelTypes;
import dev.cel.common.types.OpaqueType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.CelValue;
import dev.cel.common.values.OpaqueValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains mapping of XSD types (qnames) and CEL types
 *
 * @author Radovan Semancik
 */
public class CelTypeMapper {

    private static final Map<CelType, QName> CEL_TO_XSD_TYPE_MAP = new HashMap<>();
    private static final Map<QName, CelType> XSD_TO_CEL_TYPE_MAP = new HashMap<>();

    public static final String POLYSTRING_PACKAGE_NAME = PolyString.class.getTypeName();
    public static final CelType POLYSTRING_TYPE =
            OpaqueType.create(POLYSTRING_PACKAGE_NAME);

    private static final Trace LOGGER = TraceManager.getTrace(CelTypeMapper.class);

    private static void initTypeMap() {
        addMapping(SimpleType.STRING, DOMUtil.XSD_STRING, true);
        addMapping(SimpleType.INT, DOMUtil.XSD_INT, true);
        addMapping(SimpleType.INT, DOMUtil.XSD_INTEGER, false);
        addMapping(SimpleType.DOUBLE, DOMUtil.XSD_DECIMAL, false);
        addMapping(SimpleType.DOUBLE, DOMUtil.XSD_DOUBLE, true);
        addMapping(SimpleType.DOUBLE, DOMUtil.XSD_FLOAT, false);
        addMapping(SimpleType.INT, DOMUtil.XSD_LONG, false);
        addMapping(SimpleType.INT, DOMUtil.XSD_SHORT, false);
        addMapping(SimpleType.INT, DOMUtil.XSD_BYTE, false);
        addMapping(SimpleType.BOOL, DOMUtil.XSD_BOOLEAN, true);
        addMapping(SimpleType.BYTES, DOMUtil.XSD_BASE64BINARY, true);
        addMapping(SimpleType.TIMESTAMP, DOMUtil.XSD_DATETIME, true);
        addMapping(SimpleType.DURATION, DOMUtil.XSD_DURATION, true);

//        addMapping(ItemPathType.class, ItemPathType.COMPLEX_TYPE, true);
//        addMapping(UniformItemPath.class, ItemPathType.COMPLEX_TYPE, false);
//        addMapping(ItemPath.class, ItemPathType.COMPLEX_TYPE, false);
//        addMapping(QName.class, DOMUtil.XSD_QNAME, true);

        addMapping(POLYSTRING_TYPE, PrismConstants.POLYSTRING_TYPE_QNAME, true);

//        addXsdToCelMapping(DOMUtil.XSD_ANYURI, String.class);
    }

    private static void addMapping(CelType celType, QName xsdType, boolean bidirectional) {
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

    @Nullable
    public static CelType getCelType(@NotNull QName xsdType) {
        return XSD_TO_CEL_TYPE_MAP.get(xsdType);
    }


    static <T> Object convertVariableValue(TypedValue<T> typedValue) {
        ItemDefinition def = typedValue.getDefinition();
        if (def == null) {
            return typedValue.getValue();
        }
        if (def instanceof PrismPropertyDefinition<?>) {
            if (QNameUtil.match(((PrismPropertyDefinition<?>)def).getTypeName(), PrismConstants.POLYSTRING_TYPE_QNAME)) {
                Object value = typedValue.getValue();
                if (value == null) {
                    return createPolystringCelValue(null);
                }
                if (value instanceof PolyString) {

                    return createPolystringCelValue((PolyString) value);
                }
                if (value instanceof PolyStringType) {
                    PolyStringType polystringtype = (PolyStringType) typedValue.getValue();
                    return createPolystringCelValue(polystringtype.toPolyString());
                }
            }
        }
        return typedValue.getValue();
    }

    private static CelValue createPolystringCelValue(PolyString polystring) {
        return OpaqueValue.create(CelTypeMapper.POLYSTRING_PACKAGE_NAME, polystring);
    }

    public static boolean stringEqualsOpaque(String s, OpaqueValue opaqueValue) {
        if (s == null && opaqueValue == null) {
            return true;
        }
        if (s == null || opaqueValue == null) {
            return false;
        }
        Object value = opaqueValue.value();
        if (value == null) {
            return false;
        }
        if (value instanceof PolyString) {
            return s.equals(((PolyString)value).getOrig());
        } else {
            return s.equals(value);
        }
    }

    public static boolean opaqueEqualsString(OpaqueValue opaqueValue, String s) {
        return stringEqualsOpaque(s,opaqueValue);
    }

    public static String funcPolystringOrig(OpaqueValue opaqueValue) {
        if (opaqueValue == null || opaqueValue.value() == null) {
            return null;
        }
        return getPolystring(opaqueValue).getOrig();
    }

    public static String funcPolystringNorm(OpaqueValue opaqueValue) {
        if (opaqueValue == null || opaqueValue.value() == null) {
            return null;
        }
        return getPolystring(opaqueValue).getNorm();
    }

    private static PolyString getPolystring(OpaqueValue opaqueValue) {
        Object value = opaqueValue.value();
        if (value == null) {
            return null;
        }
        if (value instanceof PolyString) {
            return (PolyString)value;
        } else {
            throw new IllegalArgumentException("Expected PolyString value, but got "+value+" ("+value.getClass()+")");
        }
    }

    static {
        try {
            initTypeMap();
        } catch (Exception e) {
            LOGGER.error("Cannot initialize XSD-CEL type mapping: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot initialize XSD-CEL type mapping: " + e.getMessage(), e);
        }
    }


}
