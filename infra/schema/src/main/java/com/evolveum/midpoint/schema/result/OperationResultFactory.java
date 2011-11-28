/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.result;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.xml.ns._public.common.common_1.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ParamsType;

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
		LocalizedMessageType localizedMessageType = factory.createLocalizedMessageType();
		result.setLocalizedMessage(localizedMessageType);
		localizedMessageType.setKey(localizedMessage);
		if (localizedArguments == null || localizedArguments.length == 0) {
			return result;
		}

		for (Object object : localizedArguments) {
			localizedMessageType.getArgument().add(object);
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
			entryType.setAny(entry.getValue());

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
