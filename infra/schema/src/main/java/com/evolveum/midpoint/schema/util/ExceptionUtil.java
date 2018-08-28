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
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
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

	public static CriticalityType getCriticality(ErrorSelectorType selector, Throwable exception, CriticalityType defaultValue) {
		if (selector == null) {
			return defaultValue;
		}
		if (exception instanceof CommunicationException) {
			return getCriticality(selector.getNetwork(), defaultValue);
		}
		if (exception instanceof SecurityViolationException) {
			return getCriticality(selector.getSecurity(), defaultValue);
		}
		if (exception instanceof PolicyViolationException) {
			return getCriticality(selector.getPolicy(), defaultValue);
		}
		if (exception instanceof SchemaException) {
			return getCriticality(selector.getSchema(), defaultValue);
		}
		if (exception instanceof ConfigurationException || exception instanceof ExpressionEvaluationException) {
			return getCriticality(selector.getConfiguration(), defaultValue);
		}
		if (exception instanceof UnsupportedOperationException) {
			return getCriticality(selector.getUnsupported(), defaultValue);
		}
		return getCriticality(selector.getGeneric(), defaultValue);
	}

	private static CriticalityType getCriticality(CriticalityType value, CriticalityType defaultValue) {
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

	public static <T extends Throwable> T findException(Throwable throwable, Class<T> clazz) {
		while (throwable != null) {
			if (clazz.isAssignableFrom(throwable.getClass())) {
				//noinspection unchecked
				return (T) throwable;
			}
			throwable = throwable.getCause();
		}
		return null;
	}
}
