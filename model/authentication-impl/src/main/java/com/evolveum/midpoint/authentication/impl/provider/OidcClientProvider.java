/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcClientModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcAdditionalConfiguration;
import com.evolveum.midpoint.authentication.impl.oidc.OidcUserTokenService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.function.Function;

/**
 * @author skublik
 */

public class OidcClientProvider extends RemoteModuleProvider {

    private static final Trace LOGGER = TraceManager.getTrace(OidcClientProvider.class);

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider;

    private final Map<String, OidcAdditionalConfiguration> additionalConfiguration;
    private Function<ClientRegistration, JWK> jwkResolver;

    public OidcClientProvider(Map<String, OidcAdditionalConfiguration> additionalConfiguration) {
        this.additionalConfiguration = additionalConfiguration;
        initJwkResolver();
        JwtDecoderFactory<ClientRegistration> decoder = new OidcIdTokenDecoderFactory();
        OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter =
                new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        requestEntityConverter.addParametersConverter(
                new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRequestEntityConverter(requestEntityConverter);
        oidcProvider = new OidcAuthorizationCodeAuthenticationProvider(client, getUserService(decoder));
        oidcProvider.setJwtDecoderFactory(decoder);
    }

    private OidcUserService getUserService(JwtDecoderFactory<ClientRegistration> decoder) {
        OidcUserService service = new OidcUserService();
        OidcUserTokenService oidcTokenUserService = new OidcUserTokenService();
        oidcTokenUserService.setJwtDecoderFactory(decoder);
        service.setOauth2UserService(oidcTokenUserService);
        return service;
    }

    private void initJwkResolver() {
        jwkResolver = (clientRegistration) -> {
            if (clientRegistration.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.CLIENT_SECRET_JWT)) {
                OctetSequenceKey.Builder builder = new OctetSequenceKey.Builder(clientRegistration.getClientSecret().getBytes(StandardCharsets.UTF_8))
                        .keyID(UUID.randomUUID().toString());
                String signingAlg = additionalConfiguration.get(clientRegistration.getRegistrationId()).getSingingAlg();
                builder.algorithm(Algorithm.parse(signingAlg));
                return builder.build();

            } else if (clientRegistration.getClientAuthenticationMethod().equals(ClientAuthenticationMethod.PRIVATE_KEY_JWT)) {
                OidcAdditionalConfiguration config = additionalConfiguration.get(clientRegistration.getRegistrationId());
                RSAPublicKey publicKey = config.getPublicKey();
                RSAPrivateKey privateKey = config.getPrivateKey();
                RSAKey.Builder builder = new RSAKey.Builder(publicKey)
                        .privateKey(privateKey)
                        .keyID(UUID.randomUUID().toString());
                String signingAlg = additionalConfiguration.get(clientRegistration.getRegistrationId()).getSingingAlg();
                builder.algorithm(Algorithm.parse(signingAlg));
                builder.keyID(null); //hack without it resolver can't find key
                return builder.build();
            }
            return null;
        };
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
            AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        PreAuthenticatedAuthenticationToken token;
        if (authentication instanceof OAuth2LoginAuthenticationToken) {
            OAuth2LoginAuthenticationToken oidcAuthenticationToken;
            try {
                oidcAuthenticationToken = (OAuth2LoginAuthenticationToken) oidcProvider.authenticate(authentication);
            } catch (Exception e) {
                getAuditProvider().auditLoginFailure(null, null, createConnectEnvironment(getChannel()), e.getMessage());
                throw new AuthenticationServiceException("web.security.provider.unavailable", e);
            }
            OidcClientModuleAuthenticationImpl oidcModule = (OidcClientModuleAuthenticationImpl) AuthUtil.getProcessingModule();
            try {
                String enteredUsername = oidcAuthenticationToken.getName();
                if (StringUtils.isEmpty(enteredUsername)) {
                    LOGGER.error("Oidc attribute, which define username don't contains value");
                    throw new AuthenticationServiceException("web.security.provider.invalid");
                }
                token = getPreAuthenticationToken(authentication, enteredUsername, focusType, requireAssignment, channel);
                ((OAuth2LoginAuthenticationToken) authentication).setDetails(oidcAuthenticationToken.getPrincipal());
            } catch (AuthenticationException e) {
                oidcModule.setAuthentication(oidcAuthenticationToken);
                LOGGER.info("Authentication with oidc module failed: {}", e.getMessage()); // TODO debug?
                throw e;
            }
        } else {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;
    }

    @Override
    public boolean supports(Class authentication) {
        return oidcProvider.supports(authentication);
    }
}
