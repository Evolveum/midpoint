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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class LoginRecoveryAuthenticationChannel extends AuthenticationChannelImpl {

    public LoginRecoveryAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_LOGIN_RECOVERY_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/loginRecovery";
    }

//    public Collection<Authorization> resolveAuthorities(@NotNull Collection<Authorization> authorities) {
//        ArrayList<Authorization> newAuthorities = new ArrayList<>();
//        for (Authorization authzI : authorities) {
//            authzI.getAction().removeIf(action -> action.contains(AuthorizationConstants.NS_AUTHORIZATION_UI));
//        }
//        AuthorizationType authorizationBean = new AuthorizationType();
//        authorizationBean.getAction().add(AuthorizationConstants.AUTZ_UI_USERNAME_RECOVERY_URL);
//        Authorization selfServiceCredentialsAuthz = new Authorization(authorizationBean);
//        newAuthorities.add(selfServiceCredentialsAuthz);
//        newAuthorities.addAll(authorities);
//        return Collections.unmodifiableList(newAuthorities);
//    }

}
