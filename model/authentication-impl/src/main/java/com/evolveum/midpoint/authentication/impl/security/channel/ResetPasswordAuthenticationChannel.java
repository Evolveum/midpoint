/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

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

    public String getPathAfterSuccessfulAuthentication() {
        return "/resetPassword";
    }

    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities) {
        ArrayList<Authorization> newAuthorities = new ArrayList<Authorization>();
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
