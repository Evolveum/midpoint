/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.forgetpassword.PageResetPassword;
import com.evolveum.midpoint.web.page.forgetpassword.PageShowPassword;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author skublik
 */

public class ResetPasswordAuthenticationChannel extends AuthenticationChannelImpl {


    public ResetPasswordAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_GUI_RESET_PASSWORD_URI;
    }

    public String getPathAfterUnsuccessfulAuthentication() {
        return PageResetPassword.URL;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return PageShowPassword.URL;
    }

    public Collection<? extends GrantedAuthority> resolveAuthorities(Collection<? extends GrantedAuthority> authorities) {
        ArrayList<GrantedAuthority> newAuthorities = new ArrayList<GrantedAuthority>();
        for (GrantedAuthority authority : authorities) {
            if (AuthorizationConstants.AUTZ_ALL_URL.equals(authority.getAuthority())) {
                newAuthorities.add(new SimpleGrantedAuthority(PageResetPassword.AUTH_SELF_ALL_URI));
                newAuthorities.add(new SimpleGrantedAuthority(AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL));
            }
            if (PageResetPassword.AUTH_SELF_ALL_URI.equals(authority.getAuthority())) {
                newAuthorities.add(authority);
            }
            if (AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL.equals(authority.getAuthority())) {
                newAuthorities.add(authority);
            }
        }
        return newAuthorities;
    }
}
