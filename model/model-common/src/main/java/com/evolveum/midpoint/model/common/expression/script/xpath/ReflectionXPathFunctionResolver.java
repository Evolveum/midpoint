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

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;

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
		if (StringUtils.isEmpty(namespace)) {
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
