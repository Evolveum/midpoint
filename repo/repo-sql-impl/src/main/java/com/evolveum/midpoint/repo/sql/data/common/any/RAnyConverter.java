/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.xml.bind.annotation.XmlEnumValue;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author lazyman
 */
public class RAnyConverter {

    public enum ValueType {

        REFERENCE(ObjectReferenceType.class, ROExtReference.class, RAExtReference.class),

        BOOLEAN(Boolean.class, ROExtBoolean.class, RAExtBoolean.class),

        LONG(Long.class, ROExtLong.class, RAExtLong.class),

        STRING(String.class, ROExtString.class, RAExtString.class),

        DATE(Timestamp.class, ROExtDate.class, RAExtDate.class),

        POLY_STRING(PolyString.class, ROExtPolyString.class, RAExtPolyString.class);

        private final Class<?> valueType;
        private final Class<? extends ROExtValue<?>> oExtType;
        private final Class<? extends RAExtValue<?>> aExtType;

        ValueType(Class<?> valueType, Class<? extends ROExtValue<?>> oExtType, Class<? extends RAExtValue<?>> aExtType) {
            this.valueType = valueType;
            this.oExtType = oExtType;
            this.aExtType = aExtType;
        }

        public Class<?> getValueType() {
            return valueType;
        }

        @NotNull
        public RAExtValue createNewAExtValue(Object value) {
            try {
                Constructor<? extends RAExtValue> constr = aExtType.getConstructor(value.getClass());
                return constr.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                throw new SystemException(ex);
            }
        }

        @NotNull
        public ROExtValue createNewOExtValue(Object value) {
            try {
                Constructor<? extends ROExtValue> constr = oExtType.getConstructor(value.getClass());
                return constr.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                throw new SystemException(ex);
            }
        }
    }

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverter.class);
    private static final Map<QName, ValueType> TYPE_MAP = new HashMap<>();

    private final PrismContext prismContext;
    private final ExtItemDictionary extItemDictionary;

    static {
        TYPE_MAP.put(DOMUtil.XSD_BOOLEAN, ValueType.BOOLEAN);

        TYPE_MAP.put(DOMUtil.XSD_INT, ValueType.LONG);
        TYPE_MAP.put(DOMUtil.XSD_LONG, ValueType.LONG);
        TYPE_MAP.put(DOMUtil.XSD_SHORT, ValueType.LONG);

        TYPE_MAP.put(DOMUtil.XSD_INTEGER, ValueType.STRING);
        TYPE_MAP.put(DOMUtil.XSD_DECIMAL, ValueType.STRING);
        TYPE_MAP.put(DOMUtil.XSD_STRING, ValueType.STRING);
        TYPE_MAP.put(DOMUtil.XSD_DOUBLE, ValueType.STRING);
        TYPE_MAP.put(DOMUtil.XSD_FLOAT, ValueType.STRING);

        TYPE_MAP.put(DOMUtil.XSD_DATETIME, ValueType.DATE);
        TYPE_MAP.put(PolyStringType.COMPLEX_TYPE, ValueType.POLY_STRING);
    }

    public RAnyConverter(PrismContext prismContext, ExtItemDictionary extItemDictionary) {
        this.prismContext = prismContext;
        this.extItemDictionary = extItemDictionary;
    }

    @NotNull
    private RAnyValue extractAndCreateValue(PrismPropertyValue propertyValue, boolean assignment, ValueType type)
            throws SchemaException {

        Object extractedValue = extractValue(propertyValue, type.getValueType());
        return assignment ? type.createNewAExtValue(extractedValue) : type.createNewOExtValue(extractedValue);
    }

    @NotNull
    public RAnyValue<?> convertToRValue(PrismValue value, boolean isAssignment, Integer itemId) throws SchemaException {
        RAnyValue<?> rValue;

        ItemDefinition definition = value.getParent().getDefinition();

        if (value instanceof PrismPropertyValue) {
            PrismPropertyValue propertyValue = (PrismPropertyValue) value;
            rValue = extractAndCreateValue(propertyValue, isAssignment, getValueType(definition.getTypeName()));
        } else if (value instanceof PrismReferenceValue) {
            if (isAssignment) {
                PrismReferenceValue referenceValue = (PrismReferenceValue) value;
                rValue = RAExtReference.createReference(referenceValue);
            } else {
                PrismReferenceValue referenceValue = (PrismReferenceValue) value;
                rValue = ROExtReference.createReference(referenceValue);
            }
        } else {
            // shouldn't get here because if isIndexed test above
            throw new AssertionError("Wrong value type: " + value);
        }

        rValue.setItemId(itemId);

        return rValue;
    }

    //todo assignment parameter really messed up this method, proper interfaces must be introduced later [lazyman]
    public Set<RAnyValue<?>> convertToRValue(Item<?, ?> item, boolean assignment,
            RObjectExtensionType ownerType) throws SchemaException, DtoTranslationException {
        Objects.requireNonNull(item, "Object for converting must not be null.");
        Objects.requireNonNull(item.getDefinition(), "Item '" + item.getElementName() + "' without definition can't be saved.");

        ItemDefinition definition = item.getDefinition();
        Set<RAnyValue<?>> rValues = new HashSet<>();
        if (!isIndexed(definition, item.getElementName(), areDynamicsOfThisKindIndexed(ownerType), prismContext)) {
            return rValues;
        }

        LOGGER.trace("Converting any values of item {}; definition: {}", item, definition);

        Integer itemId = extItemDictionary.createOrFindItemDefinition(definition).getId();

        try {
            for (PrismValue value : item.getValues()) {
                RAnyValue rValue = convertToRValue(value, assignment, itemId);
                rValues.add(rValue);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException("Exception when translating " + item + ": " + ex.getMessage(), ex);
        }

        return rValues;
    }

    private static String getEnumStringValue(Enum<?> realValue) {
        return findEnumFieldValueUncached(realValue.getClass(), realValue.toString());
    }

    // copied from PrismBeanInspector (maybe reconcile somehow later)
    private static String findEnumFieldValueUncached(Class classType, String toStringValue) {
        for (Field field : classType.getDeclaredFields()) {
            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
            if (xmlEnumValue != null && field.getName().equals(toStringValue)) {
                return xmlEnumValue.value();
            }
        }
        return null;
    }

    /**
     * @return null if item is not indexed
     */
    public static ValueType getValueType(ItemDefinition<?> definition,
            ItemName itemName, boolean indexAlsoDynamics, PrismContext prismContext)
            throws SchemaException {
        if (definition == null) {
            return null;
        }
        if (definition instanceof PrismContainerDefinition) {
            return null;
        } else if (definition instanceof PrismReferenceDefinition) {
            // TODO make reference indexing configurable
            return ValueType.REFERENCE;
        } else if (!(definition instanceof PrismPropertyDefinition)) {
            throw new UnsupportedOperationException("Unknown definition type '"
                    + definition + "', can't say if '" + itemName + "' is indexed or not.");
        }

        PrismPropertyDefinition<?> pDefinition = (PrismPropertyDefinition<?>) definition;
        Boolean isIndexed = pDefinition.isIndexed();
        if (BooleanUtils.isFalse(isIndexed)) {
            return null;
        } else if (isIndexed == null && definition.isDynamic() && !indexAlsoDynamics) {
            return null;
        }

        IndexableStatus status = getIndexableStatus(pDefinition, prismContext);
        if (isIndexed == null) {
            return status.indexedByDefault ? status.valueType : null;
        } else {
            assert Boolean.TRUE.equals(isIndexed);
            if (status.valueType != null) {
                return status.valueType;
            } else {
                throw new SchemaException("Item is marked as indexed (definition " + definition
                        + ") but definition type (" + definition.getTypeName() + ") doesn't support indexing");
            }
        }
    }

    private static final class IndexableStatus {
        final boolean indexedByDefault;
        final ValueType valueType;

        private IndexableStatus(boolean indexedByDefault, ValueType valueType) {
            this.indexedByDefault = indexedByDefault;
            this.valueType = valueType;
        }
    }

    private static IndexableStatus getIndexableStatus(PrismPropertyDefinition<?> definition, PrismContext prismContext) {
        QName type = definition.getTypeName();
        ValueType valueType = TYPE_MAP.get(type);
        if (valueType != null) {
            return new IndexableStatus(true, valueType);
        } else {
            Collection<? extends TypeDefinition> typeDefinitions = prismContext.getSchemaRegistry()
                    .findTypeDefinitionsByType(definition.getTypeName());
            if (typeDefinitions.size() != 1) {
                return new IndexableStatus(false, null);        // shouldn't occur
            }
            TypeDefinition typeDef = typeDefinitions.iterator().next();
            if (typeDef instanceof SimpleTypeDefinition) {
                SimpleTypeDefinition simpleTypeDef = (SimpleTypeDefinition) typeDef;
                if (DOMUtil.XSD_STRING.equals(simpleTypeDef.getBaseTypeName())
                        && simpleTypeDef.getDerivationMethod() == SimpleTypeDefinition.DerivationMethod.RESTRICTION) {
                    return new IndexableStatus(true, ValueType.STRING);
                }
            }
            return new IndexableStatus(false, null);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isIndexed(ItemDefinition definition, ItemName elementName,
            boolean indexAlsoDynamics, PrismContext prismContext)
            throws SchemaException {
        return getValueType(definition, elementName, indexAlsoDynamics, prismContext) != null;
    }

    public static boolean areDynamicsOfThisKindIndexed(RObjectExtensionType extensionType) {
        // We assume that if an object/assignment extension item is not configured in XSD, we won't be searching for it.
        // This is to fix MID-4878 with a reasonable effort (as the sync token is currently not defined in any XSD).
        //
        // For shadow attributes we - of course - treat them as indexed.
        return extensionType != RObjectExtensionType.EXTENSION;
    }

    @NotNull
    private <T> T extractValue(PrismPropertyValue value, Class<T> returnType) throws SchemaException {
        ItemDefinition definition = value.getParent().getDefinition();
        //todo raw types

        Object object = value.getValue();
        if (object instanceof Element) {
            object = getRealRepoValue(definition, (Element) object, prismContext);
        } else if (object instanceof RawType) {
            RawType raw = (RawType) object;
            object = raw.getParsedRealValue(returnType);        // todo this can return null!
        } else {
            object = getAggregatedRepoObject(object);
        }

        if (returnType.isAssignableFrom(object.getClass())) {
            //noinspection unchecked
            return (T) object;
        }

        throw new IllegalStateException("Can't extract value for saving from prism property value " + value
                + " expected return type " + returnType + ", actual type " + (object == null ? null : object.getClass()));
    }

    private static ValueType getValueType(QName qname) {
        if (qname == null) {
            return ValueType.STRING;
        }
        ValueType type = TYPE_MAP.get(qname);
        if (type == null) {
            return ValueType.STRING;
        }

        return type;
    }

    /**
     * This method provides extension type (in real it's table) string for definition and value
     * defined as parameters.
     *
     * @return One of "strings", "longs", "dates", "clobs"
     */
    public static String getAnySetType(ItemDefinition definition, PrismContext prismContext) throws
            SchemaException, QueryException {
        if (!isIndexed(definition, definition.getItemName(), true, prismContext)) {
            throw new QueryException("Can't query non-indexed value for '" + definition.getItemName()
                    + "', definition " + definition);
        }
        QName typeName = definition.getTypeName();

        ValueType valueType = getValueType(typeName);
        switch (valueType) {
            case BOOLEAN:
                return "booleans";
            case DATE:
                return "dates";
            case LONG:
                return "longs";
            case STRING:
            default:
                return "strings";
        }
    }

    /**
     * This method provides transformation of {@link Element} value to its object form, e.g. <value>1</value> to
     * {@link Integer} number 1. It's based on element definition from schema registry or xsi:type attribute
     * in that element.
     * <p>
     * Expects only property values (references are handled at other place).
     * <p>
     * [pm] is this method really used? i.e. do we ever try to store PrismPropertyValue<Element>?
     */
    @NotNull
    public static Object getRealRepoValue(ItemDefinition definition, Element value, PrismContext prismContext) throws SchemaException {
        ValueType willBeSavedAs;
        QName typeName;
        if (definition != null) {
            willBeSavedAs = getValueType(definition.getTypeName());
            typeName = definition.getTypeName();
        } else {
            willBeSavedAs = null;
            typeName = DOMUtil.resolveXsiType(value);
        }

        Objects.requireNonNull(typeName, "Definition was not defined for element value '"
                + DOMUtil.getQNameWithoutPrefix(value) + "' and it doesn't have xsi:type.");

        if (willBeSavedAs == ValueType.STRING) {
            if (DOMUtil.listChildElements(value).isEmpty()) {
                return value.getTextContent();                  //simple values
            } else {
                return DOMUtil.serializeDOMToString(value);     //composite elements or containers
            }
        } else {
            Object object = prismContext.parserFor(value).type(typeName).parseRealValue();
            object = getAggregatedRepoObject(object);
            if (object == null) {
                throw new IllegalStateException("Can't extract value for saving from prism property value\n" + value);
            }
            return object;
        }
    }

    /**
     * Method provides aggregation of some java types (only simple types, which are indexed)
     *
     * @return aggregated object
     */
    public static Object getAggregatedRepoObject(Object object) {
        //check float/double to string
        if (object instanceof Float) {
            object = object.toString();
        } else if (object instanceof Double) {
            object = object.toString();
        } else if (object instanceof BigInteger) {
            object = object.toString();
        } else if (object instanceof BigDecimal) {
            object = object.toString();
        }

        //check short/integer to long
        if (object instanceof Short) {
            object = ((Short) object).longValue();
        } else if (object instanceof Integer) {
            object = ((Integer) object).longValue();
        }

        //check gregorian calendar, xmlgregorian calendar to date
        if (object instanceof GregorianCalendar) {
            object = ((GregorianCalendar) object).getTime();
        } else if (object instanceof XMLGregorianCalendar) {
            object = XMLGregorianCalendarType.asDate(((XMLGregorianCalendar) object));
        }

        if (object instanceof Date) {
            object = new Timestamp(((Date) object).getTime());
        }

        //if object instance of boolean, nothing to do

        if (object instanceof Enum<?>) {
            object = getEnumStringValue((Enum<?>) object);
        }

        if (object instanceof byte[]) {
            object = new String((byte[]) object);
        }

        return object;
    }

    // TODO fix this!
    public static Collection<? extends ROExtValue> getExtValues(RObject rObject, RExtItem extItemDef, ItemDefinition<?> itemDef) {
        assert extItemDef.getId() != null;
        return getAllExtValues(rObject, itemDef).stream()
                .filter(value -> extItemDef.getId().equals(value.getItemId()))  // check also extensionType
                .collect(Collectors.toList());
    }

    // TODO fix this! (add support for other types)
    private static Collection<? extends ROExtValue<?>> getAllExtValues(RObject rObject, ItemDefinition<?> itemDef) {
        if (DOMUtil.XSD_STRING.equals(itemDef.getTypeName())) {
            return rObject.getStrings();
        } else {
            return Collections.emptyList();
        }
    }
}
