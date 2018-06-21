package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;

public class NonceAuthenticationContext extends AbstractAuthenticationContext {

	private String nonce;
	private NonceCredentialsPolicyType policy;

	public NonceAuthenticationContext(String username, String nonce, NonceCredentialsPolicyType policy) {
		super(username);
		this.nonce = nonce;
		this.policy = policy;
	}

	public String getNonce() {
		return nonce;
	}

	public NonceCredentialsPolicyType getPolicy() {
		return policy;
	}

	@Override
	public Object getEnteredCredential() {
		return getNonce();
	}
}
