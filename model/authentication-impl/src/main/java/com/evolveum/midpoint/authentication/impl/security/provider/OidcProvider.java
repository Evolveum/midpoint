/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.authentication.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.OidcModuleAuthenticationImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */

public class OidcProvider extends MidPointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(OidcProvider.class);

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider = new OidcAuthorizationCodeAuthenticationProvider(
            new DefaultAuthorizationCodeTokenResponseClient() ,new OidcUserService());

    @Autowired
    @Qualifier("passwordAuthenticationEvaluator")
    private AuthenticationEvaluator<PasswordAuthenticationContext> authenticationEvaluator;

    public OidcProvider() {
        JwtDecoderFactory<ClientRegistration>  decoder = new OidcIdTokenDecoderFactory();
        oidcProvider.setJwtDecoderFactory(decoder);
    }

//    public OidcProvider() {
//        openSamlProvider.setResponseAuthenticationConverter((responseToken) -> {
//            Saml2Authentication authentication = defaultConverter.convert(responseToken);
//            if (authentication == null) {
//                return null;
//            }
//            DefaultSaml2AuthenticatedPrincipal principal = (DefaultSaml2AuthenticatedPrincipal) authentication.getPrincipal();
//            Map<String, List<Object>> originalAttributes = principal.getAttributes();
//            Response response = responseToken.getResponse();
//            Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
//            if (assertion == null) {
//                return authentication;
//            }
//            Map<String, List<Object>> attributes = new LinkedHashMap<>();
//            for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
//                for (Attribute attribute : attributeStatement.getAttributes()) {
//                    if (originalAttributes.containsKey(attribute.getName())) {
//                        List<Object> attributeValues = originalAttributes.get(attribute.getName());
//                        attributes.put(attribute.getName(), attributeValues);
//                        if (StringUtils.isNotEmpty(attribute.getFriendlyName())) {
//                            attributes.put(attribute.getFriendlyName(), attributeValues);
//                        }
//                    }
//                }
//            }
//            MidpointSaml2AuthenticatedPrincipal newPrincipal = new MidpointSaml2AuthenticatedPrincipal(
//                    principal.getName(),
//                    attributes,
//                    assertion.getSubject().getNameID()
//            );
//            newPrincipal.setRelyingPartyRegistrationId(responseToken.getToken().getRelyingPartyRegistration().getRegistrationId());
//            Saml2Authentication saml2Authentication = new Saml2Authentication(
//                    newPrincipal,
//                    authentication.getSaml2Response(),
//                    authentication.getAuthorities()
//            );
//            saml2Authentication.setDetails(assertion.getSubject().getNameID());
//            return saml2Authentication;
//        });
//    }

    @Override
    protected AuthenticationEvaluator getEvaluator() {
        return authenticationEvaluator;
    }

    @Override
    protected void writeAuthentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
                                       ModuleAuthenticationImpl moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal instanceof GuiProfiledPrincipal) {
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
        if (authentication instanceof OAuth2LoginAuthenticationToken) {
            OAuth2LoginAuthenticationToken oidcAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;
            OAuth2LoginAuthenticationToken oidcAuthentication = (OAuth2LoginAuthenticationToken) oidcProvider.authenticate(oidcAuthenticationToken);
            OidcModuleAuthenticationImpl oidcModule = (OidcModuleAuthenticationImpl) AuthUtil.getProcessingModule();
            try {
                OidcUser principal = (OidcUser) oidcAuthentication.getPrincipal();
                oidcAuthenticationToken.setDetails(principal);
                Map<String, Object> attributes = principal.getAttributes();
                String enteredUsername;
                ClientRegistration config = oidcModule.getClientsRepository().findByRegistrationId(
                        oidcAuthenticationToken.getClientRegistration().getRegistrationId());
                String nameOfSamlAttribute = config.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
                if (!attributes.containsKey(nameOfSamlAttribute)){
                    LOGGER.error("Couldn't find attribute for username in oidc response");
                    throw new AuthenticationServiceException("web.security.auth.oidc.username.null");
                } else {
                    enteredUsername = String.valueOf(attributes.get(nameOfSamlAttribute));
                    if (StringUtils.isEmpty(enteredUsername)) {
                        LOGGER.error("Saml attribute, which define username don't contains value");
                        throw new AuthenticationServiceException("web.security.auth.saml2.username.null");
                    }
                }
                PreAuthenticationContext authContext = new PreAuthenticationContext(enteredUsername, focusType, requireAssignment);
                if (channel != null) {
                    authContext.setSupportActivationByChannel(channel.isSupportActivationByChannel());
                }
                token = authenticationEvaluator.authenticateUserPreAuthenticated(connEnv, authContext);
            } catch (AuthenticationException e) {
                oidcModule.setAuthentication(oidcAuthenticationToken);
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
        return oidcProvider.supports(authentication);
    }

//    public static class MidpointSaml2AuthenticatedPrincipal extends DefaultSaml2AuthenticatedPrincipal{
//
//        private final String spNameQualifier;
//        private final String nameIdFormat;
//
//        public MidpointSaml2AuthenticatedPrincipal(String name, Map<String, List<Object>> attributes, NameID nameID) {
//            super(name, attributes);
//            spNameQualifier = nameID.getSPNameQualifier();
//            nameIdFormat = nameID.getFormat();
//        }
//
//        public String getNameIdFormat() {
//            return nameIdFormat;
//        }
//
//        public String getSpNameQualifier() {
//            return spNameQualifier;
//        }
//    }
}
