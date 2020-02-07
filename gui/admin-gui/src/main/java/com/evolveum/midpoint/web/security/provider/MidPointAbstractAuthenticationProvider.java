/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.provider;

import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 * @author Radovan Semancik
 * @author skublik
 */
public abstract class MidPointAbstractAuthenticationProvider<T extends AbstractAuthenticationContext> implements AuthenticationProvider {//}, MessageSourceAware {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointAbstractAuthenticationProvider.class);

//    private MessageSourceAccessor messages;

//    @Override
//    public void setMessageSource(MessageSource messageSource) {
//        this.messages = new MessageSourceAccessor(messageSource);
//    }
//
//    @Autowired
//    private transient
//    private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;

    protected abstract AuthenticationEvaluator<T> getEvaluator();

    @Override
    public Authentication authenticate(Authentication originalAuthentication) throws AuthenticationException {

        List<ObjectReferenceType> requireAssignment = null;
        AuthenticationChannel channel = null;
        try {
            Authentication processingAuthentication = originalAuthentication;
            if (originalAuthentication instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) originalAuthentication;
                ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
                if (moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken) {
                    return mpAuthentication; // hack for specific situation when user is anonymous, but accessDecisionManager resolve it
                }
                processingAuthentication = moduleAuthentication.getAuthentication();
                requireAssignment = mpAuthentication.getSequence().getRequireAssignmentTarget();
            }
            Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
            if (actualAuthentication instanceof MidpointAuthentication) {
                requireAssignment = ((MidpointAuthentication) actualAuthentication).getSequence().getRequireAssignmentTarget();
                channel = ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel();
            }
            Authentication token = internalAuthentication(processingAuthentication, requireAssignment, channel);

            if (actualAuthentication instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) actualAuthentication;
                ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
                MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();
                token = createNewAuthenticationToken(token, mpAuthentication.getAuthenticationChannel().resolveAuthorities(principal.getAuthorities()));
                writeAutentication(originalAuthentication, mpAuthentication, moduleAuthentication, token);

                return mpAuthentication;
            }

            return token;


        } catch (RuntimeException | Error e) {
            // Make sure to explicitly log all runtime errors here. Spring security is doing very poor job and does not log this properly.
            LOGGER.error("Authentication (runtime) error: {}", e.getMessage(), e);
            throw e;
        }
    }

    protected void writeAutentication(Authentication originalAuthentication, MidpointAuthentication mpAuthentication, ModuleAuthentication moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal != null && principal instanceof MidPointUserProfilePrincipal) {
            mpAuthentication.setPrincipal((MidPointUserProfilePrincipal) principal);
        }

        moduleAuthentication.setAuthentication(token);
    }

    protected ModuleAuthentication getProcessingModule(MidpointAuthentication mpAuthentication) {
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        if (moduleAuthentication == null) {
            LOGGER.error("Couldn't find processing module authentication {}", mpAuthentication);
            throw new AuthenticationServiceException("web.security.auth.module.null"); //
        }
        return moduleAuthentication;
    }

    protected ConnectionEnvironment createEnviroment(AuthenticationChannel channel) {
        if (channel != null) {
            ConnectionEnvironment connEnv = ConnectionEnvironment.create(channel.getChannelId());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof MidpointAuthentication) {
                connEnv.setSessionIdOverride(((MidpointAuthentication) authentication).getSessionId());
            }
            return connEnv;
        } else {
            return ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI);
        }
    }

    protected abstract Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment, AuthenticationChannel channel) throws AuthenticationException;

    protected abstract Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities);

//    @Override
//    public boolean supports(Class<?> authentication) {
//        if (UsernamePasswordAuthenticationToken.class.equals(authentication)) {
//            return true;
//        }
//        if (PreAuthenticatedAuthenticationToken.class.equals(authentication)) {
//            return true;
//        }
//
////        if (MidpointAuthentication.class.equals(authentication)) {
////            return true;
////        }
//
//        return false;
//    }

    public boolean supports(Class<?> authenticationClass, Authentication authentication) {
        if (!(authentication instanceof MidpointAuthentication)) {
            return supports(authenticationClass);
        }
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
        if (mpAuthentication == null || moduleAuthentication == null || moduleAuthentication.getAuthentication() == null) {
            return false;
        }
        if (moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return true; // hack for specific situation when user is anonymous, but accessDecisionManager resolve it
        }
        return supports(moduleAuthentication.getAuthentication().getClass());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEvaluator() == null) ? 0 : getEvaluator().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        return (this.hashCode() == obj.hashCode());
    }
}
