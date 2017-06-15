/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ErrorSelectorType;

/**
 * @author Radovan Semancik
 *
 */
public class ExceptionUtil {
	
	public static Throwable lookForTunneledException(Throwable ex) {
		if (ex instanceof TunnelException) {
			return ex.getCause();
		}
		if (ex.getCause() != null) {
			return lookForTunneledException(ex.getCause());
		}
		return null;
	}

	public static String lookForMessage(Throwable e) {
		if (e.getMessage() != null) {
			return e.getMessage();
		}
		if (e.getCause() != null) {
			return lookForMessage(e.getCause());
		}
		return null;
	}
	
	public static boolean isSelected(ErrorSelectorType selector, Throwable exception) {
		if (selector == null) {
			return false;
		}
		if (exception instanceof CommunicationException) {
			return Boolean.TRUE.equals(selector.isNetwork());
		}
		if (exception instanceof SecurityViolationException) {
			return Boolean.TRUE.equals(selector.isSecurity());
		}
		if (exception instanceof PolicyViolationException) {
			return Boolean.TRUE.equals(selector.isPolicy());
		}
		if (exception instanceof SchemaException) {
			return Boolean.TRUE.equals(selector.isSchema());
		}
		if (exception instanceof ConfigurationException || exception instanceof ExpressionEvaluationException) {
			return Boolean.TRUE.equals(selector.isConfiguration());
		}
		return Boolean.TRUE.equals(selector.isGeneric());
	}
	
}
