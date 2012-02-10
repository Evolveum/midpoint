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

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
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

	private Protector protector;

	GenerateValueConstructor(Protector protector) {
		super();
		this.protector = protector;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.valueconstruction.ValueConstructor#construct(javax.xml.bind.JAXBElement, com.evolveum.midpoint.schema.processor.PropertyDefinition, com.evolveum.midpoint.schema.processor.Property, java.util.Map, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismProperty construct(JAXBElement<?> constructorElement, PrismPropertyDefinition outputDefinition, PropertyPath propertyParentPath,
			PrismProperty input, Map<QName, Object> variables, String contextDescription, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		Object constructorTypeObject = constructorElement.getValue();
        if (!(constructorTypeObject instanceof GenerateValueConstructorType)) {
            throw new IllegalArgumentException("Generate value constructor cannot handle elements of type " + constructorTypeObject.getClass().getName());
        }
        GenerateValueConstructorType constructorType = (GenerateValueConstructorType) constructorTypeObject;
		
        QName outputType = outputDefinition.getTypeName();
        if (!outputType.equals(DOMUtil.XSD_STRING) && !outputType.equals(SchemaConstants.R_PROTECTED_STRING_TYPE)) {
        	throw new IllegalArgumentException("Generate value constructor cannot generate values for properties of type " + outputType);
        }
        
    	int length = DEFAULT_LENGTH;
    	if (constructorType.getLength() != null) {
    		length = constructorType.getLength().intValue();
    	}
		RandomString randomString = new RandomString(length);
		String stringValue= randomString.nextString();
        Object value  = stringValue;
        
        if (outputType.equals(SchemaConstants.R_PROTECTED_STRING_TYPE)) {
        	try {
				value = protector.encryptString(stringValue);
			} catch (EncryptionException e) {
				throw new ExpressionEvaluationException("Crypto error: "+e.getMessage(),e);
			}
        }
        
		PrismProperty output = outputDefinition.instantiate(propertyParentPath);
		PrismPropertyValue<Object> pValue = new PrismPropertyValue<Object>(value);
		output.setValue(pValue);
		
		return output;
		
	}

}
