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
package com.evolveum.midpoint.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author semancik
 *
 */
public class SingleLocalizableMessage implements LocalizableMessage {
	private static final long serialVersionUID = 1L;

	final private String key;
	final private Object[] args;
	// at most one of the following can be present
	final private LocalizableMessage fallbackLocalizableMessage;
	final private String fallbackMessage;

	public SingleLocalizableMessage(String key, Object[] args, LocalizableMessage fallbackLocalizableMessage) {
		super();
		this.key = key;
		this.args = args;
		this.fallbackLocalizableMessage = fallbackLocalizableMessage;
		this.fallbackMessage = null;
	}

	public SingleLocalizableMessage(String key, Object[] args, String fallbackMessage) {
		super();
		this.key = key;
		this.args = args;
		this.fallbackLocalizableMessage = null;
		this.fallbackMessage = fallbackMessage;
	}

	/**
	 * Message key. This is the key in localization files that
	 * determine message or message template.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Message template arguments.
	 */
	public Object[] getArgs() {
		return args;
	}

	/**
	 * Fallback message. This message is used in case that the
	 * message key cannot be found in the localization files.
	 */
	@Override
    public String getFallbackMessage() {
		return fallbackMessage;
	}

	/**
	 * Fallback localization message. This message is used in case that the
	 * message key cannot be found in the localization files.
	 */
	public LocalizableMessage getFallbackLocalizableMessage() {
		return fallbackLocalizableMessage;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SingleLocalizableMessage))
			return false;
		SingleLocalizableMessage that = (SingleLocalizableMessage) o;
		return Objects.equals(key, that.key) &&
				Arrays.equals(args, that.args) &&
				Objects.equals(fallbackLocalizableMessage, that.fallbackLocalizableMessage) &&
				Objects.equals(fallbackMessage, that.fallbackMessage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, args, fallbackLocalizableMessage, fallbackMessage);
	}

	@Override
	public String toString() {
		return "SingleLocalizableMessage(" + key + ": " + Arrays.toString(args) + " ("
				+ (fallbackMessage != null ? fallbackMessage : fallbackLocalizableMessage) + "))";
	}

	@Override
	public void shortDump(StringBuilder sb) {
		if (key != null) {
			sb.append(key);
			if (args != null) {
				sb.append(": ");
				sb.append(Arrays.toString(args));
			}
			if (fallbackMessage != null) {
				sb.append(" (");
				sb.append(fallbackMessage);
				sb.append(")");
			}
			if (fallbackLocalizableMessage != null) {
				sb.append(" (");
				sb.append(fallbackLocalizableMessage.shortDump());
				sb.append(")");
			}
		} else {
			sb.append(fallbackLocalizableMessage != null ? fallbackLocalizableMessage.shortDump() : fallbackMessage);
		}
	}

	@Override
	public boolean isEmpty() {
		return key == null && LocalizableMessage.isEmpty(fallbackLocalizableMessage) && fallbackMessage == null;
	}
}
