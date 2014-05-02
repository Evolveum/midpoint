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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsIsExpressionEvaluatorType;

/**
 * @author Radovan Semancik
 */
public class AsIsExpressionEvaluator<V extends PrismValue> implements ExpressionEvaluator<V> {
	
	private PrismContext prismContext;
	ItemDefinition outputDefinition;
	private AsIsExpressionEvaluatorType asIsExpressionEvaluatorType;
	private Protector protector;

	public AsIsExpressionEvaluator(AsIsExpressionEvaluatorType asIsExpressionEvaluatorType, 
			ItemDefinition outputDefinition, Protector protector, PrismContext prismContext) {
		this.asIsExpressionEvaluatorType = asIsExpressionEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
		this.protector = protector;
	}

	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext params) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
        
		Source<V> source;
    	if (params.getSources().isEmpty()) {
    		throw new ExpressionEvaluationException("asIs evaluator cannot work without a source in "+params.getContextDescription());
    	}
    	if (params.getSources().size() > 1) {
    		Source<V> defaultSource = (Source<V>) params.getDefaultSource();
    		if (defaultSource != null) {
    			source = defaultSource;
    		} else {
    			throw new ExpressionEvaluationException("asIs evaluator cannot work with more than one source ("+params.getSources().size()
    				+" sources specified) without specification of a default source, in "+params.getContextDescription());
    		}
    	} else {
    		source = (Source<V>) params.getSources().iterator().next();
    	}
        PrismValueDeltaSetTriple<V> sourceTriple = ItemDelta.toDeltaSetTriple(source.getItemOld(), source.getDelta());
        
        if (sourceTriple == null) {
        	return null;
        }
        return ExpressionUtil.toOutputTriple(sourceTriple, outputDefinition, source.getResidualPath(), 
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
