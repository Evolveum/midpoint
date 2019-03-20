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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 *
 * @author Radovan Semancik
 * @param <C> compiled code
 *
 */
public abstract class AbstractScriptEvaluator implements ScriptEvaluator {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractScriptEvaluator.class);

	private final PrismContext prismContext;
	private final Protector protector;
	private final LocalizationService localizationService;

	public AbstractScriptEvaluator(PrismContext prismContext, Protector protector,
			LocalizationService localizationService) {
		this.prismContext = prismContext;
		this.protector = protector;
		this.localizationService = localizationService;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public Protector getProtector() {
		return protector;
	}

	public LocalizationService getLocalizationService() {
		return localizationService;
	}

	/**
	 * Returns simple variable map: name -> value.
	 */
	protected Map<String,Object> prepareScriptVariablesValueMap(ScriptExpressionEvaluationContext context) 
					throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Map<String,Object> scriptVariableMap = new HashMap<>();
		// Functions
		if (context.getFunctions() != null) {
			for (FunctionLibrary funcLib: context.getFunctions()) {
				scriptVariableMap.put(funcLib.getVariableName(), funcLib.getGenericFunctions());
			}
		}
		
		// Variables
		if (context.getVariables() != null) {
			for (Entry<String, TypedValue> variableEntry: context.getVariables().entrySet()) {
				if (variableEntry.getKey() == null) {
					// This is the "root" node. We have no use for it in script expressions, just skip it
					continue;
				}
				String variableName = variableEntry.getKey();
				TypedValue variableTypedValue = ExpressionUtil.convertVariableValue(variableEntry.getValue(), variableName, context.getObjectResolver(), context.getContextDescription(), prismContext, context.getTask(), context.getResult());
				scriptVariableMap.put(variableName, variableTypedValue.getValue());
			}
		}

		String prismContextName = ExpressionConstants.VAR_PRISM_CONTEXT;
		if (!scriptVariableMap.containsKey(prismContextName)) {
			scriptVariableMap.put(prismContextName, prismContext);
		}
		return scriptVariableMap;
	}

}
