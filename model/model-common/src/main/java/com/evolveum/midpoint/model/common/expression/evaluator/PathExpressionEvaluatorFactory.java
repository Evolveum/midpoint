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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author semancik
 *
 */
public class PathExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {

	private PrismContext prismContext;
	private ObjectResolver objectResolver;
	private Protector protector;

	public PathExpressionEvaluatorFactory(PrismContext prismContext, Protector protector) {
		super();
		this.prismContext = prismContext;
		this.protector = protector;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	@Override
	public QName getElementName() {
		return new ObjectFactory().createPath(null).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.ItemDefinition, com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public <V extends PrismValue, D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
																									 D outputDefinition, String contextDescription, Task task, OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for path expression evaluator");

		if (evaluatorElements.size() > 1) {
			throw new SchemaException("More than one evaluator specified in "+contextDescription);
		}
		JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

		Object evaluatorElementObject = evaluatorElement.getValue();
		 if (!(evaluatorElementObject instanceof ItemPathType)) {
		        throw new IllegalArgumentException("Path expression cannot handle elements of type "
		        		+ evaluatorElementObject.getClass().getName()+" in "+contextDescription);
		}
        ItemPath path = ((ItemPathType)evaluatorElementObject).getItemPath();

        return new PathExpressionEvaluator<>(path, objectResolver, outputDefinition, protector, prismContext);

	}

}
