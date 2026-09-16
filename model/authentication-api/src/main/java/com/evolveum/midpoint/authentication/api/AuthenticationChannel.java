/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import java.util.List;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

import org.jetbrains.annotations.NotNull;
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

    /**
     * Filters an authorization the principal got from its assignments for use in this channel.
     * Returns the (possibly reduced) authorization, or null to drop it in this channel.
     */
    @Nullable Authorization resolveAuthorization(Authorization autz);

    /**
     * Single authorization granted by the channel itself regardless of assignments, typically access to the page
     * the channel leads to. Null if the channel grants nothing. See also {@link #getAdditionalAuthorities()}.
     */
    @Nullable Authorization getAdditionalAuthority();

    /**
     * Authorizations granted to the principal by the channel itself (in addition to the ones from assignments).
     * Channels that need more than one authorization override this method, the default is the single
     * {@link #getAdditionalAuthority()} value.
     */
    default @NotNull List<Authorization> getAdditionalAuthorities() {
        Authorization single = getAdditionalAuthority();
        return single == null ? List.of() : List.of(single);
    }
}
