/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.PrismBeanInspector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author lazyman
 */
public class RAnyConverter {

    private enum ValueType {

        BOOLEAN(Boolean.class, ROExtBoolean.class, RAExtBoolean.class),

        LONG(Long.class, ROExtLong.class, RAExtLong.class),

        STRING(String.class, ROExtString.class, RAExtString.class),

        DATE(Timestamp.class, ROExtDate.class, RAExtDate.class),

        POLY_STRING(PolyString.class, ROExtPolyString.class, RAExtPolyString.class);

        private Class valueType;
        private Class<? extends ROExtValue> oExtType;
        private Class<? extends RAExtValue> aExtType;

        ValueType(Class valueType, Class oExtType, Class aExtType) {
            this.valueType = valueType;
            this.oExtType = oExtType;
            this.aExtType = aExtType;
        }

        public Class getValueType() {
            return valueType;
        }

        public RAExtValue createNewAExtValue(Object value) {
            try {
                Constructor<? extends RAExtValue> constr = aExtType.getConstructor(value.getClass());
                return constr.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                throw new SystemException(ex);
            }
        }

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
    private PrismContext prismContext;
    private ExtItemDictionary extItemDictionary;

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

    private RAnyValue extractAndCreateValue(ItemDefinition def, PrismPropertyValue propertyValue, boolean assignment)
            throws SchemaException {

        ValueType type = getValueType(def.getTypeName());
        Object extractedValue = extractValue(propertyValue, type.getValueType());
        return assignment ? type.createNewAExtValue(extractedValue) : type.createNewOExtValue(extractedValue);
    }

    public RAnyValue convertToRValue(PrismValue value, boolean assignment) throws SchemaException {
        RAnyValue rValue;

        ItemDefinition definition = value.getParent().getDefinition();

        if (!isIndexed(definition, value.getParent().getElementName(), prismContext)) {
            return null;
        }

        if (value instanceof PrismPropertyValue) {
            PrismPropertyValue propertyValue = (PrismPropertyValue) value;

            rValue = extractAndCreateValue(definition, propertyValue, assignment);
        } else if (value instanceof PrismReferenceValue) {
            if (assignment) {
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

        rValue.setItemId(extItemDictionary.createOrFindItemDefinition(definition).getId());

        return rValue;
    }

    //todo assignment parameter really messed up this method, proper interfaces must be introduced later [lazyman]
    public Set<RAnyValue<?>> convertToRValue(Item item, boolean assignment) throws SchemaException, DtoTranslationException {
        Validate.notNull(item, "Object for converting must not be null.");
        Validate.notNull(item.getDefinition(), "Item '" + item.getElementName() + "' without definition can't be saved.");

        ItemDefinition definition = item.getDefinition();
        Set<RAnyValue<?>> rValues = new HashSet<>();
        if (!isIndexed(definition, item.getElementName(), prismContext)) {
            return rValues;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Converting any values of item {}; definition: {}", item, definition);
        }

        try {
            RAnyValue rValue;
            List<PrismValue> values = item.getValues();
            for (PrismValue value : values) {
                rValue = convertToRValue(value, assignment);
                rValues.add(rValue);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException("Exception when translating " + item + ": " + ex.getMessage(), ex);
        }

        return rValues;
    }

    private static String getEnumStringValue(Enum<?> realValue) {
        return PrismBeanInspector.findEnumFieldValueUncached(realValue.getClass(), realValue.toString());
    }

    private static boolean isIndexed(ItemDefinition definition, QName elementName, PrismContext prismContext)
            throws SchemaException {
        if (definition instanceof PrismContainerDefinition) {
            return false;
        }
        if (definition instanceof PrismReferenceDefinition) {
            return true;        // TODO make reference indexing configurable
        }
        if (!(definition instanceof PrismPropertyDefinition)) {
            throw new UnsupportedOperationException("Unknown definition type '"
                    + definition + "', can't say if '" + elementName + "' is indexed or not.");
        }

        PrismPropertyDefinition pDefinition = (PrismPropertyDefinition) definition;
        Boolean isIndexed = pDefinition.isIndexed();
        if (isIndexed != null) {
            if (isIndexed && !isIndexedByDefault(definition, prismContext)) {
                throw new SchemaException("Item is marked as indexed (definition " + definition.debugDump()
                        + ") but definition type (" + definition.getTypeName() + ") doesn't support indexing");
            }

            return isIndexed;
        }

        return isIndexedByDefault(definition, prismContext);
    }

    //todo should be renamed to canBeIndexed
    private static boolean isIndexedByDefault(ItemDefinition definition, PrismContext prismContext) {
        QName type = definition.getTypeName();
        if (DOMUtil.XSD_DATETIME.equals(type)
                || DOMUtil.XSD_INT.equals(type)
                || DOMUtil.XSD_LONG.equals(type)
                || DOMUtil.XSD_SHORT.equals(type)
                || DOMUtil.XSD_INTEGER.equals(type)
                || DOMUtil.XSD_DOUBLE.equals(type)
                || DOMUtil.XSD_FLOAT.equals(type)
                || DOMUtil.XSD_STRING.equals(type)
                || DOMUtil.XSD_DECIMAL.equals(type)
                || DOMUtil.XSD_BOOLEAN.equals(type)
                || PolyStringType.COMPLEX_TYPE.equals(type)) {
            return true;
        }
        Collection<? extends TypeDefinition> typeDefinitions = prismContext.getSchemaRegistry()
                .findTypeDefinitionsByType(definition.getTypeName());
        if (typeDefinitions.isEmpty() || typeDefinitions.size() > 1) {
            return false;        // shouldn't occur
        }
        TypeDefinition typeDef = typeDefinitions.iterator().next();
        if (typeDef instanceof SimpleTypeDefinition) {
            SimpleTypeDefinition simpleTypeDef = (SimpleTypeDefinition) typeDef;
            return DOMUtil.XSD_STRING.equals(simpleTypeDef.getBaseTypeName())
                    && simpleTypeDef.getDerivationMethod() == SimpleTypeDefinition.DerivationMethod.RESTRICTION;
        } else {
            return false;
        }
    }

    @NotNull
    private <T> T extractValue(PrismPropertyValue value, Class<T> returnType) throws SchemaException {
        ItemDefinition definition = value.getParent().getDefinition();
        //todo raw types

        Object object = value.getValue();
        if (object instanceof Element) {
            object = getRealRepoValue(definition, (Element) object);
        } else if (object instanceof RawType) {
            RawType raw = (RawType) object;
            object = raw.getParsedRealValue(returnType);        // todo this can return null!
        } else {
            object = getAggregatedRepoObject(object);
        }

        if (returnType.isAssignableFrom(object.getClass())) {
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
     * @param definition
     * @param prismContext
     * @return One of "strings", "longs", "dates", "clobs"
     * @throws SchemaException
     */
    public static String getAnySetType(ItemDefinition definition, PrismContext prismContext) throws
            SchemaException, QueryException {
        if (!isIndexed(definition, definition.getName(), prismContext)) {
            throw new QueryException("Can't query non-indexed value for '" + definition.getName()
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
     *
     * @param definition
     * @param value
     * @return
     */
    @NotNull
    public static Object getRealRepoValue(ItemDefinition definition, Element value) throws SchemaException {
        ValueType willBeSaveAs = definition == null ? null : getValueType(definition.getTypeName());
        QName typeName = definition == null ? DOMUtil.resolveXsiType(value) : definition.getTypeName();

        Validate.notNull(typeName, "Definition was not defined for element value '"
                + DOMUtil.getQNameWithoutPrefix(value) + "' and it doesn't have xsi:type.");

        Object object;
        if (ValueType.STRING.equals(willBeSaveAs)) {
            if (DOMUtil.listChildElements(value).isEmpty()) {
                //simple values
                return value.getTextContent();
            } else {
                //composite elements or containers
                return DOMUtil.serializeDOMToString(value);
            }
        } else {
            object = XmlTypeConverter.toJavaValue(value, typeName);
        }

        object = getAggregatedRepoObject(object);
        if (object == null) {
            throw new IllegalStateException("Can't extract value for saving from prism property value\n" + value);
        }

        return object;
    }

    /**
     * Method provides aggregation of some java types (only simple types, which are indexed)
     *
     * @param object
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
        } else if (object instanceof BigDecimal)
            object = object.toString();

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
}
