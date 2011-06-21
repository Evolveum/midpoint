package com.evolveum.midpoint.provisioning.ucf.impl;

import com.evolveum.midpoint.provisioning.ucf.api.Token;

public class TokenImpl implements Token{

	private String serializedToken;
	
	
	public TokenImpl() {
	}

	public TokenImpl(String serializedToken) {
		this.serializedToken = serializedToken;
	}

	@Override
	public String serialize() {
		return serializedToken;
	}
	
	

	@Override
	public String toString() {
		return "TokenImpl [serializedToken=" + serializedToken + "]";
	}

	public String getSerializedToken() {
		return serializedToken;
	}

	public void setSerializedToken(String serializedToken) {
		this.serializedToken = serializedToken;
	}
	
		
}
