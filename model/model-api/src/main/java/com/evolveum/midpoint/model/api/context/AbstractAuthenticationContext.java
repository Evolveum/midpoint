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
