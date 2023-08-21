/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * @author skublik
 */

public class AuthenticationChannelImpl implements AuthenticationChannel {

    private final AuthenticationSequenceChannelType channel;
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
        return sequence != null && sequence.getChannel() != null
                && getChannelId().equals(sequence.getChannel().getChannelId());
    }

    public String getChannelId() {
        return channel.getChannelId();
    }

    public String getPathAfterSuccessfulAuthentication() {
        return AuthConstants.DEFAULT_PATH_AFTER_LOGIN;
    }

    public String getPathAfterUnsuccessfulAuthentication() {
        return AuthConstants.DEFAULT_PATH_AFTER_LOGOUT;
    }

    @Override
    public String getPathDuringProccessing() {
        String suffix = this.channel.getUrlSuffix();
        return StringUtils.isBlank(suffix) ? null : ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + AuthUtil.stripSlashes(suffix);
    }

    @Override
    public boolean isDefault() {
        return Boolean.TRUE.equals(this.channel.isDefault());
    }

    /**
     * This method cares about removing some undesirable authorities and adding some additional authorities
     * (e.g. an authority for the password reset page should be added to the list in case of successful user authentication)
     *
     * @param authorities
     * @return
     */
    public Collection<Authorization> resolveAuthorities(Collection<Authorization> authorities) {
        var cleanedUpAuthorities = cleanupAuthorities(authorities);
        var newAuthorities = new ArrayList<>(cleanedUpAuthorities);
        addAdditionalAuthorities(newAuthorities);
        return Collections.unmodifiableList(newAuthorities);
    }

    protected Collection<Authorization> cleanupAuthorities(Collection<Authorization> authorities) {
        return authorities;
    }

    private void addAdditionalAuthorities(Collection<Authorization> authorities) {
        getAdditionalAuthoritiesList()
                .forEach(a -> authorities.add(createAuthorization(a)));
    }

    protected Collection<String> getAdditionalAuthoritiesList() {
        return Collections.emptyList();
    }

    private Authorization createAuthorization(String authUrl) {
        var authorizationType = new AuthorizationType();
        authorizationType.getAction().add(authUrl);
        return new Authorization(authorizationType);
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

    @Override
    public boolean isPostAuthenticationEnabled() {
        return false;
    }
}
