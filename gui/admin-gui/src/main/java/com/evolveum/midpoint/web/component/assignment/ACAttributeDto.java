/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author lazyman
 */
public class ACAttributeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_VALUES = "values";

    // construction.getRef should point to the same attribute as definition
    private final PrismPropertyDefinition definition;
    private final ResourceAttributeDefinitionType construction;
    private List<ACValueConstructionDto> values;

    private ACAttributeDto(PrismPropertyDefinition definition, ResourceAttributeDefinitionType construction) {
        Validate.notNull(definition, "Prism property definition must not be null.");
        Validate.notNull(construction, "Value construction must not be null.");

        this.definition = definition;
        this.construction = construction;
    }

    public static ACAttributeDto createACAttributeDto(PrismPropertyDefinition definition, ResourceAttributeDefinitionType construction,
            PrismContext context) throws SchemaException {
        ACAttributeDto dto = new ACAttributeDto(definition, construction);
        dto.values = dto.createValues(context);

        return dto;
    }

    public List<ACValueConstructionDto> getValues() {
        if (values.isEmpty()) {
            values.add(new ACValueConstructionDto(this, null));
        }
        return values;
    }

    private List<ACValueConstructionDto> createValues(PrismContext context) throws SchemaException {
        List<ACValueConstructionDto> values = new ArrayList<>();
        MappingType outbound = construction.getOutbound();
        if (outbound == null || outbound.getExpression() == null) {
            return values;
        }
        ExpressionType expression = outbound.getExpression();
        if (expression.getExpressionEvaluator() != null) {
            List<JAXBElement<?>> elements = expression.getExpressionEvaluator();
            List<PrismValue> valueList = getExpressionValues(elements, context);
            for (PrismValue value : valueList) {
                if (!(value instanceof PrismPropertyValue)) {
                    //todo ignoring other values for now
                    continue;
                }
                values.add(new ACValueConstructionDto(this, ((PrismPropertyValue) value).getValue()));
            }
        }
        return values;
    }

    private List<PrismValue> getExpressionValues(List<JAXBElement<?>> elements, PrismContext context) throws SchemaException {
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        Item<PrismValue, ?> item = StaticExpressionUtil.parseValueElements(elements, definition, "gui");
        return item.getValues();
    }

    public PrismPropertyDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        String name = definition.getDisplayName();
        return StringUtils.isNotEmpty(name) ? name : definition.getItemName().getLocalPart();
    }

    public boolean isEmpty() {
        List<ACValueConstructionDto> values = getValues();
        if (values.isEmpty()) {
            return true;
        }

        for (ACValueConstructionDto dto : values) {
            if (dto.getValue() != null) {
                return false;
            }
        }

        return true;
    }

    public ResourceAttributeDefinitionType getConstruction(PrismContext prismContext) throws SchemaException {
        if (isEmpty()) {
            return null;
        }

        ResourceAttributeDefinitionType attrConstruction = new ResourceAttributeDefinitionType();
        if (construction != null && construction.getRef() != null) {
            attrConstruction.setRef(construction.getRef());         // preserves original ref (including xmlns prefix!) - in order to avoid false deltas when comparing old and new values
        } else {
            attrConstruction.setRef(new ItemPathType(definition.getItemName()));
        }
        MappingType outbound;
        if (construction != null && construction.getOutbound() != null) {
            outbound = construction.getOutbound().clone();
        } else {
            outbound = new MappingType();
            outbound.setStrength(MappingStrengthType.STRONG);
        }
        attrConstruction.setOutbound(outbound);

        ExpressionType expression = new ExpressionType();
        outbound.setExpression(expression);

        List<ACValueConstructionDto> values = getValues();
        PrismProperty property = definition.instantiate();
        property.revive(prismContext);      // needed in serializeValueElements below
        for (ACValueConstructionDto dto : values) {
            if (dto.getValue() == null) {
                continue;
            }

            property.addRealValue(dto.getValue());
        }

        List evaluators = expression.getExpressionEvaluator();
        List<JAXBElement<RawType>> collection = StaticExpressionUtil.serializeValueElements(property);
        ObjectFactory of = new ObjectFactory();
        for (JAXBElement<RawType> evaluator : collection) {
            evaluators.add(evaluator);
        }

        if (evaluators.isEmpty()) {
            return null;
        }

        return attrConstruction;
    }
}
