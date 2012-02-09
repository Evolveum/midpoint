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
package com.evolveum.midpoint.common.valueconstruction;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyValue;
import com.evolveum.midpoint.prism.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * @author Radovan Semancik
 */
public class ExpressionValueConstructor implements ValueConstructor {

    private ExpressionFactory factory;

    ExpressionValueConstructor(ExpressionFactory factory) {
        this.factory = factory;
    }

    /* (non-Javadoc)
      * @see com.evolveum.midpoint.common.valueconstruction.ValueConstructor#construct(com.evolveum.midpoint.schema.processor.PropertyDefinition, com.evolveum.midpoint.schema.processor.Property)
      */
    @Override
    public PrismProperty construct(JAXBElement<?> constructorElement, PrismPropertyDefinition outputDefinition, PropertyPath propertyParentPath,
            PrismProperty input, Map<QName, Object> variables, String contextDescription, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

        Object constructorTypeObject = constructorElement.getValue();
        if (!(constructorTypeObject instanceof ExpressionType)) {
            throw new IllegalArgumentException("Expression value constructor cannot handle elements of type " + constructorTypeObject.getClass().getName());
        }
        ExpressionType constructorType = (ExpressionType) constructorTypeObject;

        Expression expression = factory.createExpression(constructorType, contextDescription);

        expression.addVariableDefinitions(variables);

        QName typeName = outputDefinition.getTypeName();
        Class<Object> type = XsdTypeConverter.toJavaType(typeName);
        PrismProperty output = outputDefinition.instantiate(propertyParentPath);

        if (outputDefinition.isMultiValue()) {
            List<PropertyValue<Object>> resultValues = expression.evaluateList(type, result);
            output.getValues().addAll(resultValues);
        } else {
            PropertyValue<Object> resultValue = expression.evaluateScalar(type, result);
            if (resultValue != null) {
                output.getValues().add(resultValue);
            }
        }

        return output;
    }

}
