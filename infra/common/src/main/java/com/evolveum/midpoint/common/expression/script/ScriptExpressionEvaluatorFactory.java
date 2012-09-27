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
package com.evolveum.midpoint.common.expression.script;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
public class ScriptExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {
	
	ScriptExpressionFactory scriptExpressionFactory;

	public ScriptExpressionEvaluatorFactory(ScriptExpressionFactory scriptExpressionFactory) {
		this.scriptExpressionFactory = scriptExpressionFactory;
	}

	@Override
	public QName getElementName() {
		return new ObjectFactory().createScript(new ScriptExpressionEvaluatorType()).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public <V extends PrismValue> ExpressionEvaluator<V> createEvaluator(JAXBElement<?> evaluatorElement,
			ItemDefinition outputDefinition, String contextDescription) throws SchemaException {
		
		Object evaluatorElementObject = evaluatorElement.getValue();
        if (!(evaluatorElementObject instanceof ScriptExpressionEvaluatorType)) {
            throw new IllegalArgumentException("Script expression cannot handle elements of type " + evaluatorElementObject.getClass().getName());
        }
        ScriptExpressionEvaluatorType scriptType = (ScriptExpressionEvaluatorType) evaluatorElementObject;
        
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, contextDescription);
        
        return new ScriptExpressionEvaluator(scriptType, scriptExpression);
        
	}

}
