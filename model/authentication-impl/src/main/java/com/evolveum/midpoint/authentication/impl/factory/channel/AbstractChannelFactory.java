/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.channel;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */

public abstract class AbstractChannelFactory {

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Autowired
    private AuthChannelRegistryImpl registry;

    public AuthChannelRegistryImpl getRegistry() {
        return registry;
    }

    public abstract boolean match(String channelId);

    public abstract AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) throws Exception;

    protected Integer getOrder(){
        return 0;
    }

}
