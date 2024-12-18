/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.evaluator.PreAuthenticatedEvaluatorImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.authentication.api.evaluator.context.FocusIdentificationAuthenticationContext;
import com.evolveum.midpoint.authentication.api.evaluator.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FocusIdentificationProvider extends MidpointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(FocusIdentificationProvider.class);

    @Autowired private PreAuthenticatedEvaluatorImpl<FocusIdentificationAuthenticationContext> evaluator;



    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

//        if (StringUtils.isNotBlank(enteredUsername)) {
//            LOGGER.debug("User already identified, skipping focusIdentification module.");
//            return authentication;
//        }
        ConnectionEnvironment connEnv = createEnvironment(channel);

        if (!(authentication instanceof FocusVerificationToken focusVerificationToken)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        Map<ItemPath, String> attrValuesMap = focusVerificationToken.getDetails(); //TODO should not be details
        if (attrValuesMap == null || attrValuesMap.isEmpty()) {
            // E.g. no user name or other required property provided when resetting the password.
            // Hence DEBUG, not ERROR, and BadCredentialsException, not AuthenticationServiceException.
            LOGGER.debug("No details provided: {}", authentication);
            throw new BadCredentialsException("web.security.provider.resetPassword.invalid.credentials");
        }
        ModuleAuthentication moduleAuthentication = AuthUtil.getProcessingModule();
        List<ModuleItemConfigurationType> itemsConfig = null;
        if (moduleAuthentication instanceof FocusIdentificationModuleAuthenticationImpl focusModuleAuthentication) {
            itemsConfig = focusModuleAuthentication.getModuleConfiguration();
        }
        if (blankAttributeValueExist(attrValuesMap, itemsConfig)) {
            //all attributes are mandatory to be filled in
            LOGGER.debug("No value was provided for mandatory attribute(s): {}", authentication);
            throw new BadCredentialsException("web.security.provider.resetPassword.invalid.credentials");
        }
        FocusIdentificationAuthenticationContext ctx = new FocusIdentificationAuthenticationContext(
                attrValuesMap,
                focusType,
                itemsConfig,
                channel);
        Authentication token = evaluator.authenticate(connEnv, ctx);
        UsernamePasswordAuthenticationToken pwdToken = new UsernamePasswordAuthenticationToken(token.getPrincipal(), token.getCredentials());
        pwdToken.setAuthenticated(false);
        return pwdToken;

    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return FocusVerificationToken.class.equals(authentication);
    }

    private boolean blankAttributeValueExist(Map<ItemPath, String> attrValuesMap, List<ModuleItemConfigurationType> itemsConfig) {
        if (itemsConfig == null) {
            return true;
        }
        for (ModuleItemConfigurationType itemConfig : itemsConfig) {
            if (StringUtils.isBlank(attrValuesMap.get(itemConfig.getPath().getItemPath()))) {
                return true;
            }
        }
        return false;
    }

}
