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
package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Radovan Semancik
 */
public interface ScriptEvaluator {

	<T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType, ExpressionVariables variables,
			ItemDefinition outputDefinition, Function<Object, Object> additionalConvertor,
			ScriptExpressionReturnTypeType suggestedReturnType, ObjectResolver objectResolver,
			Collection<FunctionLibrary> functions, String contextDescription, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException;

    /**
     * Returns human readable name of the language that this evaluator supports
     */
	String getLanguageName();

	/**
	 * Returns URL of the language that this evaluator can handle
	 */
	String getLanguageUrl();

}
