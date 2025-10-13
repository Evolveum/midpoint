/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import java.util.Collection;
import java.util.Collections;

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
    public Authorization resolveAuthorization(Authorization autz) {
        return null;
    }

    @Override
    public Authorization getAdditionalAuthority() {
        return new Authorization(new AuthorizationType().action(AuthorizationConstants.AUTZ_UI_INVITATION_URL));
    }
}
