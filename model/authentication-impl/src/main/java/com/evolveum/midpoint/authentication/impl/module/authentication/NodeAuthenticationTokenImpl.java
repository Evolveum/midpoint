/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import java.util.Collection;

import com.evolveum.midpoint.authentication.api.config.NodeAuthenticationToken;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

public class NodeAuthenticationTokenImpl extends AbstractAuthenticationToken implements NodeAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final PrismObject<NodeType> node;
    private final String remoteAddress;

    public NodeAuthenticationTokenImpl(PrismObject<NodeType> node, String remoteAddress, Collection<? extends GrantedAuthority> authorities) {
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
