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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
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

	public static boolean isSelected(ErrorSelectorType selector, Throwable exception, boolean defaultValue) {
		if (selector == null) {
			return defaultValue;
		}
		if (exception instanceof CommunicationException) {
			return isSelected(selector.isNetwork(), defaultValue);
		}
		if (exception instanceof SecurityViolationException) {
			return isSelected(selector.isSecurity(), defaultValue);
		}
		if (exception instanceof PolicyViolationException) {
			return isSelected(selector.isPolicy(), defaultValue);
		}
		if (exception instanceof SchemaException) {
			return isSelected(selector.isSchema(), defaultValue);
		}
		if (exception instanceof ConfigurationException || exception instanceof ExpressionEvaluationException) {
			return isSelected(selector.isConfiguration(), defaultValue);
		}
		return isSelected(selector.isGeneric(), defaultValue);
	}

	private static boolean isSelected(Boolean value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	public static LocalizableMessage getUserFriendlyMessage(Throwable cause) {
		while (cause != null) {
			if (cause instanceof CommonException) {
				LocalizableMessage userFriendlyMessage = ((CommonException)cause).getUserFriendlyMessage();
				if (userFriendlyMessage != null) {
					return userFriendlyMessage;
				}
			}
			cause = cause.getCause();
		}
		return null;
	}

	public static <T extends Throwable> T findCause(Throwable throwable, Class<T> causeClass) {
		while (throwable != null) {
			if (causeClass.isAssignableFrom(throwable.getClass())) {
				//noinspection unchecked
				return (T) throwable;
			}
			throwable = throwable.getCause();
		}
		return null;
	}
}
