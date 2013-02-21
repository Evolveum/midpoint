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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;

import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class ReflectionXPathFunctionResolver implements XPathFunctionResolver {

	private static final Object LOG_FUNCTION_NAME = "logDebug";
	
	public static final Trace LOGGER = TraceManager.getTrace(ReflectionXPathFunctionResolver.class);
	
	private Collection<FunctionLibrary> functions;
	
	public ReflectionXPathFunctionResolver(Collection<FunctionLibrary> functions) {
		super();
		this.functions = functions;
	}

	/* (non-Javadoc)
	 * @see javax.xml.xpath.XPathFunctionResolver#resolveFunction(javax.xml.namespace.QName, int)
	 */
	@Override
	public XPathFunction resolveFunction(QName functionQName, int arity) {
		boolean enableDebug = false;
		String namespace = functionQName.getNamespaceURI();
		if (namespace == null) {
			namespace = MidPointConstants.NS_FUNC_BASIC;
			enableDebug = true;
		} else if (namespace.equals(MidPointConstants.NS_FUNC_BASIC)) {
			enableDebug = true;
		}
		
		FunctionLibrary lib = findLibrary(namespace);
		if (lib == null) {
			LOGGER.trace("Unknown namespace for function {} function with {} arguments", functionQName, arity);
			return null;
		}
		
		Object functionObject = null;
		if (lib.getXmlFunctions() != null) {
			functionObject = lib.getXmlFunctions();
		} else {
			functionObject = lib.getGenericFunctions();
		}
		
		String functionName = functionQName.getLocalPart();
		
		LOGGER.trace("Resolving to {} function with {} arguments", functionName, arity);
		ReflectionXPathFunctionWrapper xPathFunction = new ReflectionXPathFunctionWrapper(functionObject, functionName, 
				arity, enableDebug);
		return xPathFunction;
	}

	private FunctionLibrary findLibrary(String namespace) {
		for (FunctionLibrary lib: functions) {
			if (lib.getNamespace().equals(namespace)) {
				return lib;
			}
		}
		return null;
	}

}
