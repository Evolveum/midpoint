/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collection;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class AuthenticationChannelImpl implements AuthenticationChannel {

    private AuthenticationSequenceChannelType channel;

    public AuthenticationChannelImpl(AuthenticationSequenceChannelType channel) {
        Validate.notNull(channel, "Couldn't create authentication channel object, because channel is null");
        this.channel = channel;
    }

    public AuthenticationChannelImpl() {
        this.channel = new AuthenticationSequenceChannelType();
        this.channel.setChannelId(SecurityPolicyUtil.DEFAULT_CHANNEL);
    }

    protected AuthenticationSequenceChannelType getChannel() {
        return channel;
    }

    public String getChannelId() {
        return channel.getChannelId();
    }

    public String getPathAfterSuccessfulAuthentication() {
        return AuthenticationChannel.DEFAULT_POST_AUTHENTICATION_URL;
    }

    public String getPathAfterUnsuccessfulAuthentication() {
        return getPathAfterSuccessfulAuthentication();
    }

    @Override
    public String getPathDuringProccessing() {
        String suffix = this.channel.getUrlSuffix();
        return StringUtils.isBlank(suffix) ? null : ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + stripSlashes(suffix);
    }

    @Override
    public boolean isDefault() {
        return Boolean.TRUE.equals(this.channel.isDefault());
    }

    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities) {
        return authorities;
    }

    @Override
    public void postSuccessAuthenticationProcessing() {
    }

    @Override
    public String getSpecificLoginUrl() {
        return null;
    }

    @Override
    public boolean isSupportActivationByChannel() {
        return true;
    }

    @Override
    public String getUrlSuffix() {
        return this.channel.getUrlSuffix();
    }
}
