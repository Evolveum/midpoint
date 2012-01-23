/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.valueconstruction;

import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenerateValueConstructorType;

/**
 * @author semancik
 *
 */
public class GenerateValueConstructor implements ValueConstructor {

	private static final int DEFAULT_LENGTH = 8;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.valueconstruction.ValueConstructor#construct(javax.xml.bind.JAXBElement, com.evolveum.midpoint.schema.processor.PropertyDefinition, com.evolveum.midpoint.schema.processor.Property, java.util.Map, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public Property construct(JAXBElement<?> constructorElement, PropertyDefinition outputDefinition,
			Property input, Map<QName, Object> variables, String contextDescription, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		Object contstuctorTypeObject = constructorElement.getValue();
        if (!(contstuctorTypeObject instanceof GenerateValueConstructorType)) {
            throw new IllegalArgumentException("Generate value constructor cannot handle elements of type " + contstuctorTypeObject.getClass().getName());
        }
        GenerateValueConstructorType constructorType = (GenerateValueConstructorType) contstuctorTypeObject;
		
        if (!outputDefinition.getTypeName().equals(DOMUtil.XSD_STRING)) {
        	throw new IllegalArgumentException("Generate value constructor cannot generate values for properties of type " + outputDefinition.getTypeName());
        }
        
    	int length = DEFAULT_LENGTH;
    	if (constructorType.getLength() != null) {
    		length = constructorType.getLength().intValue();
    	}
		RandomString randomString = new RandomString(length);
        String value = randomString.nextString(); 
        
		Property output = outputDefinition.instantiate();
		PropertyValue<String> pValue = new PropertyValue<String>(value);
		output.setValue(pValue);
		
		return output;
		
	}

}
