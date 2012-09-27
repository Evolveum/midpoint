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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectFactory;

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
	public <V extends PrismValue> ExpressionEvaluator<V> createEvaluator(JAXBElement<?> evaluatorElement, ItemDefinition outputDefinition, String contextDescription) throws SchemaException {
		
		if (!evaluatorElement.getName().equals(SchemaConstants.C_VALUE)) {
			throw new SchemaException("Literal expression cannot handle elements "+evaluatorElement.getName() + " in "+ contextDescription);
		}
		Object evaluatorElementValue = evaluatorElement.getValue();
		if (!(evaluatorElementValue instanceof Element)) {
			throw new SchemaException("Literal expression can only handle DOM elements, but got "+evaluatorElementValue.getClass().getName()+" in "+contextDescription);
		}
		
		Element valueElement = (Element)evaluatorElementValue;
		Item<V> output = null;
		List<Element> valueSubelements = DOMUtil.listChildElements(valueElement);
		if (valueSubelements == null || valueSubelements.isEmpty()) {
			if (StringUtils.isBlank(valueElement.getTextContent())) {
				// This is OK. Empty element means empty value
				output = outputDefinition.instantiate();
			} else if (outputDefinition instanceof PrismPropertyDefinition) {
				// No sub-elements. In case of property try to parse the value is directly in the body of <value> element.
				output = (Item<V>) prismContext.getPrismDomProcessor().parsePropertyFromValueElement(valueElement, 
						(PrismPropertyDefinition) outputDefinition);
			} else {
				throw new SchemaException("Tense expression forms can only be used to evalueate properties, not "+
						output.getClass().getSimpleName()+", try to enclose the value with proper elements");
			}
		} else { 
			output = (Item<V>) prismContext.getPrismDomProcessor().parseItem(valueSubelements, outputDefinition.getName(), outputDefinition);
		}
		
		PrismValueDeltaSetTriple<V> deltaSetTriple = ItemDelta.toDeltaSetTriple(output, null);
		
		return new LiteralExpressionEvaluator<V>(deltaSetTriple);
	}

}
