/*
 * Copyright (c) 2010-2019 Evolveum
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

import java.util.List;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author Radovan Semancik
 */
public interface ScriptEvaluator {

	<T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * Returns human readable name of the language that this evaluator supports
     */
	String getLanguageName();

	/**
	 * Returns URL of the language that this evaluator can handle
	 */
	String getLanguageUrl();

}
