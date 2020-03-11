/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.gui.api.GuiConstants;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.List;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class AuthenticationChannelImpl implements AuthenticationChannel {

    private AuthenticationSequenceChannelType channel;
    private String pathAfterLogout;

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

    @Override
    public void setPathAfterLogout(String pathAfterLogout) {
        this.pathAfterLogout = pathAfterLogout;
    }

    @Override
    public String getPathAfterLogout() {
        if (StringUtils.isNotBlank(this.pathAfterLogout)) {
            return pathAfterLogout;
        }
        return getPathDuringProccessing();
    }

    @Override
    public boolean matchChannel(AuthenticationSequenceType sequence) {
            if (sequence == null || sequence.getChannel() == null
                    || !getChannelId().equals(sequence.getChannel().getChannelId())) {
                return false;
            }
        return true;
    }

    public String getChannelId() {
        return channel.getChannelId();
    }

    public String getPathAfterSuccessfulAuthentication() {
        return GuiConstants.DEFAULT_PATH_AFTER_LOGIN;
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
