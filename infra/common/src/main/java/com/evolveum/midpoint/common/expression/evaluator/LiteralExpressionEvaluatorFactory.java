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
package com.evolveum.midpoint.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;

/**
 * @author semancik
 *
 */
public class LiteralExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {
	
	private PrismContext prismContext;

	public LiteralExpressionEvaluatorFactory(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
	}

	@Override
	public QName getElementName() {
		return new ObjectFactory().createValue(null).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public <V extends PrismValue> ExpressionEvaluator<V> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, ItemDefinition outputDefinition, 
			String contextDescription, OperationResult result) throws SchemaException {
		
		Item<V> output = parseValueElements(evaluatorElements, outputDefinition, contextDescription, prismContext);
		
		PrismValueDeltaSetTriple<V> deltaSetTriple = ItemDelta.toDeltaSetTriple(output, null);
		
		return new LiteralExpressionEvaluator<V>(deltaSetTriple);
	}
	
	public static <V extends PrismValue> Item<V> parseValueElements(Collection<?> valueElements, ItemDefinition outputDefinition, 
			String contextDescription, PrismContext prismContext) throws SchemaException {
		
		Item<V> output = null;
		
		for (Object valueElement: valueElements) {
			QName valueElementName = JAXBUtil.getElementQName(valueElement);
			if (!valueElementName.equals(SchemaConstants.C_VALUE)) {
				throw new SchemaException("Literal expression cannot handle element <"+valueElementName + "> in "+ contextDescription);
			}
			
			if (valueElement instanceof JAXBElement<?>) {
				valueElement = ((JAXBElement<?>)valueElement).getValue();
			}
			
			if (!(valueElement instanceof Element)) {
				throw new SchemaException("Literal expression can only handle DOM elements, but got "+valueElement.getClass().getName()+" in "
						+contextDescription);
			}
			Element valueDomElement = (Element)valueElement;
			
			List<Element> valueSubelements = DOMUtil.listChildElements(valueDomElement);
			if (valueSubelements == null || valueSubelements.isEmpty()) {
				if (StringUtils.isBlank(valueDomElement.getTextContent())) {
					// This is OK. Empty element means empty value
					if (output == null) {
						output = outputDefinition.instantiate();
					}
				} else if (outputDefinition instanceof PrismPropertyDefinition) {
					// No sub-elements. In case of property try to parse the value is directly in the body of <value> element.
					Item<V> valueOutput = (Item<V>) prismContext.getPrismDomProcessor().parsePropertyFromValueElement(valueDomElement, 
							(PrismPropertyDefinition) outputDefinition);
					if (output == null) {
						output = valueOutput;
					} else {
						output.addAll(valueOutput.getClonedValues());
					}
				} else {
					throw new SchemaException("Tense expression forms can only be used to evalueate properties, not "+
							output.getClass().getSimpleName()+", try to enclose the value with proper elements");
				}
			} else { 
				Item<V> valueOutput = (Item<V>) prismContext.getPrismDomProcessor().parseItem(valueSubelements, 
						outputDefinition.getName(), outputDefinition);
				if (output == null) {
					output = valueOutput;
				} else {
					output.addAll(valueOutput.getClonedValues());
				}
			}
		}
		return output;
	}
	
	public static <V extends PrismValue> List<?> serializeValueElements(Item<V> item, String contextDescription) throws SchemaException {
		if (item == null) {
			return null;
		}
		PrismDomProcessor domProcessor = item.getPrismContext().getPrismDomProcessor();
		List<Object> elements = new ArrayList<Object>(1);
		Element valueElement = DOMUtil.createElement(DOMUtil.getDocument(), SchemaConstants.C_VALUE);
		domProcessor.serializeItemToDom(item, valueElement);
		elements.add(valueElement);
		return elements;
	}

}
