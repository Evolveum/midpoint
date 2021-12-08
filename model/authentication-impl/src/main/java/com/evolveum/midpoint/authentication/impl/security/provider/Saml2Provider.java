/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.provider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.*;
import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.Saml2ModuleAuthenticationImpl;

import com.evolveum.midpoint.authentication.impl.security.module.configuration.SamlMidpointAdditionalConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.CollectionUtils;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class Saml2Provider extends MidPointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2Provider.class);

    private final OpenSaml4AuthenticationProvider openSamlProvider = new OpenSaml4AuthenticationProvider();
    private final Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> defaultConverter = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();

    @Autowired
    @Qualifier("passwordAuthenticationEvaluator")
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
    protected AuthenticationEvaluator getEvaluator() {
        return authenticationEvaluator;
    }

    @Override
    protected void writeAuthentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
                                       ModuleAuthenticationImpl moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal != null && principal instanceof GuiProfiledPrincipal) {
            mpAuthentication.setPrincipal(principal);
        }
        if (token instanceof PreAuthenticatedAuthenticationToken) {
            ((PreAuthenticatedAuthenticationToken)token).setDetails(originalAuthentication);
        }
        moduleAuthentication.setAuthentication(token);
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
                                                    AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        ConnectionEnvironment connEnv = createEnvironment(channel);

        Authentication token;
        if (authentication instanceof Saml2AuthenticationToken) {
            Saml2AuthenticationToken samlAuthenticationToken = (Saml2AuthenticationToken) authentication;
            Saml2Authentication samlAuthentication = (Saml2Authentication) openSamlProvider.authenticate(samlAuthenticationToken);
            Saml2ModuleAuthenticationImpl samlModule = (Saml2ModuleAuthenticationImpl) AuthUtil.getProcessingModule(true);
            try {
                DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) samlAuthentication.getPrincipal();
                samlAuthenticationToken.setDetails(principal);
                Map<String, List<Object>> attributes = principal.getAttributes();
                String enteredUsername = "";
                SamlMidpointAdditionalConfiguration config = samlModule.getAdditionalConfiguration().get(samlAuthenticationToken.getRelyingPartyRegistration().getRegistrationId());
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
                samlModule.setAuthentication(samlAuthenticationToken);
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

    public class MidpointSaml2AuthenticatedPrincipal extends DefaultSaml2AuthenticatedPrincipal{

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
