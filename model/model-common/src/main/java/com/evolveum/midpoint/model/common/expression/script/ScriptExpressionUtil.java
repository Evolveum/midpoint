/**
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

/**
 * @author semancik
 *
 */
public class ScriptExpressionUtil {
	
	public static Map<String,Object> prepareScriptVariables(ExpressionVariables variables, ObjectResolver objectResolver,
			Collection<FunctionLibrary> functions,
			String contextDescription, PrismContext prismContext, Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		Map<String,Object> scriptVariables = new HashMap<>();
		// Functions
		if (functions != null) {
			for (FunctionLibrary funcLib: functions) {
				scriptVariables.put(funcLib.getVariableName(), funcLib.getGenericFunctions());
			}
		}
		// Variables
		if (variables != null) {
			for (Entry<QName, Object> variableEntry: variables.entrySet()) {
				if (variableEntry.getKey() == null) {
					// This is the "root" node. We have no use for it in JSR223, just skip it
					continue;
				}
				String variableName = variableEntry.getKey().getLocalPart();
				Object variableValue = ExpressionUtil.convertVariableValue(variableEntry.getValue(), variableName, objectResolver, contextDescription, prismContext, task, result);
				scriptVariables.put(variableName, variableValue);
			}
		}
		return scriptVariables;
	}

}
