package com.evolveum.midpoint.model.impl.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

public class NodeAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;
	
	private PrismObject<NodeType> node;
	private String remoteAddress;

	public NodeAuthenticationToken(PrismObject<NodeType> node, String remoteAddress, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.node = node;
		this.remoteAddress = remoteAddress;
	}

	@Override
	public Object getCredentials() {
		return remoteAddress;
	}

	@Override
	public PrismObject<NodeType> getPrincipal() {
		return node;
	}

}
