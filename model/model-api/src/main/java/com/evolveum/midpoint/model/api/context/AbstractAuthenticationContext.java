/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

public abstract class AbstractAuthenticationContext {

	private String username;

	public String getUsername() {
		return username;
	}

	public AbstractAuthenticationContext(String username) {
		this.username = username;
	}

	public abstract Object getEnteredCredential();

}
