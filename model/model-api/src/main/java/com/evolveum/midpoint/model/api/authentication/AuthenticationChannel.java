/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * @author skublik
 */

public interface AuthenticationChannel {

    public void setPathAfterLogout(String pathAfterLogout);

    public String getPathAfterLogout();

    public boolean matchChannel(AuthenticationSequenceType sequence);

    public String getChannelId();

    public String getPathAfterSuccessfulAuthentication();

    public String getPathAfterUnsuccessfulAuthentication();

    public String getPathDuringProccessing();

    public boolean isDefault();

    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities);

    public void postSuccessAuthenticationProcessing();

    public String getSpecificLoginUrl();

    public boolean isSupportActivationByChannel();

    public String getUrlSuffix();
}
