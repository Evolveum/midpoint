/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.provider;

import java.util.*;

import com.evolveum.midpoint.web.security.module.configuration.SamlMidpointAdditionalConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.*;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.util.CollectionUtils;

/**
 * @author skublik
 */

public class Saml2Provider extends MidPointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2Provider.class);

    private final OpenSaml4AuthenticationProvider openSamlProvider = new OpenSaml4AuthenticationProvider();
    private final Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> defaultConverter = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();

    @Autowired
    @Qualifier("samlAuthenticationEvaluator")
    private AuthenticationEvaluator<PasswordAuthenticationContext> authenticationEvaluator;

    public Saml2Provider() {
        openSamlProvider.setResponseAuthenticationConverter((responseToken) -> {
            Saml2Authentication authentication = defaultConverter.convert(responseToken);
            DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) authentication.getPrincipal();
            Map<String, List<Object>> originalAttributes = principal.getAttributes();
            Response response = responseToken.getResponse();
            Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
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
            return new Saml2Authentication(new DefaultSaml2AuthenticatedPrincipal(principal.getName(), attributes),
                    authentication.getSaml2Response(), authentication.getAuthorities());
        });
    }

    @Override
    protected AuthenticationEvaluator getEvaluator() {
        return authenticationEvaluator;
    }

    @Override
    protected void writeAutentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
            ModuleAuthentication moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal != null && principal instanceof GuiProfiledPrincipal) {
            mpAuthentication.setPrincipal(principal);
        }

        moduleAuthentication.setAuthentication(originalAuthentication);
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
            AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        ConnectionEnvironment connEnv = createEnvironment(channel);

        Authentication token;
        if (authentication instanceof Saml2AuthenticationToken) {
            Saml2AuthenticationToken samlAuthenticationToken = (Saml2AuthenticationToken) authentication;
            Saml2Authentication samlAuthentication = (Saml2Authentication) openSamlProvider.authenticate(samlAuthenticationToken);
            Saml2ModuleAuthentication samlModule = (Saml2ModuleAuthentication) SecurityUtils.getProcessingModule(true);
            try {
                DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) samlAuthentication.getPrincipal();
                Map<String, List<Object>> attributes = principal.getAttributes();
                String enteredUsername = "";
                SamlMidpointAdditionalConfiguration config = samlModule.getAdditionalConfiguration().get(samlAuthenticationToken.getRelyingPartyRegistration().getAssertingPartyDetails().getEntityId());
                String nameOfSamlAttribute = config.getNameOfUsernameAttribute();
                if (!attributes.containsKey(nameOfSamlAttribute)){
                    LOGGER.error("Couldn't find attribute for username in saml response");
                    throw new AuthenticationServiceException("web.security.auth.saml2.username.null");
                } else {
                    List<Object> values = attributes.get(nameOfSamlAttribute);
                    if (values == null || values.isEmpty() || values.get(0) == null) {
                        LOGGER.error("Saml attribute, which define username don't contains value");
                        throw new AuthenticationServiceException("web.security.auth.saml2.username.null");
                    }
                    if (values.size() != 1) {
                        LOGGER.error("Saml attribute, which define username contains more values {}", values);
                        throw new AuthenticationServiceException("web.security.auth.saml2.username.more.values");
                    }
                    enteredUsername = (String) values.iterator().next();
                }
                PreAuthenticationContext authContext = new PreAuthenticationContext(enteredUsername, focusType, requireAssignment);
                if (channel != null) {
                    authContext.setSupportActivationByChannel(channel.isSupportActivationByChannel());
                }
                token = authenticationEvaluator.authenticateUserPreAuthenticated(connEnv, authContext);
            } catch (AuthenticationException e) {
                samlModule.setAuthentication(samlAuthentication);
                LOGGER.info("Authentication with saml module failed: {}", e.getMessage());
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
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof PreAuthenticatedAuthenticationToken) {
            return new PreAuthenticatedAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class authentication) {
        return openSamlProvider.supports(authentication);
    }
}
