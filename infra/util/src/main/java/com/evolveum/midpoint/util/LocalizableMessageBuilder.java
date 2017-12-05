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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author semancik
 *
 */
public class LocalizableMessageBuilder {

	private String key;
	private final List<Object> args = new ArrayList<>();
	private String fallbackMessage;
	private LocalizableMessage fallbackLocalizableMessage;

	public LocalizableMessageBuilder() {
	}

	public LocalizableMessageBuilder key(String key) {
		this.key = key;
		return this;
	}

	public static SingleLocalizableMessage buildKey(String key) {
		return new SingleLocalizableMessage(key, null, (SingleLocalizableMessage) null);
	}

	public LocalizableMessageBuilder args(Object... args) {
		Collections.addAll(this.args, args);
		return this;
	}

	public LocalizableMessageBuilder args(List<Object> args) {
		this.args.addAll(args);
		return this;
	}

	public LocalizableMessageBuilder arg(Object arg) {
		this.args.add(arg);
		return this;
	}

	public LocalizableMessageBuilder fallbackMessage(String fallbackMessage) {
		this.fallbackMessage = fallbackMessage;
		return this;
	}

	public LocalizableMessageBuilder fallbackLocalizableMessage(LocalizableMessage fallbackLocalizableMessage) {
		this.fallbackLocalizableMessage = fallbackLocalizableMessage;
		return this;
	}

	public static SingleLocalizableMessage buildFallbackMessage(String fallbackMessage) {
		return new SingleLocalizableMessage(null, null, fallbackMessage);
	}

	public SingleLocalizableMessage build() {
		if (fallbackMessage != null) {
			if (fallbackLocalizableMessage != null) {
				throw new IllegalStateException("fallbackMessage and fallbackLocalizableMessage cannot be both set");
			}
			return new SingleLocalizableMessage(key, args.toArray(), fallbackMessage);
		} else {
			return new SingleLocalizableMessage(key, args.toArray(), fallbackLocalizableMessage);
		}
	}
}
