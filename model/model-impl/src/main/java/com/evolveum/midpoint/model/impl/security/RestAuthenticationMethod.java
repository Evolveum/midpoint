package com.evolveum.midpoint.model.impl.security;

import org.apache.commons.lang.StringUtils;

public enum RestAuthenticationMethod {

	BASIC("Basic"),
	SECURITY_QUESTIONS("SecQ"),
	CLUSTER("Cluster");
	

	private String method;

	private RestAuthenticationMethod(String method) {
		this.method = method;
	}

	public String getMethod() {
		return method;
	}

	protected boolean equals(String authenticationType) {
		if (StringUtils.isBlank(authenticationType)) {
			return false;
		}

		if (getMethod().equals(authenticationType)) {
			return true;
		}
		return false;
	}
}
