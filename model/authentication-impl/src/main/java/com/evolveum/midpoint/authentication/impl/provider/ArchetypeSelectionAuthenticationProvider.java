/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.ArchetypeSelectionAuthenticationToken;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.cxf.common.util.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

public class ArchetypeSelectionAuthenticationProvider extends MidpointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeSelectionAuthenticationProvider.class);

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment, AuthenticationChannel channel, Class<? extends FocusType> focusType) {

        //TODO do we want to check entered username? e.g. it probably doesn't have any sense to check archetype selection
        // when the user was already identified?
        if (!(authentication instanceof ArchetypeSelectionAuthenticationToken)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        var archetypeOid = ((ArchetypeSelectionAuthenticationToken) authentication).getArchetypeOid();
        var allowUndefinedArchetype = ((ArchetypeSelectionAuthenticationToken) authentication).isAllowUndefinedArchetype();
        if (StringUtils.isEmpty(archetypeOid) && !allowUndefinedArchetype) {
            LOGGER.debug("No details provided: {}", authentication);
            throw new BadCredentialsException(AuthUtil.generateBadCredentialsMessageKey(authentication));
        }
        authentication.setAuthenticated(true);
        saveArchetypeToMidpointAuthentication(archetypeOid);
        return authentication;

    }

    private void saveArchetypeToMidpointAuthentication(String archetypeOid) {
        MidpointAuthentication authentication = AuthUtil.getMidpointAuthentication();
        authentication.setArchetypeOid(archetypeOid);
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
        return ArchetypeSelectionAuthenticationToken.class.equals(authentication);
    }

}
