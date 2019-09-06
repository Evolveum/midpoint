/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is final and must remain so, as it's not cloneable.
 */
public class LocalizableMessageList implements LocalizableMessage {

	public static final LocalizableMessage SPACE = LocalizableMessageBuilder.buildFallbackMessage(" ");
	public static final LocalizableMessage COMMA = LocalizableMessageBuilder.buildFallbackMessage(", ");
	public static final LocalizableMessage SEMICOLON = LocalizableMessageBuilder.buildFallbackMessage("; ");

	private final List<LocalizableMessage> messages;
	private final LocalizableMessage separator;
	private final LocalizableMessage prefix;
	private final LocalizableMessage postfix;

	public LocalizableMessageList(List<LocalizableMessage> messages, LocalizableMessage separator, LocalizableMessage prefix, LocalizableMessage postfix) {
		this.messages = messages;
		this.separator = separator;
		this.prefix = prefix;
		this.postfix = postfix;
	}

	public List<LocalizableMessage> getMessages() {
		return Collections.unmodifiableList(messages);
	}

	public LocalizableMessage getSeparator() {
		return separator;
	}

	public LocalizableMessage getPrefix() {
		return prefix;
	}

	public LocalizableMessage getPostfix() {
		return postfix;
	}

	@Override
	public String getFallbackMessage() {
		String msg = messages.stream()
				.filter(m -> m.getFallbackMessage() != null)
				.map(m -> m.getFallbackMessage())
				.collect(Collectors.joining("; "));
		if (!msg.isEmpty()) {
			return msg;
		} else {
			return messages.size() + " message(s)";
		}
	}

	@Override
	public void shortDump(StringBuilder sb) {
		boolean first = true;
		for (LocalizableMessage message : messages) {
			if (first) {
				first = false;
			} else {
				sb.append("; ");
			}
			message.shortDump(sb);
		}
	}

	@Override
	public boolean isEmpty() {
		return LocalizableMessage.isEmpty(prefix)
				&& LocalizableMessage.isEmpty(postfix)
				&& messages.stream().allMatch(m -> m.isEmpty());
	}

	@Override
	public String toString() {
		return "LocalizableMessageList(" + messages +
				(isNonTrivial(separator) ? ", separator=" + separator : "") +
				(isNonTrivial(prefix) ? ", prefix=" + prefix : "") +
				(isNonTrivial(postfix) ? ", postfix=" + postfix : "")
				+ ')';
	}

	private boolean isNonTrivial(LocalizableMessage msg) {
		if (msg == null) {
			return false;
		}
		if (!(msg instanceof SingleLocalizableMessage)) {
			return true;
		}
		SingleLocalizableMessage single = (SingleLocalizableMessage) msg;
		if (single.getKey() != null || single.getFallbackLocalizableMessage() != null) {
			return true;
		}
		// a value more complex than "; " or ", " or ". " or "[" or "]" or something like that
		return single.getFallbackMessage() != null && single.getFallbackMessage().length() > 2;
	}
}
