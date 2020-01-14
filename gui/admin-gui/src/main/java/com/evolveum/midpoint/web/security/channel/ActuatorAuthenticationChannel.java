/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author skublik
 */

public class ActuatorAuthenticationChannel extends AuthenticationChannelImpl {


    public ActuatorAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_ACTUATOR_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/actuator";
    }

    public Collection<? extends GrantedAuthority> resolveAuthorities(Collection<? extends GrantedAuthority> authorities) {
        ArrayList<GrantedAuthority> newAuthorities = new ArrayList<GrantedAuthority>();
        for (GrantedAuthority authority : authorities) {
            if (authority != null && authority.getAuthority() != null
            && authority.getAuthority().startsWith(AuthorizationConstants.NS_AUTHORIZATION_ACTUATOR)) {
                newAuthorities.add(authority);
            }
            if (AuthorizationConstants.AUTZ_ALL_URL.equals(authority.getAuthority())) {
                newAuthorities.add(authority);
            }
        }
        return authorities;
    }
}
