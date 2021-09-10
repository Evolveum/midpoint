/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author skublik
 */
public class MidpointMetadataRelyingPartyRegistrationResolver implements Converter<HttpServletRequest, RelyingPartyRegistration> {

    private static final char PATH_DELIMITER = '/';

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

    protected Function<String, String> templateResolver(String applicationUri, RelyingPartyRegistration relyingParty) {
        return (template) -> resolveUrlTemplate(template, applicationUri, relyingParty);
    }

    private static String resolveUrlTemplate(String template, String baseUrl, RelyingPartyRegistration relyingParty) {
        String entityId = relyingParty.getAssertingPartyDetails().getEntityId();
        String registrationId = relyingParty.getRegistrationId();
        Map<String, String> uriVariables = new HashMap<>();
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(baseUrl).replaceQuery(null).fragment(null)
                .build();
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", (scheme != null) ? scheme : "");
        String host = uriComponents.getHost();
        uriVariables.put("baseHost", (host != null) ? host : "");
        // following logic is based on HierarchicalUriComponents#toUriString()
        int port = uriComponents.getPort();
        uriVariables.put("basePort", (port == -1) ? "" : ":" + port);
        String path = uriComponents.getPath();
        if (StringUtils.hasLength(path) && path.charAt(0) != PATH_DELIMITER) {
            path = PATH_DELIMITER + path;
        }
        uriVariables.put("basePath", (path != null) ? path : "");
        uriVariables.put("baseUrl", uriComponents.toUriString());
        uriVariables.put("entityId", StringUtils.hasText(entityId) ? entityId : "");
        uriVariables.put("registrationId", StringUtils.hasText(registrationId) ? registrationId : "");
        return UriComponentsBuilder.fromUriString(template).buildAndExpand(uriVariables).toUriString();
    }

    protected static String getApplicationUri(HttpServletRequest request) {
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                .replacePath(request.getContextPath()).replaceQuery(null).fragment(null).build();
        return uriComponents.toUriString();
    }
}
