package com.evolveum.midpoint.model.api.authentication;

public interface NodeAuthenticationEvaluator {

	boolean authenticate(String remoteName, String remoteAddress, String credentials, String operation);

}
