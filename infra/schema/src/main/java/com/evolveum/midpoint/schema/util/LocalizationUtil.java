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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleLocalizableMessageType;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * @author mederly
 */
public class LocalizationUtil {

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
			SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH);

	// consider using LocalizationService instead
	public static String resolve(String key) {
		if (key != null && RESOURCE_BUNDLE.containsKey(key)) {
			return RESOURCE_BUNDLE.getString(key);
		} else {
			return key;
		}
	}

	// consider using LocalizationService instead
	public static String resolve(String key, Object... params  ) {
		if (key != null && RESOURCE_BUNDLE.containsKey(key)) {
			return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
		} else {
			return key;
		}
	}

	public static LocalizableMessageType createLocalizableMessageType(LocalizableMessage message) {
		return createLocalizableMessageType(message, null);
	}
	
	public static LocalizableMessageType createLocalizableMessageType(LocalizableMessage message, Function<LocalizableMessage, String> resolveKeys) {
		if (message == null) {
			return null;
		} else if (message instanceof SingleLocalizableMessage) {
			return createLocalizableMessageType((SingleLocalizableMessage) message, resolveKeys);
		} else if (message instanceof LocalizableMessageList) {
			return createLocalizableMessageType((LocalizableMessageList) message, resolveKeys);
		} else {
			throw new AssertionError("Unsupported localizable message type: " + message);
		}
	}

	@NotNull
	private static LocalizableMessageListType createLocalizableMessageType(@NotNull LocalizableMessageList messageList, Function<LocalizableMessage, String> resolveKeys) {
		LocalizableMessageListType rv = new LocalizableMessageListType();
		messageList.getMessages().forEach(message -> {if (rv.getMessage() != null) rv.getMessage().add(createLocalizableMessageType(message, resolveKeys));});
		rv.setSeparator(createLocalizableMessageType(messageList.getSeparator(), resolveKeys));
		rv.setPrefix(createLocalizableMessageType(messageList.getPrefix(), resolveKeys));
		rv.setPostfix(createLocalizableMessageType(messageList.getPostfix(), resolveKeys));
		return rv;
	}

	@NotNull
	private static SingleLocalizableMessageType createLocalizableMessageType(@NotNull SingleLocalizableMessage message, Function<LocalizableMessage, String> resolveKeys) {
		SingleLocalizableMessageType rv = new SingleLocalizableMessageType();
		rv.setKey(message.getKey());
		if (message.getArgs() != null) {
			for (Object argument : message.getArgs()) {
				LocalizableMessageArgumentType messageArgument;
				if (argument instanceof LocalizableMessage) {
					messageArgument = new LocalizableMessageArgumentType()
								.localizable(createLocalizableMessageType(((LocalizableMessage) argument), resolveKeys));
				} else {
					messageArgument = new LocalizableMessageArgumentType().value(argument != null ? argument.toString() : null);
				}
				rv.getArgument().add(messageArgument);
			}
		}
		if (message.getFallbackLocalizableMessage() != null) {
			rv.setFallbackLocalizableMessage(createLocalizableMessageType(message.getFallbackLocalizableMessage(), resolveKeys));
		}
		
		if (StringUtils.isBlank(message.getFallbackMessage()) && resolveKeys != null) {
			rv.setFallbackMessage(resolveKeys.apply(message));
		} else {
			rv.setFallbackMessage(message.getFallbackMessage());
		}
		return rv;
	}

	public static LocalizableMessageType createForFallbackMessage(String fallbackMessage) {
		return new SingleLocalizableMessageType().fallbackMessage(fallbackMessage);
	}

	public static LocalizableMessageType createForKey(String key) {
		return new SingleLocalizableMessageType().key(key);
	}

	public static LocalizableMessage toLocalizableMessage(@NotNull LocalizableMessageType message) {
		if (message instanceof SingleLocalizableMessageType) {
			return toLocalizableMessage((SingleLocalizableMessageType) message);
		} else if (message instanceof LocalizableMessageListType) {
			return toLocalizableMessage((LocalizableMessageListType) message);
		} else {
			throw new AssertionError("Unknown LocalizableMessageType type: " + message);
		}
	}

	public static LocalizableMessage toLocalizableMessage(@NotNull SingleLocalizableMessageType message) {
		LocalizableMessage fallbackLocalizableMessage;
		if (message.getFallbackLocalizableMessage() != null) {
			fallbackLocalizableMessage = toLocalizableMessage(message.getFallbackLocalizableMessage());
		} else {
			fallbackLocalizableMessage = null;
		}
		if (message.getKey() == null && message.getFallbackMessage() == null) {
			return fallbackLocalizableMessage;
		} else {
			return new LocalizableMessageBuilder()
					.key(message.getKey())
					.args(convertLocalizableMessageArguments(message.getArgument()))
					.fallbackMessage(message.getFallbackMessage())
					.fallbackLocalizableMessage(fallbackLocalizableMessage)
					.build();
		}
	}

	private static LocalizableMessage toLocalizableMessage(@NotNull LocalizableMessageListType messageList) {
		LocalizableMessageListBuilder builder = new LocalizableMessageListBuilder();
		for (LocalizableMessageType m : messageList.getMessage()) {
			builder.addMessage(toLocalizableMessage(m));
		}
		
		if (messageList.getSeparator() != null) {
			builder = builder.separator(toLocalizableMessage(messageList.getSeparator()));
		}
		
		if (messageList.getPostfix() != null) {
			builder = builder.postfix(toLocalizableMessage(messageList.getPostfix()));
		}
		
		if (messageList.getPrefix() != null) {
			builder = builder.postfix(toLocalizableMessage(messageList.getPostfix()));
		}
		return builder.build();
		
	}

	private static List<Object> convertLocalizableMessageArguments(List<LocalizableMessageArgumentType> arguments) {
		List<Object> rv = new ArrayList<>();
		for (LocalizableMessageArgumentType argument : arguments) {
			if (argument.getLocalizable() != null) {
				rv.add(toLocalizableMessage(argument.getLocalizable()));
			} else {
				rv.add(argument.getValue());        // may be null
			}
		}
		return rv;
	}

	// migration-related method: if localizable message is present, defaultValue is ignored (i.e. it is not the same as fallbackMessage in LocalizableMessageType)
	public static LocalizableMessageType getLocalizableMessageOrDefault(LocalizableMessageType localizableMessage, String defaultValue) {
		return localizableMessage != null ? localizableMessage : new SingleLocalizableMessageType().fallbackMessage(defaultValue);
	}
}
