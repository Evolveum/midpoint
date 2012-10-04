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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAttributeDefinitionType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ACAttributeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_VALUES = "values";

    private PrismPropertyDefinition definition;
    private ResourceAttributeDefinitionType construction;
    private List<ACValueConstructionDto> values;

    public ACAttributeDto(PrismPropertyDefinition definition, ResourceAttributeDefinitionType construction) {
        Validate.notNull(definition, "Prism property definition must not be null.");
        Validate.notNull(construction, "Value construction must not be null.");

        this.definition = definition;
        this.construction = construction;
    }

    public List<ACValueConstructionDto> getValues() {
        if (values == null) {
            values = createValues();

            if (values.isEmpty()) {
                values.add(new ACValueConstructionDto(this, null));
            }
        }
        return values;
    }

    private List<ACValueConstructionDto> createValues() {
        List<ACValueConstructionDto> values = new ArrayList<ACValueConstructionDto>();
        MappingType outbound = construction.getOutbound();
        if (outbound == null || outbound.getExpression() == null) {
            return values;
        }
        ExpressionType expression = outbound.getExpression();
        if (expression.getExpressionEvaluator() != null) {
            JAXBElement<?> element = expression.getExpressionEvaluator();
            values.add(new ACValueConstructionDto(this, getExpressionValue(element)));
        }
        if (expression.getSequence() == null) {
            return values;
        }

        ExpressionType.Sequence sequence = expression.getSequence();
        List<JAXBElement<?>> elements = sequence.getExpressionEvaluator();
        for (JAXBElement element : elements) {
            values.add(new ACValueConstructionDto(this, getExpressionValue(element)));
        }

        return values;
    }

    private Object getExpressionValue(JAXBElement element) {
        Element expression = (Element) element.getValue();
        if (!DOMUtil.isElementName(expression, SchemaConstants.C_VALUE)) {
            return null;
        }

        return expression.getTextContent();
    }

    public PrismPropertyDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        String name = definition.getDisplayName();
        return StringUtils.isNotEmpty(name) ? name : definition.getName().getLocalPart();
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

    public ResourceAttributeDefinitionType getConstruction() {
        if (isEmpty()) {
            return null;
        }

        ResourceAttributeDefinitionType attrConstruction = new ResourceAttributeDefinitionType();
        attrConstruction.setRef(definition.getName());
        MappingType outbound = new MappingType();
        attrConstruction.setOutbound(outbound);

        ObjectFactory of = new ObjectFactory();


        return attrConstruction;
    }
}
