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
package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Radovan Semancik
 */
public interface ScriptEvaluator {
	
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType, ExpressionVariables variables, 
			ItemDefinition outputDefinition, ScriptExpressionReturnTypeType suggestedReturnType, ObjectResolver objectResolver,
    		Collection<FunctionLibrary> functions, String contextDescription, OperationResult result)
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
