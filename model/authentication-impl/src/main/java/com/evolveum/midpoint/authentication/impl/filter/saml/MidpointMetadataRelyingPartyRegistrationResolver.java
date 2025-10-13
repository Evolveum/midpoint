/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.saml;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;

/**
 * @author skublik
 */
public class MidpointMetadataRelyingPartyRegistrationResolver implements RelyingPartyRegistrationResolver {

    private final DefaultRelyingPartyRegistrationResolver defaultResolver;

    public MidpointMetadataRelyingPartyRegistrationResolver(
            InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this.defaultResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);
    }

    @Override
    public RelyingPartyRegistration resolve(HttpServletRequest request, String relyingPartyRegistrationId) {
        RelyingPartyRegistration relyingPartyRegistration = defaultResolver.resolve(request, relyingPartyRegistrationId);

        return RelyingPartyRegistration
                .withRelyingPartyRegistration(relyingPartyRegistration)
                .entityId(relyingPartyRegistration.getEntityId())
                .assertionConsumerServiceLocation(relyingPartyRegistration.getAssertionConsumerServiceLocation())
                .singleLogoutServiceLocation(relyingPartyRegistration.getSingleLogoutServiceLocation())
                .singleLogoutServiceResponseLocation("")
                .build();
    }
}
