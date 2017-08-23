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

import java.util.List;

/**
 * @author semancik
 *
 */
public class LocalizableMessageBuilder {
	
	private String key;
	private Object[] args;	
	private String fallbackMessage;

	public LocalizableMessageBuilder() {
		super();
	}

	public void key(String key) {
		this.key = key;
	}
	
	public static LocalizableMessage buildKey(String key) {
		return new LocalizableMessage(key, null, null);
	}

	public void args(Object... args) {
		this.args = args;
	}
	
	public void args(List<Object> args) {
		this.args = args.toArray();
	}

	public void fallbackMessage(String fallbackMessage) {
		this.fallbackMessage = fallbackMessage;
	}
	
	public static LocalizableMessage buildFallbackMessage(String fallbackMessage) {
		return new LocalizableMessage(null, null, fallbackMessage);
	}

	public LocalizableMessage build() {
		return new LocalizableMessage(key, args, fallbackMessage);
	}
}
