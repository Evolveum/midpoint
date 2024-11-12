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
import com.evolveum.midpoint.authentication.impl.filter.oidc.OidcUserTokenService;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
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
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        OidcIdTokenDecoderFactory decoder = new OidcIdTokenDecoderFactory();
        OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter =
                new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        requestEntityConverter.addParametersConverter(createParameterConverter());
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRequestEntityConverter(requestEntityConverter);
        decoder.setJwsAlgorithmResolver(getAlgorithmResolver());
        oidcProvider = new OidcAuthorizationCodeAuthenticationProvider(client, getUserService(decoder));
        oidcProvider.setJwtDecoderFactory(decoder);
    }

    private Function<ClientRegistration, JwsAlgorithm> getAlgorithmResolver() {
        return clientRegistration -> {
            String signingAlg = additionalConfiguration.get(clientRegistration.getRegistrationId()).getIdTokenSingingAlg();

            if (StringUtils.isBlank(signingAlg)) {
                return SignatureAlgorithm.RS256;
            }

            JwsAlgorithm ret;
            try {
                ret = SignatureAlgorithm.valueOf(signingAlg);
            } catch (IllegalArgumentException e) {
                try {
                    ret = MacAlgorithm.valueOf(signingAlg);
                } catch (IllegalArgumentException ex) {
                    ArrayList<String> supportedValues = new ArrayList<>();
                    supportedValues.addAll(Arrays.stream(SignatureAlgorithm.values()).map(Enum::name).toList());
                    supportedValues.addAll(Arrays.stream(MacAlgorithm.values()).map(Enum::name).toList());
                    LOGGER.error("Couldn't ' parse signing algorithm '" + signingAlg + "', supported values are " +
                            String.join(", ", supportedValues) + ", using default RS256");
                    return SignatureAlgorithm.RS256;
                }
            }
            return ret;
        };
    }

    private Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> createParameterConverter() {
        NimbusJwtClientAuthenticationParametersConverter<OAuth2AuthorizationCodeGrantRequest> parameterConverter =
                new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver);
        parameterConverter.setJwtClientAssertionCustomizer((context) -> {
            ClientRegistration clientRegistration = context.getAuthorizationGrantRequest().getClientRegistration();
            JWK jwk = jwkResolver.apply(clientRegistration);
            if (jwk.getX509CertThumbprint() != null) {
                context.getHeaders().x509SHA1Thumbprint(jwk.getX509CertThumbprint().toString());
                context.getHeaders().type("JWT");
            }
        });
        return parameterConverter;
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
                builder.x509CertThumbprint(config.getThumbprint());

                // sha-1 is deprecated, but some servers don't allow 'x5t#S256' header
                //builder.x509CertSHA256Thumbprint(config.getThumbprint256());

                builder.keyID(null);  //hack without it resolver can't find key
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
                LOGGER.debug("Unexpected exception in oidc module", e);
                throw new AuthenticationServiceException("web.security.provider.unavailable", e);
            }
            OidcClientModuleAuthenticationImpl oidcModule = (OidcClientModuleAuthenticationImpl) AuthUtil.getProcessingModule();
            try {
                String enteredUsername = oidcAuthenticationToken.getName();
                if (StringUtils.isEmpty(enteredUsername)) {
                    LOGGER.error("Oidc attribute, which define username don't contains value");
                    throw new AuthenticationServiceException("web.security.provider.invalid");
                }
                token = getPreAuthenticationToken(enteredUsername, focusType, requireAssignment, channel);
                ((OAuth2LoginAuthenticationToken) authentication).setDetails(oidcAuthenticationToken.getPrincipal());
            } catch (AuthenticationException e) {
                oidcModule.setAuthentication(oidcAuthenticationToken);
                LOGGER.debug("Authentication with oidc module failed: {}", e.getMessage());
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
