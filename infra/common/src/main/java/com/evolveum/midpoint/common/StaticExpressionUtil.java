/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Utility class for manipulation of static values in expressions. This is not
 * intended for a general public usage. It is used to set and get pre-computed
 * values in expressions and mappings, e.g. when used between model and provisioning
 * in provisioning scripts.
 *
 * @author Radovan Semancik
 */
public class StaticExpressionUtil {

    /**
     * Returns either Object (if result is supposed to be single-value) or Collection<X> (if result is supposed to be multi-value)
     */
    public static Object getStaticOutput(
            ExpressionType expressionBean, PrismPropertyDefinition<?> outputDefinition,
            String contextDescription, ExpressionReturnMultiplicityType preferredMultiplicity) throws SchemaException {
        PrismProperty<?> output = getPropertyStatic(expressionBean, outputDefinition, contextDescription);
        ExpressionReturnMultiplicityType multiplicity = preferredMultiplicity;
        if (expressionBean.getReturnMultiplicity() != null) {
            multiplicity = expressionBean.getReturnMultiplicity();
        } else if (output != null && output.size() > 1) {
            multiplicity = ExpressionReturnMultiplicityType.MULTI;
        }
        if (output == null) {
            return switch (multiplicity) {
                case MULTI -> new ArrayList<>(0);
                case SINGLE -> null;
            };
        } else {
            Collection<?> realValues = output.getRealValues();
            return switch (multiplicity) {
                case MULTI -> realValues;
                case SINGLE -> MiscUtil.extractSingleton(realValues);
            };
        }
    }

    private static <X> PrismProperty<X> getPropertyStatic(
            ExpressionType expressionBean, PrismPropertyDefinition<?> outputDefinition, String contextDescription)
            throws SchemaException {
        Collection<JAXBElement<?>> expressionEvaluatorElement = expressionBean.getExpressionEvaluator();
        //noinspection unchecked,rawtypes
        return (PrismProperty) parseValueElements(expressionEvaluatorElement, outputDefinition, contextDescription);
    }

    public static ItemDefinition<?> deriveOutputDefinitionFromValueElements(
            QName elementName, Collection<JAXBElement<?>> valueElements, String contextDescription)
            throws SchemaException {
        QName overallType = null;
        for (Object valueElement : valueElements) {
            RawType rawType = getRawType(valueElement, contextDescription);
            QName currentType = rawType.getExplicitTypeName();
            if (currentType != null) {
                QName unified = PrismContext.get().getSchemaRegistry().unifyTypes(overallType, currentType);
                if (unified == null) {
                    throw new SchemaException("Couldn't unify types " + overallType + " and " + currentType + " in " + contextDescription);
                }
                overallType = unified;
            }
        }
        if (overallType == null) {
            overallType = DOMUtil.XSD_STRING;
        }
        int maxOccurs = valueElements.size() > 1 ? -1 : 1;
        return PrismContext.get().getSchemaRegistry().createAdHocDefinition(elementName, overallType, 0, maxOccurs);
    }

    public static <IV extends PrismValue, ID extends ItemDefinition<?>> Item<IV, ID> parseValueElements(
            Collection<?> valueElements, ID outputDefinition, String contextDescription) throws SchemaException {
        Item<IV, ID> output = null;
        for (Object valueElement : valueElements) {
            RawType rawType = getRawType(valueElement, contextDescription);
            Item<IV, ID> elementItem = rawType.getParsedItem(outputDefinition);
            if (output == null) {
                output = elementItem;
            } else {
                output.addAll(elementItem.getClonedValues());
            }
        }
        return output;
    }

    /**
     * Parses value elements without definitions into raw values - this allows further conversion.
     */
    public static List<Object> parseValueElements(
            Collection<?> valueElements, String contextDescription) throws SchemaException {
        List<Object> values = new ArrayList<>();
        for (Object valueElement : valueElements) {
            RawType rawType = getRawType(valueElement, contextDescription);
            Object rawValue = rawType.getParsedRealValue(null, null);
            // This is very crude, but without definition we don't know how to parse the value,
            //  let's just take its value as is and see what conversion does with it.
            //  User has always the option to use the xsi:type for value to avoid RawType.Raw value.
            if (!rawType.isParsed()) {
                rawValue = rawType.getValue();
            }
            values.add(rawValue);
        }
        return values;
    }

    private static RawType getRawType(Object valueElement, String contextDescription) throws SchemaException {
        if (!(valueElement instanceof JAXBElement<?> jaxbElement)) {
            throw new SchemaException("Literal expression cannot handle element " + valueElement + " "
                    + valueElement.getClass().getName() + " in " + contextDescription);
        }
        QName valueElementName = JAXBUtil.getElementQName(valueElement);
        if (!valueElementName.equals(SchemaConstants.C_VALUE)) {
            throw new SchemaException("Literal expression cannot handle element <" + valueElementName + "> in " + contextDescription);
        }
        // not checking declaredType because it may be Object.class instead ... but actual type must be of RawType
        if (jaxbElement.getValue() != null && !(jaxbElement.getValue() instanceof RawType)) {
            throw new SchemaException("Literal expression cannot handle JAXBElement value type "
                    + jaxbElement.getValue().getClass() + " in " + contextDescription);
        }
        return (RawType) jaxbElement.getValue();
    }

    public static <IV extends PrismValue, ID extends ItemDefinition<?>> List<JAXBElement<RawType>> serializeValueElements(Item<IV, ID> item) throws SchemaException {
        if (item == null) {
            return null;
        }
        List<JAXBElement<RawType>> elements = new ArrayList<>(item.size());
        for (PrismValue value : item.getValues()) {
            RootXNode xnode = PrismContext.get().xnodeSerializer().serialize(value);
            RawType rawType = new RawType(xnode.getSubnode().frozen());
            JAXBElement<RawType> jaxbElement = new JAXBElement<>(SchemaConstants.C_VALUE, RawType.class, rawType);
            elements.add(jaxbElement);
        }
        return elements;
    }
}
