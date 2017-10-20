/*
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.expr;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequentialValueExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
@Component
public class SequentialValueExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

	@Autowired private Protector protector;
	@Autowired private PrismContext prismContext;
	@Autowired private RepositoryService repositoryService;

	@Override
	public QName getElementName() {
		return new ObjectFactory().createSequentialValue(new SequentialValueExpressionEvaluatorType()).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
																									D outputDefinition, ExpressionFactory factory, String contextDescription, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException {

		if (evaluatorElements.size() > 1) {
			throw new SchemaException("More than one evaluator specified in "+contextDescription);
		}
		JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

		Object evaluatorTypeObject = null;
        if (evaluatorElement != null) {
        	evaluatorTypeObject = evaluatorElement.getValue();
        }
        if (evaluatorTypeObject != null && !(evaluatorTypeObject instanceof SequentialValueExpressionEvaluatorType)) {
            throw new SchemaException("SequentialValue expression evaluator cannot handle elements of type " + evaluatorTypeObject.getClass().getName()+" in "+contextDescription);
        }

        SequentialValueExpressionEvaluatorType seqEvaluatorType = (SequentialValueExpressionEvaluatorType)evaluatorTypeObject;

        if (seqEvaluatorType.getSequenceRef() == null || seqEvaluatorType.getSequenceRef().getOid() == null) {
        	throw new SchemaException("Missing sequence reference in sequentialValue expression evaluator in "+contextDescription);
        }

		return new SequentialValueExpressionEvaluator<V,D>(seqEvaluatorType, outputDefinition, protector, repositoryService, prismContext);
	}

}
