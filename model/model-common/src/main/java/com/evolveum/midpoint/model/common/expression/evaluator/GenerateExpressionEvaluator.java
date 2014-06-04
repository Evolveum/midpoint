/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
	
	private boolean isNotEmptyMinLength(StringPolicyType policy){
		Integer minLength = policy.getLimitations().getMinLength();
		if (minLength != null){
			if (minLength.intValue() == 0){
				return false;
			}
			return true;
		}
		return  false;
	}
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
		String stringValue = null;
	    GenerateExpressionEvaluatorModeType mode = generateEvaluatorType.getMode();
	    if (mode == null || mode == GenerateExpressionEvaluatorModeType.POLICY) {
	    
			// TODO: generate value based on stringPolicyType (if not null)
			if (stringPolicyType != null) {
				if (isNotEmptyMinLength(stringPolicyType)) {
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
	    		stringValue = randomString.nextString();	
	        }
	        
	    } else if (mode == GenerateExpressionEvaluatorModeType.UUID) {
	    	UUID randomUUID = UUID.randomUUID();
	    	stringValue = randomUUID.toString();
	    	
	    } else {
	    	throw new ExpressionEvaluationException("Unknown mode for generate expression: "+mode);
	    }
        
        Object value;
        QName outputType = outputDefinition.getTypeName();
        if (outputType.equals(DOMUtil.XSD_STRING)) {
        	value  = stringValue;
        } else if (outputType.equals(ProtectedStringType.COMPLEX_TYPE)) {
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		if (elementStringPolicy != null) {
			return "generate: "+toHumanReadableString(elementStringPolicy);
		}
		return "generate";
	}

	private String toHumanReadableString(StringPolicyType stringPolicy) {
		if (stringPolicy.getDescription() != null) {
			return "StringPolicy: "+StringUtils.abbreviate(stringPolicy.getDescription(), 60);
		}
		return "StringPolicy";
	}

}
