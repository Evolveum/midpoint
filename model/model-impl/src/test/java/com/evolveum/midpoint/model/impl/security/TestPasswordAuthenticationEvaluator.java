package com.evolveum.midpoint.model.impl.security;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;

public class TestPasswordAuthenticationEvaluator extends TestAbstractAuthenticationEvaluator<String, PasswordAuthenticationContext, AuthenticationEvaluator<PasswordAuthenticationContext>>{

	@Autowired(required=true)
	private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;
	
	@Override
	public AuthenticationEvaluator<PasswordAuthenticationContext> getAuthenticationEvaluator() {
		return passwordAuthenticationEvaluator;
	}

	@Override
	public PasswordAuthenticationContext getAuthenticationContext(String username, String value) {
		return new PasswordAuthenticationContext(username, value);
	}

	@Override
	public String getGoodPasswordJack() {
		return USER_JACK_PASSWORD;
	}

	@Override
	public String getBadPasswordJack() {
		return "this IS NOT myPassword!";
	}

	@Override
	public String getGoodPasswordGuybrush() {
		return USER_GUYBRUSH_PASSWORD;
	}

	@Override
	public String getBadPasswordGuybrush() {
		return "thisIsNotMyPassword";
	}

	@Override
	public String get103EmptyPasswordJack() {
		return "";
	}

}
