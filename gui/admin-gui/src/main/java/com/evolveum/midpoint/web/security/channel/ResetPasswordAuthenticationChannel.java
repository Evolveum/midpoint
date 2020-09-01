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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author skublik
 */

public class ResetPasswordAuthenticationChannel extends AuthenticationChannelImpl {


    public ResetPasswordAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_RESET_PASSWORD_URI;
    }

    public String getPathAfterUnsuccessfulAuthentication() {
        return PageResetPassword.URL;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return PageResetPassword.URL;
    }

    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities) {
        ArrayList<Authorization> newAuthorities = new ArrayList<Authorization>();
//        for (GrantedAuthority authority : authorities) {
//            List<String> authoritiesString = new ArrayList<String>();
//            if (authority instanceof Authorization) {
//                Authorization clone = ((Authorization) authority).clone();
//                authoritiesString = clone.getAction();
//                List<String> newAction = new ArrayList<String>();
//                for (String authorityString : authoritiesString) {
//                    if (AuthorizationConstants.AUTZ_ALL_URL.equals(authorityString)) {
//                        authoritiesString.remove(authorityString);
//                        newAction.add(PageResetPassword.AUTH_SELF_ALL_URI);
//                        newAction.add(AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL);
//                    }
//                    if (authority.getAuthority().startsWith(AuthorizationConstants.NS_AUTHORIZATION_REST)) {
//                        newAction.add(AuthorizationConstants.NS_AUTHORIZATION_REST);
//                    }
//                    if (authority.getAuthority().equals(AuthorizationConstants.AUTZ_ALL_URL)) {
//                        newAction.add(AuthorizationConstants.AUTZ_ALL_URL);
//                    }
//                }
//                if (!newAction.isEmpty()) {
//                    clone.getAction().clear();
//                    clone.getAction().addAll(newAction);
//                    newAuthorities.add(clone);
//                }
//            } else {
//                if (AuthorizationConstants.AUTZ_ALL_URL.equals(authority.getAuthority())) {
//                    newAuthorities.add(new SimpleGrantedAuthority(PageResetPassword.AUTH_SELF_ALL_URI));
//                    newAuthorities.add(new SimpleGrantedAuthority(AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL));
//                }
//                if (PageResetPassword.AUTH_SELF_ALL_URI.equals(authority.getAuthority())) {
//                    newAuthorities.add(authority);
//                }
//                if (AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL.equals(authority.getAuthority())) {
//                    newAuthorities.add(authority);
//                }
//            }
//
//        }
        if (authorities != null) {
            Iterator<Authorization> authzIterator = authorities.iterator();
            while (authzIterator.hasNext()) {
                Authorization authzI= authzIterator.next();
                Iterator<String> actionIterator = authzI.getAction().iterator();
                while (actionIterator.hasNext()) {
                    String action = actionIterator.next();
                    if (action.contains(AuthorizationConstants.NS_AUTHORIZATION_UI)) {
                        actionIterator.remove();
                    }
                }
            }
        }
        AuthorizationType authorizationType = new AuthorizationType();
        authorizationType.getAction().add(AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL);
        Authorization selfServiceCredentialsAuthz = new Authorization(authorizationType);
        newAuthorities.add(selfServiceCredentialsAuthz);
        authorities.addAll(newAuthorities);
        return authorities;
    }
}
