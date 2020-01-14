/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.forgetpassword.PageResetPassword;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author skublik
 */

public class RestAuthenticationChannel extends AuthenticationChannelImpl {


    public RestAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_REST_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/ws/rest/self";
    }

    public Collection<? extends GrantedAuthority> resolveAuthorities(Collection<? extends GrantedAuthority> authorities) {
        ArrayList<GrantedAuthority> newAuthorities = new ArrayList<GrantedAuthority>();
        for (GrantedAuthority authority : authorities) {
            List<String> authoritiesString = new ArrayList<String>();
            if (authority instanceof Authorization) {
                authoritiesString = ((Authorization)authority).getAction();
            } else {
                authoritiesString.add(authority.getAuthority());
            }
            if (authoritiesString != null) {
                for (String authorityString : authoritiesString) {
                    if (authorityString.startsWith(AuthorizationConstants.NS_AUTHORIZATION_REST)) {
                        newAuthorities.add(authority);
                    }
                }
                if (authoritiesString.contains(AuthorizationConstants.AUTZ_ALL_URL)) {
                    newAuthorities.add(authority);
                }
            }
        }
        return newAuthorities;
    }
}
