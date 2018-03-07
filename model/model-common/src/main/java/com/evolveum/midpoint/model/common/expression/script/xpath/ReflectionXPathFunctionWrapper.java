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
package com.evolveum.midpoint.model.common.expression.script.xpath;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import com.evolveum.midpoint.model.common.expression.functions.LogExpressionFunctions;
import com.evolveum.midpoint.util.ReflectionUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class ReflectionXPathFunctionWrapper implements XPathFunction {

private static final Object LOG_FUNCTION_NAME = "logDebug";

	public static final Trace LOGGER = TraceManager.getTrace(ReflectionXPathFunctionWrapper.class);

	private Object functionObject;
	private String functionName;
	private int arity;
	private boolean enableDebug;

	ReflectionXPathFunctionWrapper(Object functionObject, String functionName, int arity, boolean enableDebug) {
		super();
		this.functionObject = functionObject;
		this.functionName = functionName;
		this.arity = arity;
		this.enableDebug = enableDebug;
	}

	/* (non-Javadoc)
	 * @see javax.xml.xpath.XPathFunction#evaluate(java.util.List)
	 */
	@Override
	public Object evaluate(List argList) throws XPathFunctionException {
		if (enableDebug && LOG_FUNCTION_NAME.equals(functionName)) {
			LogExpressionFunctions.LOGGER.debug("{}", ReflectionUtil.debugDumpArgList(argList));
			return null;
		}

		try {
			return ReflectionUtil.invokeMethod(functionObject, functionName, argList);
		} catch (IllegalArgumentException e) {
			throw new XPathFunctionException(e);
		} catch (IllegalAccessException e) {
			throw new XPathFunctionException(e);
		} catch (InvocationTargetException e) {
			throw new XPathFunctionException(e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("Cannot find {} function with {} arguments: {}", new Object[]{functionName, arity, e});
			throw new XPathFunctionException(e);
		}

	}

}
