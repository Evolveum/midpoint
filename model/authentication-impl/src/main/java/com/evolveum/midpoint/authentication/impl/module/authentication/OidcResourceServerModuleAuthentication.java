/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author skublik
 */

public class OidcResourceServerModuleAuthentication extends HttpModuleAuthentication {

    public OidcResourceServerModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.OIDC, sequenceModule);
    }

    public ModuleAuthenticationImpl clone() {
        OidcResourceServerModuleAuthentication module = new OidcResourceServerModuleAuthentication(this.getSequenceModule());
        clone(module);
        return module;
    }

    public String getRealmFromHeader(AuthenticationException authException) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException)authException).getError();
            parameters.put("error", error.getErrorCode());
            if (org.springframework.util.StringUtils.hasText(error.getDescription())) {
                parameters.put("error_description", error.getDescription());
            }

            if (org.springframework.util.StringUtils.hasText(error.getUri())) {
                parameters.put("error_uri", error.getUri());
            }

            if (error instanceof BearerTokenError) {
                BearerTokenError bearerTokenError = (BearerTokenError)error;
                if (StringUtils.hasText(bearerTokenError.getScope())) {
                    parameters.put("scope", bearerTokenError.getScope());
                }
            }
        }
        StringBuilder wwwAuthenticate = new StringBuilder(super.getRealmFromHeader(authException));
        if (!parameters.isEmpty()) {
            parameters.forEach((key, value) -> {
                wwwAuthenticate.append(", ");
                wwwAuthenticate.append(key).append("=\"").append(value).append("\"");
            });
        }
        return wwwAuthenticate.toString();
    }
}
