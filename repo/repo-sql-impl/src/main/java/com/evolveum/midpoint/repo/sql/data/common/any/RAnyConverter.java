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
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author lazyman
 */
public class RAnyConverter {

    private static enum ValueType {
        BOOLEAN, LONG, STRING, DATE, POLY_STRING;
    }

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverter.class);
    private static final Map<QName, ValueType> TYPE_MAP = new HashMap<QName, ValueType>();
    private PrismContext prismContext;

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

    public RAnyConverter(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    //todo assignment parameter really messed up this method, proper interfaces must be introduced later [lazyman]
    public Set<RAnyValue> convertToRValue(Item item, boolean assignment) throws DtoTranslationException {
        Validate.notNull(item, "Object for converting must not be null.");
        Validate.notNull(item.getDefinition(), "Item '" + item.getElementName() + "' without definition can't be saved.");

        Set<RAnyValue> rValues = new HashSet<>();
        try {
            ItemDefinition definition = item.getDefinition();

            RAnyValue rValue = null;
            List<PrismValue> values = item.getValues();
            for (PrismValue value : values) {
                if (value instanceof PrismContainerValue) {
                    continue;
                } else if (value instanceof PrismPropertyValue) {
                    PrismPropertyValue propertyValue = (PrismPropertyValue) value;
                    Object realValue = propertyValue.getValue();
                    if (realValue.getClass().isEnum()) {
                        PrismBeanInspector inspector = new PrismBeanInspector(prismContext);
                        String enumToString = inspector.findEnumFieldValueUncached(realValue.getClass(), realValue.toString());
                        rValue = new ROExtString(enumToString);

                    } else {
                        //todo  omg, do something with this!!! [lazyman]
                        switch (getValueType(definition.getTypeName())) {
                            case BOOLEAN:
                                if (assignment) {
                                    RAExtBoolean booleanValue = new RAExtBoolean();
                                    booleanValue.setValue(extractValue(propertyValue, Boolean.class));
                                    rValue = booleanValue;
                                } else {
                                    ROExtBoolean booleanValue = new ROExtBoolean();
                                    booleanValue.setValue(extractValue(propertyValue, Boolean.class));
                                    rValue = booleanValue;
                                }
                                break;
                            case LONG:
                                if (assignment) {
                                    RAExtLong longValue = new RAExtLong();
                                    longValue.setValue(extractValue(propertyValue, Long.class));
                                    rValue = longValue;
                                } else {
                                    ROExtLong longValue = new ROExtLong();
                                    longValue.setValue(extractValue(propertyValue, Long.class));
                                    rValue = longValue;
                                }
                                break;
                            case DATE:
                                if (assignment) {
                                    RAExtDate dateValue = new RAExtDate();
                                    dateValue.setValue(extractValue(propertyValue, Timestamp.class));
                                    rValue = dateValue;
                                } else {
                                    ROExtDate dateValue = new ROExtDate();
                                    dateValue.setValue(extractValue(propertyValue, Timestamp.class));
                                    rValue = dateValue;
                                }
                                break;
                            case POLY_STRING:
                                if (assignment) {
                                    rValue = new RAExtPolyString(extractValue(propertyValue, PolyString.class));
                                } else {
                                    rValue = new ROExtPolyString(extractValue(propertyValue, PolyString.class));
                                }
                                break;
                            case STRING:
                            default:
                                if (isIndexable(definition)) {
                                    if (assignment) {
                                        RAExtString strValue = new RAExtString();
                                        strValue.setValue(extractValue(propertyValue, String.class));
                                        rValue = strValue;
                                    } else {
                                        ROExtString strValue = new ROExtString();
                                        strValue.setValue(extractValue(propertyValue, String.class));
                                        rValue = strValue;
                                    }
                                } else {
                                    continue;
                                }
                        }
                    }
                } else if (value instanceof PrismReferenceValue) {
                    if (assignment) {
                        PrismReferenceValue referenceValue = (PrismReferenceValue) value;
                        rValue = RAExtReference.createReference(referenceValue);
                    } else {
                        PrismReferenceValue referenceValue = (PrismReferenceValue) value;
                        rValue = ROExtReference.createReference(referenceValue);
                    }
                }

                rValue.setName(RUtil.qnameToString(definition.getName()));
                rValue.setType(RUtil.qnameToString(definition.getTypeName()));
                rValue.setValueType(getValueType(value.getParent()));
                rValue.setDynamic(definition.isDynamic());

                rValues.add(rValue);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException("Exception when translating " + item + ": " + ex.getMessage(), ex);
        }

        return rValues;
    }

    private static boolean isIndexable(ItemDefinition definition) {
        if (definition instanceof PrismContainerDefinition) {
            return false;
        }
        if (!(definition instanceof PrismPropertyDefinition)) {
            throw new UnsupportedOperationException("Unknown definition type '"
                    + definition + "', can't say if it's indexed or not.");
        }

        PrismPropertyDefinition pDefinition = (PrismPropertyDefinition) definition;
        if (pDefinition.isIndexed() != null) {
            return pDefinition.isIndexed();
        }

        QName type = definition.getTypeName();
        return isIndexable(type);
    }

    private static boolean isIndexable(QName type) {
        return DOMUtil.XSD_DATETIME.equals(type)
                || DOMUtil.XSD_INT.equals(type)
                || DOMUtil.XSD_LONG.equals(type)
                || DOMUtil.XSD_SHORT.equals(type)
                || DOMUtil.XSD_INTEGER.equals(type)
                || DOMUtil.XSD_DOUBLE.equals(type)
                || DOMUtil.XSD_FLOAT.equals(type)
                || DOMUtil.XSD_STRING.equals(type)
                || DOMUtil.XSD_DECIMAL.equals(type)
                || DOMUtil.XSD_BOOLEAN.equals(type);
    }

    private RValueType getValueType(Itemable itemable) {
        Validate.notNull(itemable, "Value parent must not be null.");
        if (!(itemable instanceof Item)) {
            throw new IllegalArgumentException("Item type '" + itemable.getClass() + "' not supported in 'any' now.");
        }

        return RValueType.getTypeFromItemClass(((Item) itemable).getClass());
    }

    private <T> T extractValue(PrismPropertyValue value, Class<T> returnType) throws SchemaException {
        ItemDefinition definition = value.getParent().getDefinition();
        //todo raw types

        Object object = value.getValue();
        if (object instanceof Element) {
            object = getRealRepoValue(definition, (Element) object);
        } else {
            object = getAggregatedRepoObject(object);
        }

        if (returnType.isAssignableFrom(object.getClass())) {
            return (T) object;
        }

        throw new IllegalStateException("Can't extract value for saving from prism property value\n" + value);
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

    public void convertFromRValue(RAnyValue value, PrismContainerValue any) throws DtoTranslationException {
        Validate.notNull(value, "Value for converting must not be null.");
        Validate.notNull(any, "Parent prism container value must not be null.");

        try {
            Item<?,?> item = any.findOrCreateItem(RUtil.stringToQName(value.getName()), value.getValueType().getItemClass());
            if (item == null) {
                throw new DtoTranslationException("Couldn't create item for value '" + value.getName() + "'.");
            }

            addValueToItem(value, item);
        } catch (Exception ex) {
            if (ex instanceof DtoTranslationException) {
                throw (DtoTranslationException) ex;
            }
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    private void addValueToItem(RAnyValue value, Item item) throws SchemaException {
        Object realValue = createRealValue(value, item.getDefinition().getTypeName());
        if (!(value instanceof ROExtReference) && realValue == null) {
            throw new SchemaException("Real value must not be null. Some error occurred when adding value "
                    + value + " to item " + item);
        }
        switch (value.getValueType()) {
            case REFERENCE:
                PrismReferenceValue referenceValue = ROExtReference.createReference((ROExtReference) value);
                item.add(referenceValue);
                break;
            case PROPERTY:
                PrismPropertyValue propertyValue = new PrismPropertyValue(realValue, null, null);
                item.add(propertyValue);
                break;
            case OBJECT:
            case CONTAINER:
                //todo implement
                throw new UnsupportedOperationException("Not implemented yet.");
        }
    }

    /**
     * Method restores aggregated object type to its real type, e.g. number 123.1 is type of double, but was
     * saved as string. This method takes RAnyValue instance and creates 123.1 double from string based on
     * provided definition.
     *
     * @param rValue
     * @return
     * @throws SchemaException
     */
    private Object createRealValue(RAnyValue rValue, QName type) throws SchemaException {
        if (rValue instanceof ROExtReference || rValue instanceof RAExtReference) {
            //this is special case, reference doesn't have value, it only has a few properties (oid, filter, etc.)
            return null;
        }

        Object value = rValue.getValue();

        if (rValue instanceof ROExtDate || rValue instanceof RAExtDate) {
            if (value instanceof Date) {
                return XMLGregorianCalendarType.asXMLGregorianCalendar((Date) value);
            }
        } else if (rValue instanceof ROExtLong || rValue instanceof RAExtLong) {
            if (DOMUtil.XSD_LONG.equals(type)) {
                return value;
            } else if (DOMUtil.XSD_INT.equals(type)) {
                return ((Long) value).intValue();
            } else if (DOMUtil.XSD_SHORT.equals(type)) {
                return ((Long) value).shortValue();
            }
        } else if (rValue instanceof ROExtString || rValue instanceof RAExtString) {
            if (DOMUtil.XSD_STRING.equals(type)) {
                return value;
            } else if (DOMUtil.XSD_DOUBLE.equals(type)) {
                return Double.parseDouble((String) value);
            } else if (DOMUtil.XSD_FLOAT.equals(type)) {
                return Float.parseFloat((String) value);
            } else if (DOMUtil.XSD_INTEGER.equals(type)) {
                return new BigInteger((String) value);
            } else if (DOMUtil.XSD_DECIMAL.equals(type)) {
                return new BigDecimal((String) value);
            }
        } else if (rValue instanceof ROExtPolyString) {
            ROExtPolyString poly = (ROExtPolyString) rValue;
            return new PolyString(poly.getValue(), poly.getNorm());
        } else if (rValue instanceof RAExtPolyString) {
            RAExtPolyString poly = (RAExtPolyString) rValue;
            return new PolyString(poly.getValue(), poly.getNorm());
        } else if (rValue instanceof RAExtBoolean || rValue instanceof ROExtBoolean) {
            return rValue.getValue();
        }

        LOGGER.trace("Couldn't create real value of type '{}' from '{}'",
                new Object[]{type, rValue.getValue()});

        throw new IllegalStateException("Can't create real value of type '" + type
                + "' from value saved in DB as '" + rValue.getClass().getSimpleName() + "'.");
    }

    /**
     * This method provides extension type (in real it's table) string for definition and value
     * defined as parameters.
     *
     * @param definition
     * @return One of "strings", "longs", "dates", "clobs"
     * @throws SchemaException
     */
    public static <T extends ObjectType> String getAnySetType(ItemDefinition definition) throws
            SchemaException, QueryException {
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
                if (isIndexable(definition)) {
                    return "strings";
                } else {
                    throw new QueryException("Can't query CLOB (non indexed string) value, definition " + definition);
                }
        }
    }


    /**
     * This method provides transformation of {@link Element} value to its object form, e.g. <value>1</value> to
     * {@link Integer} number 1. It's based on element definition from schema registry or xsi:type attribute
     * in that element.
     *
     * @param definition
     * @param value
     * @return
     */
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
            object = ((Float) object).toString();
        } else if (object instanceof Double) {
            object = ((Double) object).toString();
        } else if (object instanceof BigInteger) {
            object = ((BigInteger) object).toString();
        } else if (object instanceof BigDecimal) {
            object = ((BigDecimal) object).toString();
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

        return object;
    }

	private static String getEnumStringValue(Enum<?> realValue) {
		return PrismBeanInspector.findEnumFieldValueUncached(realValue.getClass(), realValue.toString());
	}

}
