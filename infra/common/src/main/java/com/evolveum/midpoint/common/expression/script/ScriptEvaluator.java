/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.script;

import com.evolveum.midpoint.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.common.expression.MidPointFunctions;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ScriptExpressionReturnTypeType;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * @author Radovan Semancik
 */
public interface ScriptEvaluator {
	
	public <T> List<PrismPropertyValue<T>> evaluate(ScriptExpressionEvaluatorType expressionType, ScriptVariables variables, 
			ItemDefinition outputDefinition, ScriptExpressionReturnTypeType suggestedReturnType, ObjectResolver objectResolver,
    		MidPointFunctions functionLibrary, String contextDescription, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException;

    /**
     * Returns human readable name of the language that this evaluator supports
     */
    public String getLanguageName();

	/**
	 * Returns URL of the language that this evaluator can handle
	 */
	public String getLanguageUrl();

}
