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
package com.evolveum.midpoint.schema.result;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

/**
 *
 * @author lazyman
 *
 */
public abstract class OperationResultFactory {

	private static long TOKEN_NUMBER = 0;

	private static synchronized long getNextToken() {
		TOKEN_NUMBER++;

		return TOKEN_NUMBER;
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, String message, String localizedMessage) {
		return createOperationResult(operation, status, null, message, null, localizedMessage, null);
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, String message, String localizedMessage,
			Object[] localizedArguments) {
		return createOperationResult(operation, status, null, message, null, localizedMessage,
				localizedArguments);
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, String message, String messageCode, String localizedMessage) {
		return createOperationResult(operation, status, null, message, messageCode, localizedMessage, null);
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, String message, String messageCode, String localizedMessage,
			Object[] localizedArguments) {
		return createOperationResult(operation, status, null, message, messageCode, localizedMessage,
				localizedArguments);
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, Map<String, Element> params, String message,
			String messageCode, String localizedMessage, Object[] localizedArguments) {
		OperationResultType result = createOperationResult(operation, status, params, message, messageCode);
		if (StringUtils.isEmpty(localizedMessage)) {
			return result;
		}

		ObjectFactory factory = new ObjectFactory();
		SingleLocalizableMessageType localizedMessageType = factory.createSingleLocalizableMessageType();
		result.setUserFriendlyMessage(localizedMessageType);
		localizedMessageType.setKey(localizedMessage);
		if (localizedArguments == null || localizedArguments.length == 0) {
			return result;
		}

		for (Object object : localizedArguments) {
			LocalizableMessageArgumentType arg = new LocalizableMessageArgumentType();
			if (object != null) {
				arg.setValue(object.toString());
			}

			localizedMessageType.getArgument().add(arg);
		}

		return result;
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, Map<String, Element> params, String message, String messageCode) {
		OperationResultType result = createOperationResult(operation, status, params, message);
		result.setMessageCode(messageCode);

		return result;
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, Map<String, Element> params, String message) {
		OperationResultType result = createOperationResult(operation, status, params);
		result.setMessage(message);

		return result;
	}

	public static OperationResultType createOperationResult(String operation,
			OperationResultStatusType status, Map<String, Element> params) {

		OperationResultType result = createOperationResult(operation, status);
		if (params == null || params.isEmpty()) {
			return result;
		}

		ObjectFactory factory = new ObjectFactory();
		ParamsType paramsType = factory.createParamsType();
		result.setParams(paramsType);

		EntryType entryType;
		Set<Entry<String, Element>> set = params.entrySet();
		for (Entry<String, Element> entry : set) {
			entryType = factory.createEntryType();
			entryType.setKey(entry.getKey());
			entryType.setEntryValue(new JAXBElement<>(EntryType.F_ENTRY_VALUE, Element.class, entry.getValue()));

			paramsType.getEntry().add(entryType);
		}

		return result;
	}

	public static OperationResultType createOperationResult(String operation, OperationResultStatusType status) {
		if (StringUtils.isEmpty(operation)) {
			throw new IllegalArgumentException("Operation name not defined.");
		}
		if (status == null) {
			throw new IllegalArgumentException("Operation status not defined.");
		}

		ObjectFactory factory = new ObjectFactory();
		OperationResultType result = factory.createOperationResultType();
		result.setToken(getNextToken());
		result.setOperation(operation);
		result.setStatus(status);

		return result;
	}
}
