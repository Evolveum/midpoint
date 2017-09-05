/**
 * Copyright (c) 2017 Evolveum
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

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author semancik
 *
 */
public class LocalizableMessage implements Serializable, ShortDumpable {
	private static final long serialVersionUID = 1L;

	final private String key;
	final private Object[] args;
	final private String fallbackMessage;

	public LocalizableMessage(String key, Object[] args, String fallbackMessage) {
		super();
		this.key = key;
		this.args = args;
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
	public String getFallbackMessage() {
		return fallbackMessage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + ((fallbackMessage == null) ? 0 : fallbackMessage.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LocalizableMessage other = (LocalizableMessage) obj;
		if (!Arrays.equals(args, other.args)) {
			return false;
		}
		if (fallbackMessage == null) {
			if (other.fallbackMessage != null) {
				return false;
			}
		} else if (!fallbackMessage.equals(other.fallbackMessage)) {
			return false;
		}
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "LocalizableMessage(" + key + ": " + Arrays.toString(args) + " ("
				+ fallbackMessage + "))";
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
		} else {
			sb.append(fallbackMessage);
		};
	}

}
