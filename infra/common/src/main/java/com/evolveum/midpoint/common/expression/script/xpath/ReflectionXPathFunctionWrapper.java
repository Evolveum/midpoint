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
package com.evolveum.midpoint.common.expression.script.xpath;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import com.evolveum.midpoint.common.expression.BasicExpressionFunctions;
import com.evolveum.midpoint.common.expression.BasicExpressionFunctionsXPath;
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
			BasicExpressionFunctions.LOGGER.debug("Expression debug: {}", ReflectionUtil.debugDumpArgList(argList));
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
			LOGGER.error("Cannot find {} function with {} arguments", new Object[]{functionName, arity, e});
			throw new XPathFunctionException(e);
		}

	}

}
