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

import java.util.Collection;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.ExpressionUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AsIsExpressionEvaluatorType;

/**
 * @author Radovan Semancik
 */
public class AsIsExpressionEvaluator<V extends PrismValue> implements ExpressionEvaluator<V> {
	
	private PrismContext prismContext;
	ItemDefinition outputDefinition;
	private AsIsExpressionEvaluatorType asIsExpressionEvaluatorType;

	public AsIsExpressionEvaluator(AsIsExpressionEvaluatorType asIsExpressionEvaluatorType, ItemDefinition outputDefinition, PrismContext prismContext) {
		this.asIsExpressionEvaluatorType = asIsExpressionEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.prismContext = prismContext;
	}

	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext params) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
        
    	if (params.getSources().isEmpty()) {
    		throw new ExpressionEvaluationException("asIs evaluator cannot work without a source in "+params.getContextDescription());
    	}
    	if (params.getSources().size() > 1) {
    		throw new ExpressionEvaluationException("asIs evaluator cannot work more than one source ("+params.getSources().size()
    				+" sources specified) in "+params.getContextDescription());
    	}
    	Source<V> source = (Source<V>) params.getSources().iterator().next();
        PrismValueDeltaSetTriple<V> sourceTriple = ItemDelta.toDeltaSetTriple(source.getItemOld(), source.getDelta());
        
        if (sourceTriple == null) {
        	return null;
        }
        return ExpressionUtil.toOutputTriple(sourceTriple, outputDefinition, source.getResidualPath(), prismContext);
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "asIs";
	}

}
