/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author skublik
 */

public class InvitationAuthenticationChannel extends AuthenticationChannelImpl {

    public InvitationAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_INVITATION_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/invitation";
    }

    public String getPathAfterUnsuccessfulAuthentication() {
        return "/";
    }

    @Override
    public String getSpecificLoginUrl() {
        return "/";
    }

    @Override
    public boolean isSupportActivationByChannel() {
        return false;
    }

    @Override
    public String getPathDuringProccessing() {
        return "/";
    }

    @Override
    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities) {
        ArrayList<Authorization> newAuthorities = new ArrayList<>();
        AuthorizationType authorizationBean = new AuthorizationType();
        authorizationBean.getAction().add(AuthorizationConstants.AUTZ_UI_INVITATION_URL);
        Authorization selfServiceCredentialsAuthz = new Authorization(authorizationBean);
        newAuthorities.add(selfServiceCredentialsAuthz);
        authorities.addAll(newAuthorities);
        return authorities;
    }
}
