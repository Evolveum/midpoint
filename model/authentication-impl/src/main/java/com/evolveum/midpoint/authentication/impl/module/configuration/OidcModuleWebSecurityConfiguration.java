/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.Objects;

import static com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil.getBasePath;

/**
 * @author skublik
 */

public class OidcModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    private static final Trace LOGGER = TraceManager.getTrace(OidcModuleWebSecurityConfiguration.class);

    private static Protector protector;

    private InMemoryClientRegistrationRepository clientRegistrationRepository;

    private OidcModuleWebSecurityConfiguration() {
    }

    public static void setProtector(Protector protector) {
        OidcModuleWebSecurityConfiguration.protector = protector;
    }

    public static OidcModuleWebSecurityConfiguration build(OidcAuthenticationModuleType modelType, String prefixOfSequence,
                                                           String publicHttpUrlPattern, ServletRequest request) {
        OidcModuleWebSecurityConfiguration configuration = buildInternal(modelType, prefixOfSequence, publicHttpUrlPattern, request);
        configuration.validate();
        return configuration;
    }

    private static OidcModuleWebSecurityConfiguration buildInternal(OidcAuthenticationModuleType modelType, String prefixOfSequence,
                                                                    String publicHttpUrlPattern, ServletRequest request) {
        OidcModuleWebSecurityConfiguration configuration = new OidcModuleWebSecurityConfiguration();
        build(configuration, modelType, prefixOfSequence);

        List<OidcClientAuthenticationModuleType> clients = modelType.getClient();
        List<ClientRegistration> registrations = new ArrayList<>();
        clients.forEach(client -> {

            OidcOpenIdProviderType openIdProvider = client.getOpenIdProvider();
            Assert.notNull(openIdProvider, "openIdProvider cannot be null");
            ClientRegistration.Builder builder = null;
            try {
                builder = ClientRegistrations.fromOidcIssuerLocation(openIdProvider.getIssuerUri());
            } catch (Exception e) {
                LOGGER.debug("Couldn't create oidc client builder by issuer uri.");
            }

            Assert.hasText(client.getRegistrationId(), "registrationId cannot be empty");
            if (builder == null) {
                builder = ClientRegistration.withRegistrationId(client.getRegistrationId());
            } else {
                builder.registrationId(client.getRegistrationId());
            }
            builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            builder.userInfoAuthenticationMethod(AuthenticationMethod.QUERY);
            UriComponentsBuilder redirectUri = UriComponentsBuilder.fromUriString(
                    StringUtils.isNotBlank(publicHttpUrlPattern) ? publicHttpUrlPattern : getBasePath((HttpServletRequest) request));
            redirectUri.pathSegment(
                    DEFAULT_PREFIX_OF_MODULE,
                    AuthUtil.stripSlashes(prefixOfSequence),
                    AuthUtil.stripSlashes(modelType.getName()),
                    AuthUtil.stripSlashes(RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX),
                    client.getRegistrationId());
            builder.redirectUri(redirectUri.toUriString());

            Assert.hasText(client.getClientId(), "clientId cannot be empty");
            builder.clientId(client.getClientId());

            Assert.hasText(client.getNameOfUsernameAttribute(), "nameOfUsernameAttribute cannot be empty");
            builder.userNameAttributeName(client.getNameOfUsernameAttribute());

            if (!Objects.isNull(client.getClientSecret())) {
                try {
                    String clientSecret = protector.decryptString(client.getClientSecret());
                    builder.clientSecret(clientSecret);
                } catch (EncryptionException e) {
                    LOGGER.error("Couldn't obtain clear string for client secret");
                }
            }

            getOptionalIfNotEmpty(client.getClientName()).ifPresent(builder::clientName);
            getOptionalIfNotEmpty(openIdProvider.getAuthorizationUri()).ifPresent(builder::clientName);
            getOptionalIfNotEmpty(openIdProvider.getTokenUri()).ifPresent(builder::clientName);
            getOptionalIfNotEmpty(openIdProvider.getUserInfoUri()).ifPresent(builder::clientName);
            getOptionalIfNotEmpty(openIdProvider.getIssuerUri()).ifPresent(builder::clientName);
            ClientRegistration clientRegistration = builder.build();

            if (clientRegistration.getScopes() == null || !clientRegistration.getScopes().contains("openid")) {
                List<String> scopes = new ArrayList<>();
                if (clientRegistration.getScopes() != null) {
                    scopes.addAll(clientRegistration.getScopes());
                }
                scopes.add("openid");
                builder.scope(scopes);
            }
            if (!clientRegistration.getProviderDetails().getConfigurationMetadata().containsKey("end_session_endpoint")
                    && StringUtils.isNotEmpty(openIdProvider.getEndSessionUri())) {
                Map<String, Object> configurationMetadata = new HashMap<>(clientRegistration.getProviderDetails().getConfigurationMetadata());
                configurationMetadata.put("end_session_endpoint", openIdProvider.getEndSessionUri());
                builder.providerConfigurationMetadata(configurationMetadata);
            }
            clientRegistration = builder.build();

            Assert.hasText(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri(), "UserInfoUri cannot be empty");

            registrations.add(clientRegistration);
        });

        InMemoryClientRegistrationRepository clientRegistrationRepository = new InMemoryClientRegistrationRepository(registrations);
        configuration.setClientRegistrationRepository(clientRegistrationRepository);
        return configuration;
    }

    private static Optional<String> getOptionalIfNotEmpty(String value) {
        return Optional.ofNullable(value).filter((s) -> !s.isEmpty());
    }

    public InMemoryClientRegistrationRepository getClientRegistrationRepository() {
        return clientRegistrationRepository;
    }

    public void setClientRegistrationRepository(InMemoryClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    protected void validate() {
        super.validate();
        if (getClientRegistrationRepository() == null) {
            throw new IllegalArgumentException("Oidc configuration is null");
        }
    }

    public String getPrefixOfSequence() {
        return DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + AuthUtil.stripSlashes(getSequenceSuffix());
    }
}
