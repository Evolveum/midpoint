/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;

import com.evolveum.midpoint.security.api.SecurityUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author Radovan Semancik
 * @author skublik
 */
public abstract class MidPointAbstractAuthenticationProvider<T extends AbstractAuthenticationContext>
        implements AuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointAbstractAuthenticationProvider.class);

    protected abstract AuthenticationEvaluator<T> getEvaluator();

    @Override
    public Authentication authenticate(Authentication originalAuthentication) throws AuthenticationException {

        AuthenticationRequirements authRequirements = new AuthenticationRequirements();
        try {
            Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
            Authentication processingAuthentication = originalAuthentication;
            if (isAnonymous(originalAuthentication)) {
                return originalAuthentication; // hack for specific situation when user is anonymous, but accessDecisionManager resolve it
            }
            processingAuthentication = initAuthRequirements(processingAuthentication, originalAuthentication, actualAuthentication,
                    authRequirements);
            Authentication token = internalAuthentication(processingAuthentication, authRequirements.requireAssignment,
                    authRequirements.channel, authRequirements.focusType);

            if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
                ModuleAuthenticationImpl moduleAuthentication = (ModuleAuthenticationImpl) getProcessingModule(mpAuthentication);
                if (token.getPrincipal() instanceof MidPointPrincipal principal) {
                    token = createNewAuthenticationToken(token,
                            mpAuthentication.getAuthenticationChannel().resolveAuthorities(principal.getAuthorities()));
                } else {
                    token = createNewAuthenticationToken(token, token.getAuthorities());
                }
                writeAuthentication(processingAuthentication, mpAuthentication, moduleAuthentication, token);

                return mpAuthentication;
            }

            return token;
        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication error: {}", e.getMessage(), e);
            throw e;
        } catch (RuntimeException | Error e) {
            // Make sure to explicitly log all runtime errors here. Spring security is doing very poor job and does not log this properly.
            LOGGER.error("Unexpected exception during authentication: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isAnonymous(Authentication originalAuthentication) {
        if (originalAuthentication instanceof MidpointAuthentication mpAuthentication) {
            ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
            return moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken;
        }
        return false;
    }

    private Authentication initAuthRequirements(Authentication processingAuthentication, Authentication originalAuthentication,
            Authentication actualAuthentication, AuthenticationRequirements authRequirements) {
        if (originalAuthentication instanceof MidpointAuthentication mpAuthentication) {
            initAuthRequirements(mpAuthentication, authRequirements);
            ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
            return moduleAuthentication.getAuthentication();
        } else if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
            initAuthRequirements(mpAuthentication, authRequirements);
        }
        return processingAuthentication;
    }

    private void initAuthRequirements(MidpointAuthentication mpAuthentication, AuthenticationRequirements authRequirements) {
        ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
        if (moduleAuthentication != null && moduleAuthentication.getFocusType() != null) {
            authRequirements.focusType = PrismContext.get().getSchemaRegistry()
                    .determineCompileTimeClass(moduleAuthentication.getFocusType());
        }
        authRequirements.requireAssignment = mpAuthentication.getSequence().getRequireAssignmentTarget();
        authRequirements.channel = mpAuthentication.getAuthenticationChannel();
    }

    protected AuthenticationRequirements initAuthRequirements(Authentication actualAuthentication) {
        AuthenticationRequirements authRequirements = new AuthenticationRequirements();
        if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
            initAuthRequirements(mpAuthentication, authRequirements);
        }
        return authRequirements;
    }

    protected void writeAuthentication(
            Authentication originalAuthentication, MidpointAuthentication mpAuthentication,
            ModuleAuthenticationImpl moduleAuthentication, Authentication token) {
        Object principal = token.getPrincipal();
        if (principal instanceof MidPointPrincipal) {
            mpAuthentication.setPrincipal(principal);
        }

        moduleAuthentication.setAuthentication(token);
    }

    protected ModuleAuthentication getProcessingModule(MidpointAuthentication mpAuthentication) {
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        if (moduleAuthentication == null) {
            LOGGER.debug("Couldn't find processing module authentication {}", mpAuthentication);    //todo temporary decision for mid-8727
            throw new AuthenticationServiceException("web.security.auth.module.null");
        }
        return moduleAuthentication;
    }

    protected ConnectionEnvironment createEnvironment(AuthenticationChannel channel) {
        ConnectionEnvironment connEnv;
        if (channel != null) {
            connEnv = ConnectionEnvironment.create(channel.getChannelId());
        } else {
            connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_USER_URI);
        }

        Authentication processingAuthentication = SecurityUtil.getAuthentication();
        if (processingAuthentication instanceof MidpointAuthentication mpAuthentication) {
            connEnv.setSessionIdOverride(mpAuthentication.getSessionId());
            connEnv.setSequenceIdentifier(mpAuthentication.getSequenceIdentifier());
            connEnv.setModuleIdentifier(mpAuthentication.getProcessingModuleAuthenticationIdentifier());
        }
        return connEnv;
    }

    protected abstract Authentication internalAuthentication(
            Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException;

    protected abstract Authentication createNewAuthenticationToken(
            Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities);

    public boolean supports(Class<?> authenticationClass, Authentication authentication) {
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return supports(authenticationClass);
        }
        ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
        if (moduleAuthentication == null || moduleAuthentication.getAuthentication() == null) {
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
        if (obj == null) {return false;}
        if (this.getClass() != obj.getClass()) {return false;}
        return (this.hashCode() == obj.hashCode());
    }

    protected Collection<? extends ItemDelta<?, ?>> computeModifications(@NotNull FocusType before, @NotNull FocusType after) {
        ObjectDelta<? extends FocusType> delta = ((PrismObject<FocusType>) before.asPrismObject())
                .diff((PrismObject<FocusType>) after.asPrismObject(), ParameterizedEquivalenceStrategy.LITERAL);
        assert delta.isModify();
        return delta.getModifications();
    }

    static class AuthenticationRequirements {
        List<ObjectReferenceType> requireAssignment = null;
        AuthenticationChannel channel = null;
        Class<? extends FocusType> focusType = UserType.class;
    }

    protected String getChannel() {
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication
                && ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel() != null) {
            return ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel().getChannelId();
        } else {
            return SchemaConstants.CHANNEL_USER_URI;
        }
    }

    protected ConnectionEnvironment createConnectEnvironment(String channel) {
        ConnectionEnvironment env = ConnectionEnvironment.create(channel);
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication mpAuthentication) {
            if (mpAuthentication.getSessionId() != null) {
                env.setSessionIdOverride(((MidpointAuthentication) actualAuthentication).getSessionId());
            }
            env.setSequenceIdentifier(mpAuthentication.getSequenceIdentifier());
        }
        return env;
    }
}
