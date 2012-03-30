/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class RAnyConverter {

    private static enum ValueType {
        LONG, STRING, DATE
    }

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverter.class);
    private static final Map<QName, ValueType> TYPE_MAP = new HashMap<QName, ValueType>();
    private PrismContext prismContext;
    private Document document;

    static {
        TYPE_MAP.put(DOMUtil.XSD_INTEGER, ValueType.LONG);
        TYPE_MAP.put(DOMUtil.XSD_LONG, ValueType.LONG);
        TYPE_MAP.put(DOMUtil.XSD_SHORT, ValueType.LONG);

        TYPE_MAP.put(DOMUtil.XSD_STRING, ValueType.STRING);
        TYPE_MAP.put(DOMUtil.XSD_DOUBLE, ValueType.STRING);
        TYPE_MAP.put(DOMUtil.XSD_FLOAT, ValueType.STRING);

        TYPE_MAP.put(DOMUtil.XSD_DATETIME, ValueType.DATE);
    }

    RAnyConverter(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    Set<RValue> convertToRValue(Item item) throws DtoTranslationException {
        Validate.notNull(item, "Object for converting must not be null.");
        Validate.notNull(item.getDefinition(), "Item '" + item.getName() + "' without definition can't be saved.");

        Set<RValue> rValues = new HashSet<RValue>();
        try {
            ItemDefinition definition = item.getDefinition();

            RValue rValue = null;
            List<PrismValue> values = item.getValues();
            for (PrismValue value : values) {
                if (value instanceof PrismContainerValue) {
                    rValue = createClobValue((PrismContainerValue) value);
                } else if (value instanceof PrismPropertyValue) {
                    PrismPropertyValue propertyValue = (PrismPropertyValue) value;
                    switch (getValueType(definition.getTypeName())) {
                        case LONG:
                            RLongValue longValue = new RLongValue();
                            longValue.setValue(extractValue(propertyValue, Long.class));
                            rValue = longValue;
                            break;
                        case DATE:
                            RDateValue dateValue = new RDateValue();
                            dateValue.setValue(extractValue(propertyValue, Date.class));
                            rValue = dateValue;
                            break;
                        case STRING:
                        default:
                            if (isIndexable(definition)) {
                                RStringValue strValue = new RStringValue();
                                strValue.setValue(extractValue(propertyValue, String.class));
                                rValue = strValue;
                            } else {
                                rValue = createClobValue(propertyValue);
                            }
                    }
                }

                rValue.setName(definition.getName());
                rValue.setType(definition.getTypeName());
                rValue.setValueType(getValueType(value.getParent()));

                rValues.add(rValue);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return rValues;
    }

    private static boolean isIndexable(ItemDefinition definition) {
        if (definition instanceof PrismContainerDefinition) {
            return false;
        } else if (definition instanceof PrismReferenceDefinition) {
            return true;
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
                || DOMUtil.XSD_LONG.equals(type)
                || DOMUtil.XSD_SHORT.equals(type)
                || DOMUtil.XSD_INTEGER.equals(type)
                || DOMUtil.XSD_DOUBLE.equals(type)
                || DOMUtil.XSD_FLOAT.equals(type)
                || DOMUtil.XSD_STRING.equals(type);
    }

    private RValueType getValueType(Itemable itemable) {
        Validate.notNull(itemable, "Value parent must not be null.");
        if (!(itemable instanceof Item)) {
            throw new IllegalArgumentException("Item type '" + itemable.getClass() + "' not supported in 'any' now.");
        }

        return RValueType.getTypeFromItemClass(((Item) itemable).getClass());
    }

    private RClobValue createClobValue(PrismContainerValue containerValue) throws SchemaException {
        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        Element root = createElement(RUtil.CUSTOM_OBJECT);
        String value = domProcessor.serializeObjectToString(containerValue, root);

        return new RClobValue(value);

    }

    private RClobValue createClobValue(PrismPropertyValue propertyValue) {
        String value;
        Object object = propertyValue.getValue();
        if (object instanceof Element) {
            Element element = (Element) object;
            value = DOMUtil.serializeDOMToString(element);
        } else {
            value = object.toString();
        }

        return new RClobValue(value);
    }

    private <T> T extractValue(PrismPropertyValue value, Class<T> returnType) throws SchemaException {
        ItemDefinition definition = value.getParent().getDefinition();
        //todo raw types

        Object object = value.getValue();
        if (object instanceof Element) {
            object = getRealRepoValue(definition, (Element) object);
        } else {
            object = updateJavaValueType(object);
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

    void convertFromRValue(RValue value, PrismContainerValue any) throws DtoTranslationException {
        Validate.notNull(value, "Value for converting must not be null.");
        Validate.notNull(any, "Parent prism container value must not be null.");

        try {
            Item item = any.findOrCreateItem(value.getName(), value.getValueType().getItemClass());
            if (item == null) {
                throw new DtoTranslationException("Couldn't create item for value '" + value.getName() + "'.");
            }

            addValueToItem(value, item);
        } catch (DtoTranslationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    private Element createElement(QName name) {
        if (document == null) {
            document = DOMUtil.getDocument();
        }

        return DOMUtil.createElement(document, name);
    }

    private void addValueToItem(RValue value, Item item) throws SchemaException {
        Object realValue = createRealValue(value, item.getDefinition());

        switch (value.getValueType()) {
            case REFERENCE:
                //todo implement
                // PrismReferenceValue referenceValue = new PrismReferenceValue();
                // item.add(referenceValue);
                throw new UnsupportedOperationException("Not implemented yet.");
            case PROPERTY:
                PrismPropertyValue propertyValue = new PrismPropertyValue(realValue, null, null);
                item.add(propertyValue);
                break;
            case OBJECT:
            case CONTAINER:
                //todo implement
                // PrismContainerValue containerValue = new PrismContainerValue();
                // item.add(containerValue);
                throw new UnsupportedOperationException("Not implemented yet.");
        }
    }

    /**
     * Method restores aggregated object type to its real type, e.g. number 123.1 is type of double, but was
     * saved as string. This method takes RValue instance and creates 123.1 double from string based on
     * provided definition.
     *
     * @param rValue
     * @param definition
     * @return
     * @throws SchemaException
     */
    private Object createRealValue(RValue rValue, ItemDefinition definition) throws SchemaException {
        if (rValue instanceof RClobValue) {
            RClobValue clob = (RClobValue) rValue;
            return DOMUtil.parseDocument(clob.getValue()).getDocumentElement();
        }

        Object value = rValue.getValue();
        if (rValue instanceof RDateValue) {
            if (value instanceof Date) {
                return XMLGregorianCalendarType.asXMLGregorianCalendar((Date) value);
            }
        } else if (rValue instanceof RLongValue) {
            if (DOMUtil.XSD_LONG.equals(rValue.getType())) {
                return value;
            } else if (DOMUtil.XSD_INTEGER.equals(rValue.getType())) {
                return ((Long) value).intValue();
            } else if (DOMUtil.XSD_SHORT.equals(rValue.getType())) {
                return ((Long) value).shortValue();
            }
        } else if (rValue instanceof RStringValue) {
            if (DOMUtil.XSD_STRING.equals(rValue.getType())) {
                return value;
            } else if (DOMUtil.XSD_DOUBLE.equals(rValue.getType())) {
                return Double.parseDouble((String) value);
            } else if (DOMUtil.XSD_FLOAT.equals(rValue.getType())) {
                return Float.parseFloat((String) value);
            }
        }

        LOGGER.trace("Couldn't create real value of type '{}' from '{}'",
                new Object[]{rValue.getType(), rValue.getValue()});

        throw new IllegalStateException("Can't create real value of type '" + rValue.getType()
                + "' from value saved in DB as '" + rValue.getClass().getSimpleName() + "'.");
    }

    /**
     * This method provides extension type (in real it's table) string for definition and value
     * defined as parameters. This string represent field in {@link RAnyContainer} where defined
     * extension value is or can be saved.
     *
     * @param definition
     * @param value
     * @param <T>
     * @return One of "strings", "longs", "dates", "clobs"
     * @throws SchemaException
     */
    public static <T extends ObjectType> String getAnySetType(ItemDefinition definition, Element value) throws
            SchemaException {
        QName typeName = definition == null ? DOMUtil.resolveXsiType(value) : definition.getTypeName();
        Validate.notNull(typeName, "Definition was not defined for element value '"
                + DOMUtil.getQNameWithoutPrefix(value) + "' and it doesn't have xsi:type.");

        ValueType valueType = getValueType(typeName);
        switch (valueType) {
            case DATE:
                return "dates";
            case LONG:
                return "longs";
            case STRING:
            default:
                boolean indexed = definition == null ? isIndexable(typeName) : isIndexable(definition);
                if (indexed) {
                    return "strings";
                } else {
                    return "clobs";
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

        object = updateJavaValueType(object);
        if (object == null) {
            throw new IllegalStateException("Can't extract value for saving from prism property value\n" + value);
        }

        return object;
    }

    /**
     * Method provides aggregation of some java types
     *
     * @param object
     * @return aggregated object
     */
    private static Object updateJavaValueType(Object object) {
        //check float/double to string
        if (object instanceof Float) {
            object = ((Float) object).toString();
        } else if (object instanceof Double) {
            object = ((Double) object).toString();
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

        return object;
    }
}
