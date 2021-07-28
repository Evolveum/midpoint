/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.util.Assert;

/**
 * @author skublik
 */
public class MidpointMetadataRelyingPartyRegistrationResolver extends MidpointLogoutRelyingPartyRegistrationResolver {

	private final InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

	public MidpointMetadataRelyingPartyRegistrationResolver(
            InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
		Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
		this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
	}

	@Override
	public RelyingPartyRegistration convert(HttpServletRequest request) {
        if (!this.relyingPartyRegistrationRepository.iterator().hasNext()) {
            return null;
        }
		RelyingPartyRegistration relyingPartyRegistration = this.relyingPartyRegistrationRepository.iterator().next();
		if (relyingPartyRegistration == null) {
			return null;
		}
		String applicationUri = getApplicationUri(request);
		Function<String, String> templateResolver = templateResolver(applicationUri, relyingPartyRegistration);
		String relyingPartyEntityId = templateResolver.apply(relyingPartyRegistration.getEntityId());
		String assertionConsumerServiceLocation = templateResolver
				.apply(relyingPartyRegistration.getAssertionConsumerServiceLocation());
		return RelyingPartyRegistration.withRelyingPartyRegistration(relyingPartyRegistration)
				.entityId(relyingPartyEntityId).assertionConsumerServiceLocation(assertionConsumerServiceLocation)
				.build();
	}
}
