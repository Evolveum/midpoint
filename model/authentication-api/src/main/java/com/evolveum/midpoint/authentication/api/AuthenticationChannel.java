/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for define channel of authentication, channel define scope of authentication etc. rest, gui, reset password ...
 *
 * @author skublik
 */

public interface AuthenticationChannel {

    void setPathAfterLogout(String pathAfterLogout);

    String getPathAfterLogout();

    boolean matchChannel(AuthenticationSequenceType sequence);

    String getChannelId();

    String getPathAfterSuccessfulAuthentication();

    String getPathAfterUnsuccessfulAuthentication();

    String getPathDuringProccessing();

    boolean isDefault();

    void postSuccessAuthenticationProcessing();

    String getSpecificLoginUrl();

    boolean isSupportActivationByChannel();

    boolean isSupportGuiConfigByChannel();

    String getUrlSuffix();

    boolean isPostAuthenticationEnabled();

    @Nullable Authorization resolveAuthorization(Authorization autz);

    @Nullable Authorization getAdditionalAuthority();
}
