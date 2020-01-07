/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @author skublik
 */

public interface AuthenticationChannel {

    public final static String DEFAULT_POST_AUTHENTICATION_URL = "/self/dashboard";

    public String getChannelId();

    public String getPathAfterSuccessfulAuthentication();

    public String getPathDuringProccessing();

    public boolean isDefault();

    public Collection<? extends GrantedAuthority> resolveAuthorities(Collection<? extends GrantedAuthority> authorities);
}
