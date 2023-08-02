/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;

import com.evolveum.midpoint.authentication.impl.module.configuration.SamlAdditionalConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.util.CollectionUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class Saml2Provider extends RemoteModuleProvider {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2Provider.class);

    private final OpenSaml4AuthenticationProvider openSamlProvider = new OpenSaml4AuthenticationProvider();
    private final Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> defaultConverter = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();

    public Saml2Provider() {
        initSamlProvider();
    }

    private void initSamlProvider() {
        openSamlProvider.setResponseAuthenticationConverter((responseToken) -> {
            Saml2Authentication authentication = defaultConverter.convert(responseToken);
            if (authentication == null) {
                return null;
            }
            DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) authentication.getPrincipal();
            Map<String, List<Object>> originalAttributes = principal.getAttributes();
            Response response = responseToken.getResponse();
            Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
            if (assertion == null) {
                return authentication;
            }
            Map<String, List<Object>> attributes = new LinkedHashMap<>();
            for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
                for (Attribute attribute : attributeStatement.getAttributes()) {
                    if (originalAttributes.containsKey(attribute.getName())) {
                        List<Object> attributeValues = originalAttributes.get(attribute.getName());
                        attributes.put(attribute.getName(), attributeValues);
                        if (StringUtils.isNotEmpty(attribute.getFriendlyName())) {
                            attributes.put(attribute.getFriendlyName(), attributeValues);
                        }
                    }
                }
            }
            MidpointSaml2AuthenticatedPrincipal newPrincipal = new MidpointSaml2AuthenticatedPrincipal(
                    principal.getName(),
                    attributes,
                    assertion.getSubject().getNameID()
            );
            newPrincipal.setRelyingPartyRegistrationId(responseToken.getToken().getRelyingPartyRegistration().getRegistrationId());
            Saml2Authentication saml2Authentication = new Saml2Authentication(
                    newPrincipal,
                    authentication.getSaml2Response(),
                    authentication.getAuthorities()
            );
            saml2Authentication.setDetails(assertion.getSubject().getNameID());
            return saml2Authentication;
        });
    }

    @Override
    protected Authentication internalAuthentication(
            Authentication authentication, List requireAssignment, AuthenticationChannel channel, Class focusType)
            throws AuthenticationException {
        Authentication token;
        if (authentication instanceof Saml2AuthenticationToken) {
            Saml2AuthenticationToken samlAuthenticationToken = (Saml2AuthenticationToken) authentication;
            Saml2Authentication samlAuthentication;
            try {
                samlAuthentication = (Saml2Authentication) openSamlProvider.authenticate(samlAuthenticationToken);
            } catch (AuthenticationException e) {
                getAuditProvider().auditLoginFailure(null, null, createConnectEnvironment(getChannel()), e.getMessage());
                LOGGER.debug("Unexpected exception in saml module", e);
                throw e;
            }
            Saml2ModuleAuthenticationImpl samlModule = (Saml2ModuleAuthenticationImpl) AuthUtil.getProcessingModule();
            try {
                DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) samlAuthentication.getPrincipal();
                samlAuthenticationToken.setDetails(principal);
                Map<String, List<Object>> attributes = principal.getAttributes();
                String enteredUsername;
                SamlAdditionalConfiguration config = samlModule.getAdditionalConfiguration().get(samlAuthenticationToken.getRelyingPartyRegistration().getRegistrationId());
                String nameOfSamlAttribute = config.getNameOfUsernameAttribute();
                enteredUsername = defineEnteredUsername(attributes, nameOfSamlAttribute);

                token = getPreAuthenticationToken(enteredUsername, focusType, requireAssignment, channel);
            } catch (AuthenticationException e) {
                samlModule.setAuthentication(samlAuthenticationToken);
                LOGGER.debug("Authentication with saml module failed: {}", e.getMessage());
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

    private String defineEnteredUsername(Map<String, List<Object>> attributes, String nameOfSamlAttribute) {
        LOGGER.debug("attributes in authentication: " + attributes);
        LOGGER.debug("nameOfSamlAttribute: " + nameOfSamlAttribute);

        if (!attributes.containsKey(nameOfSamlAttribute)){
            LOGGER.debug("Couldn't find attribute for username in saml response");
            throw new AuthenticationServiceException("web.security.provider.invalid");
        } else {
            List<Object> values = attributes.get(nameOfSamlAttribute);
            if (values == null || values.isEmpty() || values.get(0) == null) {
                LOGGER.debug("Saml attribute, which define username don't contains value");
                throw new AuthenticationServiceException("web.security.provider.invalid");
            }
            if (values.size() != 1) {
                LOGGER.debug("Saml attribute, which define username contains more values {}", values);
                throw new AuthenticationServiceException("web.security.provider.invalid");
            }
            return (String) values.iterator().next();
        }
    }

    @Override
    public boolean supports(Class authentication) {
        return openSamlProvider.supports(authentication);
    }

    public static class MidpointSaml2AuthenticatedPrincipal extends DefaultSaml2AuthenticatedPrincipal{

        private final String spNameQualifier;
        private final String nameIdFormat;

        public MidpointSaml2AuthenticatedPrincipal(String name, Map<String, List<Object>> attributes, NameID nameID) {
            super(name, attributes);
            spNameQualifier = nameID.getSPNameQualifier();
            nameIdFormat = nameID.getFormat();
        }

        public String getNameIdFormat() {
            return nameIdFormat;
        }

        public String getSpNameQualifier() {
            return spNameQualifier;
        }
    }
}
