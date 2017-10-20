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
package com.evolveum.midpoint.repo.common.expression.evaluator;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.task.api.Task;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

/**
 * @author semancik
 *
 */
@Component
public class LiteralExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

	@Autowired private PrismContext prismContext;

	// Used by Spring
	public LiteralExpressionEvaluatorFactory() {
		super();
	}
	
	// Used in tests
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
	public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, D outputDefinition,
																									ExpressionFactory factory, String contextDescription, Task task, OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for literal expression evaluator");

		Item<V,D> output = StaticExpressionUtil.parseValueElements(evaluatorElements, outputDefinition, contextDescription, prismContext);

		PrismValueDeltaSetTriple<V> deltaSetTriple = ItemDelta.toDeltaSetTriple(output, null);

		return new LiteralExpressionEvaluator<V,D>(deltaSetTriple);
	}

}
