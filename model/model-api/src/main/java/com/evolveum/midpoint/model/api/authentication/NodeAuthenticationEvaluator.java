package com.evolveum.midpoint.model.api.authentication;

public interface NodeAuthenticationEvaluator {

	
	public boolean authenticate(String remoteName, String remoteAddress, String operation);
}
