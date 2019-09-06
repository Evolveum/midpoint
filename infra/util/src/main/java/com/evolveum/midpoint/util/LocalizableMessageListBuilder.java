/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public class LocalizableMessageListBuilder {
	private List<LocalizableMessage> messages = new ArrayList<>();
	private LocalizableMessage separator;
	private LocalizableMessage prefix;
	private LocalizableMessage postfix;

	public LocalizableMessageListBuilder message(LocalizableMessage message) {
		messages.add(message);
		return this;
	}

	public void addMessage(LocalizableMessage message) {
		messages.add(message);
	}

	public LocalizableMessageListBuilder messages(Collection<LocalizableMessage> messages) {
		this.messages.addAll(messages);
		return this;
	}

	public LocalizableMessageListBuilder separator(LocalizableMessage value) {
		separator = value;
		return this;
	}

	public LocalizableMessageListBuilder prefix(LocalizableMessage value) {
		prefix = value;
		return this;
	}

	public LocalizableMessageListBuilder postfix(LocalizableMessage value) {
		postfix = value;
		return this;
	}

	public LocalizableMessageList build() {
		return new LocalizableMessageList(messages, separator, prefix, postfix);
	}

	// beware, ignores prefix and postfix for singleton lists
	public LocalizableMessage buildOptimized() {
		if (messages.size() == 1) {
			return messages.get(0);
		} else {
			return build();
		}
	}
}
