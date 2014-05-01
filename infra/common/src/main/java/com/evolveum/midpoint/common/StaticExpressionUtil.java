/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
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
 *
 */
public class StaticExpressionUtil {
	
	/**
	 * Returns either Object (if result is supposed to be single-value) or Collection<X> (if result is supposed to be multi-value) 
	 */
	public static Object getStaticOutput(ExpressionType expressionType, PrismPropertyDefinition outputDefinition, 
			String contextDescription, ExpressionReturnMultiplicityType preferredMultiplicity, PrismContext prismContext) throws SchemaException {
		PrismProperty<?> output = getPropertyStatic(expressionType, outputDefinition, contextDescription, prismContext);
		ExpressionReturnMultiplicityType multiplicity = preferredMultiplicity;
		if (expressionType.getReturnMultiplicity() != null) {
			multiplicity = expressionType.getReturnMultiplicity();
		} else if (output != null && output.size() > 1) {
			multiplicity = ExpressionReturnMultiplicityType.MULTI;
		}
		if (output == null) {
			switch (multiplicity) {
				case MULTI: return new ArrayList<Object>(0);
				case SINGLE: return null;
				default: throw new IllegalStateException("Unknown return type "+multiplicity);
			}
		} else {
			switch (multiplicity) {
				case MULTI: return output.getRealValues();
				case SINGLE: return output.getRealValue();
				default: throw new IllegalStateException("Unknown return type "+multiplicity);
			}
		}
	}

	public static <X> PrismProperty<X> getPropertyStatic(ExpressionType expressionType, PrismPropertyDefinition outputDefinition, 
			String contextDescription, PrismContext prismContext) throws SchemaException {
		Collection<JAXBElement<?>> expressionEvaluatorElement = expressionType.getExpressionEvaluator();
		return (PrismProperty) parseValueElements(expressionEvaluatorElement, outputDefinition, contextDescription, prismContext);
	}
	
	/**
	 * Always returns collection, even for single-valued results. 
	 */
	public static <X> Collection<X> getPropertyStaticRealValues(ExpressionType expressionType, PrismPropertyDefinition outputDefinition, 
			String contextDescription, PrismContext prismContext) throws SchemaException {
		PrismProperty<X> output = getPropertyStatic(expressionType, outputDefinition, contextDescription, prismContext);
		return output.getRealValues();
	}
	

	public static <V extends PrismValue> Item<V> parseValueElements(Collection<?> valueElements, ItemDefinition outputDefinition, 
			String contextDescription, PrismContext prismContext) throws SchemaException {
		
		Item<V> output = null;
		XNodeProcessor xnodeProcessor = prismContext.getXnodeProcessor();
		
		for (Object valueElement: valueElements) {
			if (!(valueElement instanceof JAXBElement<?>)) {
				throw new SchemaException("Literal expression cannot handle element "+valueElement+" "+valueElement.getClass().getName()+" in "
						+contextDescription);
			}
			QName valueElementName = JAXBUtil.getElementQName(valueElement);
			if (!valueElementName.equals(SchemaConstants.C_VALUE)) {
				throw new SchemaException("Literal expression cannot handle element <"+valueElementName + "> in "+ contextDescription);
			}
			
			JAXBElement<?> jaxbElement = (JAXBElement<?>)valueElement;
			if (!RawType.class.isAssignableFrom(jaxbElement.getDeclaredType())) {
				throw new SchemaException("Literal expression cannot handle JAXBElement value type "+jaxbElement.getDeclaredType()+" in "
						+contextDescription);
			}
			
			RawType rawType = (RawType)jaxbElement.getValue();
			
			//Item<V> elementItem = xnodeProcessor.parseItem(rawType.getXnode(), outputDefinition.getName(), outputDefinition);
            Item<V> elementItem = rawType.getParsedItem(outputDefinition);
            if (output == null) {
				output = elementItem;
			} else {
				output.addAll(elementItem.getClonedValues());
			}
		}
		return output;
	}

	public static <V extends PrismValue> List<JAXBElement<RawType>> serializeValueElements(Item<V> item, String contextDescription) throws SchemaException {
		if (item == null) {
			return null;
		}
		XNodeProcessor xnodeProcessor = item.getPrismContext().getXnodeProcessor();
		List<JAXBElement<RawType>> elements = new ArrayList<>(1);
		XNode xnode = xnodeProcessor.serializeItem(item);
		RawType rawType = new RawType(xnode);
		JAXBElement<RawType> jaxbElement = new JAXBElement<RawType>(SchemaConstants.C_VALUE, RawType.class, rawType);
		elements.add(jaxbElement);
		return elements;
	}

}
