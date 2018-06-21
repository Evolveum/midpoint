/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsIsExpressionEvaluatorType;

/**
 * @author Radovan Semancik
 */
public class AsIsExpressionEvaluator<V extends PrismValue, D extends ItemDefinition> implements ExpressionEvaluator<V,D> {

	private PrismContext prismContext;
	D outputDefinition;
	private AsIsExpressionEvaluatorType asIsExpressionEvaluatorType;
	private Protector protector;

	public AsIsExpressionEvaluator(AsIsExpressionEvaluatorType asIsExpressionEvaluatorType,
			D outputDefinition, Protector protector, PrismContext prismContext) {
		this.asIsExpressionEvaluatorType = asIsExpressionEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
		this.protector = protector;
	}

	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {

		Source<V,D> source;
    	if (context.getSources().isEmpty()) {
    		throw new ExpressionEvaluationException("asIs evaluator cannot work without a source in "+ context.getContextDescription());
    	}
    	if (context.getSources().size() > 1) {
    		Source<V,D> defaultSource = (Source<V,D>) context.getDefaultSource();
    		if (defaultSource != null) {
    			source = defaultSource;
    		} else {
    			throw new ExpressionEvaluationException("asIs evaluator cannot work with more than one source ("+ context.getSources().size()
    				+" sources specified) without specification of a default source, in "+ context.getContextDescription());
    		}
    	} else {
    		source = (Source<V,D>) context.getSources().iterator().next();
    	}
        PrismValueDeltaSetTriple<V> sourceTriple = ItemDelta.toDeltaSetTriple(source.getItemOld(), source.getDelta());

        if (sourceTriple == null) {
        	return null;
        }
        return ExpressionUtil.toOutputTriple(sourceTriple, outputDefinition, context.getAdditionalConvertor(), source.getResidualPath(),
        		protector, prismContext);
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "asIs";
	}

}
