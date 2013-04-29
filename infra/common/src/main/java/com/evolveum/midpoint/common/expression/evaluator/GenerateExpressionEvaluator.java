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
package com.evolveum.midpoint.common.expression.evaluator;

import java.util.Collection;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;

/**
 * @author semancik
 *
 */
public class GenerateExpressionEvaluator<V extends PrismValue> implements ExpressionEvaluator<V> {

	public static final int DEFAULT_LENGTH = 8;

	private GenerateExpressionEvaluatorType generateEvaluatorType;
	private ItemDefinition outputDefinition;
	private Protector protector;
	private PrismContext prismContext;
	private StringPolicyType elementStringPolicy;

	GenerateExpressionEvaluator(GenerateExpressionEvaluatorType generateEvaluatorType, ItemDefinition outputDefinition,
			Protector protector, StringPolicyType elementStringPolicy, PrismContext prismContext) {
		this.generateEvaluatorType = generateEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.protector = protector;
		this.elementStringPolicy = elementStringPolicy;
		this.prismContext = prismContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext params) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
				
        
        StringPolicyType stringPolicyType = null;
        
//        if (elementStringPolicy == null) {
		// if the policy was changed, the most fresh copy is needed, therefore
		// it must be resolved all time the value is generated..if it was not
		// resolved each time, the cached policy would be used and so bad values
		// would be generated
        
	        StringPolicyResolver stringPolicyResolver = params.getStringPolicyResolver();
	        if (stringPolicyResolver!=null) {
	        	stringPolicyType = stringPolicyResolver.resolve();
	        }
//        } else {
//        	stringPolicyType = elementStringPolicy;
//        }
//        
		// TODO: generate value based on stringPolicyType (if not null)
		String stringValue = null;
		if (stringPolicyType != null) {
			if (stringPolicyType.getLimitations().getMinLength() != null) {
				stringValue = ValuePolicyGenerator.generate(stringPolicyType, DEFAULT_LENGTH, true, params.getResult());
			} else{
				stringValue = ValuePolicyGenerator.generate(stringPolicyType, DEFAULT_LENGTH, false, params.getResult());
			}
			params.getResult().computeStatus();
			if (params.getResult().isError()){
				throw new ExpressionEvaluationException("Failed to generate value according to policy: " + stringPolicyType.getDescription() +". "+ params.getResult().getMessage());
			}
		}
        
        if (stringValue == null){
        	int length = DEFAULT_LENGTH;
    		RandomString randomString = new RandomString(length);
    		stringValue= randomString.nextString();	
        }
        
        Object value;
        QName outputType = outputDefinition.getTypeName();
        if (outputType.equals(DOMUtil.XSD_STRING)) {
        	value  = stringValue;
        } else if (outputType.equals(SchemaConstants.C_PROTECTED_STRING_TYPE)) {
        	try {
				value = protector.encryptString(stringValue);
			} catch (EncryptionException e) {
				throw new ExpressionEvaluationException("Crypto error: "+e.getMessage(),e);
			}
        } else if (XmlTypeConverter.canConvert(outputType)) {
        	Class<?> outputJavaType = XsdTypeMapper.toJavaType(outputType);
        	try {
        		value = XmlTypeConverter.toJavaValue(stringValue, outputJavaType, true);
        	} catch (NumberFormatException e) {
        		throw new SchemaException("Cannot convert generated string '"+stringValue+"' to data type "+outputType+": invalid number format", e);
        	} catch (IllegalArgumentException e) {
        		throw new SchemaException("Cannot convert generated string '"+stringValue+"' to data type "+outputType+": "+e.getMessage(), e);
        	}
		} else {
        	throw new IllegalArgumentException("Generate value constructor cannot generate values for properties of type " + outputType);
        }
                
		Item<V> output = outputDefinition.instantiate();
		if (output instanceof PrismProperty) {
			PrismPropertyValue<Object> pValue = new PrismPropertyValue<Object>(value);
			((PrismProperty<Object>)output).add(pValue);
		} else {
			throw new UnsupportedOperationException("Can only generate values of property, not "+output.getClass());
		}
		
		return ItemDelta.toDeltaSetTriple(output, null);
	}

}
