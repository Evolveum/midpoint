/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.web.security.util.RequestState;
import com.evolveum.midpoint.web.security.util.StateOfModule;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.SamlAuthenticationResponseFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class MidpointSamlAuthenticationResponseFilter extends SamlAuthenticationResponseFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointSamlAuthenticationResponseFilter.class);

    private UserProfileService userProfileService;

    private final SamlProviderProvisioning<ServiceProviderService> provisioning;

    public MidpointSamlAuthenticationResponseFilter(SamlProviderProvisioning<ServiceProviderService> provisioning) {
        super(provisioning);
        this.provisioning = provisioning;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean sendedRequest = false;
        Saml2ModuleAuthentication moduleAuthentication = null;
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            if (RequestState.SENDED.equals(moduleAuthentication.getRequestState())) {
                sendedRequest = true;
            }
        }
        boolean requiresAuthentication = requiresAuthentication((HttpServletRequest) req, (HttpServletResponse) res);

        if (!requiresAuthentication && sendedRequest) {
            AuthenticationServiceException exception = new AuthenticationServiceException("Midpoint saml module doesn't receive response from Identity Provider server."); //TODO localization
            unsuccessfulAuthentication((HttpServletRequest) req, (HttpServletResponse) res, exception);
            return;
        } else {
            super.doFilter(req, res, chain);
        }
    }

    //    public void setUserProfileService(UserProfileService userProfileService) {
//        this.userProfileService = userProfileService;
//    }

//    @Override
//    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
////        DefaultSamlAuthentication authentication = (DefaultSamlAuthentication) super.attemptAuthentication(request, response);
////
////        MidPointUserProfilePrincipal midPointUserProfilePrincipal = null;
////
////        if (authentication.getAssertion() != null
////        && authentication.getAssertion().getAttributes() != null
////        && !authentication.getAssertion().getAttributes().isEmpty()) {
////            List<Attribute> attributes = ((DefaultSamlAuthentication) authentication).getAssertion().getAttributes();
////            for (Attribute attribute : attributes){
////                if (attribute.getFriendlyName().equals("uid")) {
////                    try {
////                        midPointUserProfilePrincipal = userProfileService.getPrincipal((String)attribute.getValues().get(0));
////                    } catch (ObjectNotFoundException e) {
////                        e.printStackTrace();
////                    } catch (SchemaException e) {
////                        e.printStackTrace();
////                    } catch (CommunicationException e) {
////                        e.printStackTrace();
////                    } catch (ConfigurationException e) {
////                        e.printStackTrace();
////                    } catch (SecurityViolationException e) {
////                        e.printStackTrace();
////                    } catch (ExpressionEvaluationException e) {
////                        e.printStackTrace();
////                    }
////                }
////            }
////
////        }
////
////        MidpointSamlAuthentication mpAuthentication = new MidpointSamlAuthentication(
////                true,
////                authentication.getAssertion(),
////                authentication.getAssertingEntityId(),
////                authentication.getHoldingEntityId(),
////                authentication.getRelayState(),
////                midPointUserProfilePrincipal
////        );
////        mpAuthentication.setResponseXml(authentication.getResponseXml());
////
////        return getAuthenticationManager().authenticate(mpAuthentication);
//
//        String responseData = getSamlResponseData(request);
//        if (!hasText(responseData)) {
//            processingModuleauthenticationFail();
//            throw new AuthenticationCredentialsNotFoundException("SAMLResponse parameter missing");
//        }
//
//        ServiceProviderService provider = getProvisioning().getHostedProvider();
//
//        Response r = provider.fromXml(responseData, true, GET.matches(request.getMethod()), Response.class);
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace("Received SAMLResponse XML:" + r.getOriginalXML());
//        }
//        IdentityProviderMetadata remote = provider.getRemoteProvider(r);
//
//        ValidationResult validationResult = provider.validate(r);
//        if (validationResult.hasErrors()) {
//            processingModuleauthenticationFail();
//            throw new InsufficientAuthenticationException(
//                    validationResult.toString()
//            );
//        }
//
//        DefaultSamlAuthentication authentication = new DefaultSamlAuthentication(
//                true,
//                r.getAssertions().get(0),
//                remote.getEntityId(),
//                provider.getMetadata().getEntityId(),
//                request.getParameter("RelayState")
//        );
//        authentication.setResponseXml(r.getOriginalXML());
//
//        return getAuthenticationManager().authenticate(authentication);
//
//    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            Saml2ModuleAuthentication moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setState(StateOfModule.FAILURE);
            moduleAuthentication.setRequestState(RequestState.RECEIVED);
        }

//        if (logger.isDebugEnabled()) {
//            logger.debug("Authentication request failed: " + failed.toString(), failed);
//            logger.debug("Updated SecurityContextHolder to contain null Authentication");
//            logger.debug("Delegating to authentication failure handler " + failureHandler);
//        }

        getRememberMeServices().loginFail(request, response);

        getFailureHandler().onAuthenticationFailure(request, response, failed);
    }

    //    private void processingModuleauthenticationFail() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication instanceof MidpointAuthentication) {
//            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
//            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
//            moduleAuthentication.setState(StateOfModule.FAILURE);
//        }
//    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        if (authResult instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authResult;
            Saml2ModuleAuthentication moduleAuthentication = (Saml2ModuleAuthentication) mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.setState(StateOfModule.SUCCESSFULLY);
            moduleAuthentication.setRequestState(RequestState.RECEIVED);
        }

        super.successfulAuthentication(request, response, chain, authResult);
    }

    protected SamlProviderProvisioning<ServiceProviderService> getProvisioning() {
        return provisioning;
    }

    protected String getSamlResponseData(HttpServletRequest request) {
        return request.getParameter("SAMLResponse");
    }

}
