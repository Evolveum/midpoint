/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.logging;

/**
 *
 */
public class LoggedEvent {

	private final String text;

	LoggedEvent(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
