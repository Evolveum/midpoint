/*
 * Copyright (c) 2015 Evolveum
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

import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequentialValueExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
public class SequentialValueExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> implements ExpressionEvaluator<V,D> {

	private SequentialValueExpressionEvaluatorType sequentialValueEvaluatorType;
	private D outputDefinition;
	private Protector protector;
	RepositoryService repositoryService;
	private PrismContext prismContext;

	SequentialValueExpressionEvaluator(SequentialValueExpressionEvaluatorType sequentialValueEvaluatorType, 
			D outputDefinition, Protector protector, RepositoryService repositoryService, PrismContext prismContext) {
		this.sequentialValueEvaluatorType = sequentialValueEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.protector = protector;
		this.repositoryService = repositoryService;
		this.prismContext = prismContext;
	}

	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
        long counter = getSequenceCounter(sequentialValueEvaluatorType.getSequenceRef().getOid(), repositoryService, context.getResult());
		
		Object value = ExpressionUtil.convertToOutputValue(counter, outputDefinition, protector);
                
		Item<V,D> output = outputDefinition.instantiate();
		if (output instanceof PrismProperty) {
			PrismPropertyValue<Object> pValue = new PrismPropertyValue<Object>(value);
			((PrismProperty<Object>)output).add(pValue);
		} else {
			throw new UnsupportedOperationException("Can only generate values of property, not "+output.getClass());
		}
		
		return ItemDelta.toDeltaSetTriple(output, null);
	}
	
	public static long getSequenceCounter(String sequenceOid, RepositoryService repositoryService, OperationResult result) throws ObjectNotFoundException, SchemaException {
    	LensContext<? extends FocusType> ctx = ModelExpressionThreadLocalHolder.getLensContext();
    	if (ctx == null) {
    		throw new IllegalStateException("No lens context");
    	}
    	
    	Long counter = ctx.getSequenceCounter(sequenceOid);
    	if (counter == null) {
    		counter = repositoryService.advanceSequence(sequenceOid, result);
    		ctx.setSequenceCounter(sequenceOid, counter);
    	}
    	
    	return counter;
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "squentialValue: "+sequentialValueEvaluatorType.getSequenceRef().getOid();
	}

}
