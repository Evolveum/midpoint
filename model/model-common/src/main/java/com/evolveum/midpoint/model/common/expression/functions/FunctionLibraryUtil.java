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
package com.evolveum.midpoint.model.common.expression.functions;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

/**
 * @author semancik
 *
 */
public class FunctionLibraryUtil {

	public static FunctionLibrary createBasicFunctionLibrary(PrismContext prismContext, Protector protector) {
		FunctionLibrary lib = new FunctionLibrary();
		lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_BASIC_VARIABLE_NAME);
		lib.setNamespace(MidPointConstants.NS_FUNC_BASIC);
		BasicExpressionFunctions func = new BasicExpressionFunctions(prismContext, protector);
		lib.setGenericFunctions(func);
		BasicExpressionFunctionsXPath funcXPath = new BasicExpressionFunctionsXPath(func);
		lib.setXmlFunctions(funcXPath);
		return lib;
	}
	
	public static FunctionLibrary createLogFunctionLibrary(PrismContext prismContext) {
		FunctionLibrary lib = new FunctionLibrary();
		lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_LOG_VARIABLE_NAME);
		lib.setNamespace(MidPointConstants.NS_FUNC_LOG);
		LogExpressionFunctions func = new LogExpressionFunctions(prismContext);
		lib.setGenericFunctions(func);
		return lib;
	}

}
