/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import static org.springframework.util.StringUtils.hasText;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.security.saml.SamlMetadataCache;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.SamlValidator;
import org.springframework.security.saml.key.KeyType;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.config.LocalProviderConfiguration;
import org.springframework.security.saml.provider.config.SamlConfigurationRepository;
import org.springframework.security.saml.provider.provisioning.HostBasedSamlServiceProviderProvisioning;
import org.springframework.security.saml.provider.service.AuthenticationRequestEnhancer;
import org.springframework.security.saml.provider.service.HostedServiceProviderService;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.authentication.AuthenticationRequest;
import org.springframework.security.saml.saml2.authentication.NameIdPolicy;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.util.MidpointSamlLocalServiceProviderConfiguration;

/**
 * @author skublik
 */

public class MidpointHostBasedSamlServiceProviderProvisioning extends HostBasedSamlServiceProviderProvisioning {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointHostBasedSamlServiceProviderProvisioning.class);

    private final AuthenticationRequestEnhancer authnRequestEnhancer;

    public MidpointHostBasedSamlServiceProviderProvisioning(SamlConfigurationRepository configuration, SamlTransformer transformer, SamlValidator validator, SamlMetadataCache cache, AuthenticationRequestEnhancer authnRequestEnhancer) {
        super(configuration, transformer, validator, cache, authnRequestEnhancer);
        this.authnRequestEnhancer = authnRequestEnhancer;
    }

    protected ServiceProviderService getHostedServiceProvider(LocalServiceProviderConfiguration spConfig) {
        String basePath = spConfig.getBasePath();

        List<SimpleKey> keys = new LinkedList<>();
        SimpleKey signingKey = null;
        if (spConfig.getKeys() != null) {
            SimpleKey activeKey = spConfig.getKeys().getActive();
            if (activeKey != null) {
                keys.add(activeKey);
                keys.add(activeKey.clone(activeKey.getName() + "-encryption", KeyType.ENCRYPTION));
            }
            keys.addAll(spConfig.getKeys().getStandBy());
            signingKey = spConfig.isSignMetadata() ? spConfig.getKeys().getActive() : null;
        }

        String prefix = hasText(spConfig.getPrefix()) ? spConfig.getPrefix() : "saml/sp/";
        String aliasPath = getAliasPath(spConfig);
        ServiceProviderMetadata metadata =
                serviceProviderMetadata(
                        basePath,
                        signingKey,
                        keys,
                        prefix,
                        aliasPath,
                        spConfig.getDefaultSigningAlgorithm(),
                        spConfig.getDefaultDigest()
                );
        if (!spConfig.getNameIds().isEmpty()) {
            metadata.getServiceProvider().setNameIds(spConfig.getNameIds());
        }

        if (!spConfig.isSingleLogoutEnabled()) {
            metadata.getServiceProvider().setSingleLogoutService(Collections.emptyList());
        }
        if (hasText(spConfig.getEntityId())) {
            metadata.setEntityId(spConfig.getEntityId());
        }
        if (hasText(spConfig.getAlias())) {
            metadata.setEntityAlias(spConfig.getAlias());
        }
        metadata.getServiceProvider().setWantAssertionsSigned(spConfig.isWantAssertionsSigned());
        metadata.getServiceProvider().setAuthnRequestsSigned(spConfig.isSignRequests());

        return new HostedServiceProviderService(
                spConfig,
                metadata,
                getTransformer(),
                getValidator(),
                getCache(),
                authnRequestEnhancer
        ) {
            @Override
            public AuthenticationRequest authenticationRequest(IdentityProviderMetadata idp) {
                AuthenticationRequest request = super.authenticationRequest(idp);
                if (!spConfig.getNameIds().isEmpty()) {
                    for (NameId nameId : spConfig.getNameIds()) {
                        if (idp.getIdentityProvider().getNameIds().contains(nameId)) {
                            request.setNameIdPolicy(new NameIdPolicy(
                                    nameId,
                                    getMetadata().getEntityAlias(),
                                    true
                            ));
                            return request;
                        }
                    }
                    LOGGER.error("No NameId format from metadata of service provider isn't supported by metadata of identity provider. "
                            + "Using NameId format from IP. NameId of SP: {}; NameId of IP {}", spConfig.getNameIds(), idp.getIdentityProvider().getNameIds());
                }
                return request;
            }
        };
    }

    @Override
    protected String getAliasPath(LocalProviderConfiguration configuration) {
        if (configuration instanceof MidpointSamlLocalServiceProviderConfiguration) {
            String alias = ((MidpointSamlLocalServiceProviderConfiguration) configuration).getAliasForPath();
            if (hasText(alias)) {
                return alias;
            }
        }
        return super.getAliasPath(configuration);
    }
}
