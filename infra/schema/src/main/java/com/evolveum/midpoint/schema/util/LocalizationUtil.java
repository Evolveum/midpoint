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
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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
		LocalizableMessageType rv = new LocalizableMessageType();
		rv.setKey(message.getKey());
		if (message.getArgs() != null) {
			for (Object argument : message.getArgs()) {
				LocalizableMessageArgumentType messageArgument;
				if (argument instanceof LocalizableMessage) {
					messageArgument = new LocalizableMessageArgumentType()
								.localizable(createLocalizableMessageType(((LocalizableMessage) argument)));
				} else {
					messageArgument = new LocalizableMessageArgumentType().value(argument != null ? argument.toString() : null);
				}
				rv.getArgument().add(messageArgument);
			}
		}
		if (message.getFallbackLocalizableMessage() != null) {
			rv.setFallbackLocalizableMessage(createLocalizableMessageType(message.getFallbackLocalizableMessage()));
		}
		rv.setFallbackMessage(message.getFallbackMessage());
		return rv;
	}

	public static LocalizableMessageType createForFallbackMessage(String fallbackMessage) {
		return new LocalizableMessageType().fallbackMessage(fallbackMessage);
	}

	public static LocalizableMessageType createForKey(String key) {
		return new LocalizableMessageType().key(key);
	}

	public static LocalizableMessage parseLocalizableMessageType(@NotNull LocalizableMessageType message) {
		return parseLocalizableMessageType(message, null);
	}

	public static LocalizableMessage parseLocalizableMessageType(@NotNull LocalizableMessageType message, LocalizableMessage defaultMessage) {
		LocalizableMessage fallbackLocalizableMessage;
		if (message.getFallbackLocalizableMessage() != null) {
			fallbackLocalizableMessage = parseLocalizableMessageType(message.getFallbackLocalizableMessage(), defaultMessage);
		} else {
			fallbackLocalizableMessage = defaultMessage;
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

	private static List<Object> convertLocalizableMessageArguments(List<LocalizableMessageArgumentType> arguments) {
		List<Object> rv = new ArrayList<>();
		for (LocalizableMessageArgumentType argument : arguments) {
			if (argument.getLocalizable() != null) {
				rv.add(parseLocalizableMessageType(argument.getLocalizable(), null));
			} else {
				rv.add(argument.getValue());        // may be null
			}
		}
		return rv;
	}

	public static boolean isEmpty(LocalizableMessage message) {
		return message == null || message.getKey() == null && message.getFallbackLocalizableMessage() == null && message.getFallbackMessage() == null;
	}
}
