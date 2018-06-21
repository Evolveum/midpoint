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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.task.api.Task;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractObjectResolvableExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

/**
 * This is NOT autowired evaluator.
 * 
 * @author semancik
 *
 */
public class GenerateExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

	private final Protector protector;
	private final PrismContext prismContext;
	private final ValuePolicyProcessor valuePolicyGenerator;

	public GenerateExpressionEvaluatorFactory(ExpressionFactory expressionFactory, Protector protector, ValuePolicyProcessor valuePolicyGenerator, PrismContext prismContext) {
		super(expressionFactory);
		this.protector = protector;
		this.prismContext = prismContext;
		this.valuePolicyGenerator = valuePolicyGenerator;
	}
	
	@Override
	public QName getElementName() {
		return new ObjectFactory().createGenerate(new GenerateExpressionEvaluatorType()).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
																									D outputDefinition, ExpressionFactory factory, String contextDescription, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException {

        Validate.notNull(outputDefinition, "output definition must be specified for 'generate' expression evaluator");

		if (evaluatorElements.size() > 1) {
			throw new SchemaException("More than one evaluator specified in "+contextDescription);
		}
		JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

		Object evaluatorTypeObject = null;
        if (evaluatorElement != null) {
        	evaluatorTypeObject = evaluatorElement.getValue();
        }
        if (evaluatorTypeObject != null && !(evaluatorTypeObject instanceof GenerateExpressionEvaluatorType)) {
            throw new SchemaException("Generate expression evaluator cannot handle elements of type " + evaluatorTypeObject.getClass().getName()+" in "+contextDescription);
        }

        GenerateExpressionEvaluatorType generateEvaluatorType = (GenerateExpressionEvaluatorType)evaluatorTypeObject;

		return new GenerateExpressionEvaluator<>(generateEvaluatorType, outputDefinition, protector, getObjectResolver(), valuePolicyGenerator, prismContext);
	}

}
