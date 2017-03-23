package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;

public class PasswordAuthenticationContext extends AbstractAuthenticationContext {
	
	private String password;
	
	public String getPassword() {
		return password;
	}
	
	public PasswordAuthenticationContext(String username, String password) {
		super(username);
		this.password = password;
	}

	@Override
	public Object getEnteredCredential() {
		return getPassword();
	}
	
}
