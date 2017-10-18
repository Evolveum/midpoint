/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
public class ScriptExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {

	private ScriptExpressionFactory scriptExpressionFactory;
    private SecurityContextManager securityContextManager;

	public ScriptExpressionEvaluatorFactory(ScriptExpressionFactory scriptExpressionFactory, SecurityContextManager securityContextManager) {
		this.scriptExpressionFactory = scriptExpressionFactory;
        this.securityContextManager = securityContextManager;
	}

	@Override
	public QName getElementName() {
		return new ObjectFactory().createScript(new ScriptExpressionEvaluatorType()).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
			D outputDefinition, ExpressionFactory factory, String contextDescription, Task task, OperationResult result) throws SchemaException {

		if (evaluatorElements.size() > 1) {
			throw new SchemaException("More than one evaluator specified in "+contextDescription);
		}
		JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

		Object evaluatorElementObject = evaluatorElement.getValue();
        if (!(evaluatorElementObject instanceof ScriptExpressionEvaluatorType)) {
            throw new IllegalArgumentException("Script expression cannot handle elements of type " + evaluatorElementObject.getClass().getName());
        }
        ScriptExpressionEvaluatorType scriptType = (ScriptExpressionEvaluatorType) evaluatorElementObject;

        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(scriptType, outputDefinition, factory, contextDescription, task, result);

        return new ScriptExpressionEvaluator<>(scriptType, scriptExpression, securityContextManager);

	}

}
