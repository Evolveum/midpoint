/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.provider;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.api.util.AuthenticationEvaluatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.LdapAuthenticationToken;
import com.evolveum.midpoint.web.security.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;
import java.util.List;

public class MidPointLdapAuthenticationProvider extends MidPointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointLdapAuthenticationProvider.class);

    private final LdapAuthenticationProvider authenticatorProvider;

    @Autowired private ModelAuditRecorder auditProvider;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;

    public MidPointLdapAuthenticationProvider(LdapAuthenticator authenticator) {
        this.authenticatorProvider = createAuthenticatorProvider(authenticator);
    }

    public void setUserDetailsContextMapper(UserDetailsContextMapper userDetails){
        this.authenticatorProvider.setUserDetailsContextMapper(userDetails);
    }

    public LdapAuthenticationProvider getAuthenticatorProvider() {
        return authenticatorProvider;
    }

    private LdapAuthenticationProvider createAuthenticatorProvider(LdapAuthenticator authenticator){
        return new LdapAuthenticationProvider(authenticator){
            @Override
            protected DirContextOperations doAuthentication(UsernamePasswordAuthenticationToken authentication) {
                    DirContextOperations originalDirContextOperations = super.doAuthentication(authentication);
                    if (originalDirContextOperations instanceof DirContextAdapter) {
                        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
                        if (actualAuthentication instanceof MidpointAuthentication) {
                            MidpointAuthentication mpAuthentication = (MidpointAuthentication) actualAuthentication;
                            ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
                            if (moduleAuthentication instanceof LdapModuleAuthentication){
                                //HACK because of NP in DirContextAdapter(DirContextAdapter master)
                                if (!originalDirContextOperations.isUpdateMode()) {
                                    ((DirContextAdapter) originalDirContextOperations).setUpdateMode(true);
                                    ((DirContextAdapter) originalDirContextOperations).setUpdateMode(false);
                                }
                                MidpointDirContextAdapter mpDirContextAdapter = new MidpointDirContextAdapter((DirContextAdapter)originalDirContextOperations);
                                mpDirContextAdapter.setNamingAttr(((LdapModuleAuthentication) moduleAuthentication).getNamingAttribute());

                                if (moduleAuthentication.getFocusType() != null) {
                                    Class<FocusType> focusType = WebComponentUtil.qnameToClass(prismContext, moduleAuthentication.getFocusType(), FocusType.class);
                                    mpDirContextAdapter.setFocusType(focusType);
                                }

                                AuthenticationChannel channel = mpAuthentication.getAuthenticationChannel();
                                mpDirContextAdapter.setChannel(channel);
                                mpDirContextAdapter.setConnectionEnvironment(createEnvironment(channel));

                                mpDirContextAdapter.setRequireAssignment(mpAuthentication.getSequence().getRequireAssignmentTarget());

                                return mpDirContextAdapter;
                            }
                        }
                    }
                    return originalDirContextOperations;
            }

            @Override
            protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication,
                    UserDetails user) {
                Authentication authNCtx = super.createSuccessfulAuthentication(authentication, user);

                Object principal = authNCtx.getPrincipal();
                if (!(principal instanceof MidPointPrincipal)) {
                    recordPasswordAuthenticationFailure(authentication.getName(), "not contains required assignment");
                    throw new BadCredentialsException("LdapAuthentication.incorrect.value");
                }
                MidPointPrincipal midPointPrincipal = (MidPointPrincipal) principal;
                FocusType focusType = midPointPrincipal.getFocus();

                if (focusType == null) {
                    recordPasswordAuthenticationFailure(authentication.getName(), "not contains required assignment");
                    throw new BadCredentialsException("LdapAuthentication.bad.user");
                }

                Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
                if (actualAuthentication instanceof MidpointAuthentication) {
                    MidpointAuthentication mpAuthentication = (MidpointAuthentication) actualAuthentication;
                    List<ObjectReferenceType> requireAssignment = mpAuthentication.getSequence().getRequireAssignmentTarget();
                    if (!AuthenticationEvaluatorUtil.checkRequiredAssignment(focusType.getAssignment(), requireAssignment)) {
                        recordPasswordAuthenticationFailure(midPointPrincipal.getUsername(), "not contains required assignment");
                        throw new InternalAuthenticationServiceException("web.security.flexAuth.invalid.required.assignment");
                    }
                }

                recordPasswordAuthenticationSuccess(midPointPrincipal);
                return authNCtx;
            }
        };
    }

    private RuntimeException processInternalAuthenticationException(InternalAuthenticationServiceException rootExeption, Throwable currentException) {
        if (currentException instanceof javax.naming.AuthenticationException) {
            String message = ((javax.naming.AuthenticationException)currentException).getMessage();
            if (message.contains("error code 49")) {
                // JNDI and Active Directory strike again
                return new BadCredentialsException("Invalid username and/or password.", rootExeption);
            }
        }
        Throwable cause = currentException.getCause();
        if (cause == null) {
            return rootExeption;
        } else {
            return processInternalAuthenticationException(rootExeption, cause);
        }
    }

    @Override
    protected AuthenticationEvaluator getEvaluator() {
        return null;
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment, AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }

        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        // TODO connEnv is not used... remove?
        ConnectionEnvironment connEnv = createEnvironment(channel);

        try {

            Authentication token = null;
            if (authentication instanceof LdapAuthenticationToken) {
                token = this.authenticatorProvider.authenticate(authentication);
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                recordPasswordAuthenticationFailure(authentication.getName(), "unavailable provider");
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (AuditedAuthenticationException e) {
            throw e.getCause();
        } catch (InternalAuthenticationServiceException e) {
            // This sometimes happens ... for unknown reasons the underlying libraries cannot
            // figure out correct exception. Which results to wrong error message (MID-4518)
            // So, be smart here and try to figure out correct error.
            recordPasswordAuthenticationFailure(authentication.getName(), e.getMessage());
            throw processInternalAuthenticationException(e, e);

        } catch (IncorrectResultSizeDataAccessException e) {
            LOGGER.error("Failed to authenticate user {}. Error: {}", authentication.getName(), e.getMessage(), e);
            recordPasswordAuthenticationFailure(authentication.getName(), "bad user");
            throw new BadCredentialsException("web.security.provider.invalid.credentials", e);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to authenticate user {}. Error: {}", authentication.getName(), e.getMessage(), e);
            recordPasswordAuthenticationFailure(authentication.getName(), "bad credentials");
            throw e;
        }
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection newAuthorities) {
        if (actualAuthentication instanceof LdapAuthenticationToken) {
            return new LdapAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        if (LdapAuthenticationToken.class.equals(authentication)) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.authenticatorProvider.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.authenticatorProvider.equals(obj);
    }

    public void recordPasswordAuthenticationSuccess(@NotNull MidPointPrincipal principal) {
        String channel = getChannel();
        AuthenticationBehavioralDataType behavior = AuthenticationEvaluatorUtil.getBehavior(principal.getFocus());

        FocusType focusBefore = principal.getFocus().clone();
        Integer failedLogins = behavior.getFailedLogins();
        if (failedLogins != null && failedLogins > 0) {
            behavior.setFailedLogins(0);
        }
        LoginEventType event = new LoginEventType();
        event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
        event.setFrom(SecurityUtil.getCurrentConnectionInformation().getRemoteHostAddress());

        behavior.setPreviousSuccessfulLogin(behavior.getLastSuccessfulLogin());
        behavior.setLastSuccessfulLogin(event);

        focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        recordAuthenticationSuccess(principal.getFocus(), channel);
    }

    private String getChannel() {
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication && ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel() != null) {
            return ((MidpointAuthentication) actualAuthentication).getAuthenticationChannel().getChannelId();
        } else {
            return SchemaConstants.CHANNEL_USER_URI;
        }
    }

    private void recordAuthenticationSuccess(@NotNull FocusType focusType, @NotNull String channel) {
        auditProvider.auditLoginSuccess(focusType, createConnectEnvironment(channel));
    }

    private ConnectionEnvironment createConnectEnvironment(String channel) {
        ConnectionEnvironment env = ConnectionEnvironment.create(channel);
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication && ((MidpointAuthentication) actualAuthentication).getSessionId() != null) {
            env.setSessionIdOverride(((MidpointAuthentication) actualAuthentication).getSessionId());
        }
        return env;
    }

    public void recordPasswordAuthenticationFailure(String name, String reason) {

        FocusType focus = null;
        String channel = getChannel();
        MidPointPrincipal principal = null;
        try {
            principal = focusProfileService.getPrincipal(name, getFocusType());
            focus = principal.getFocus();
        } catch (Exception e) {
            //ignore if non-exist
        }

        if (principal != null && focus != null) {
            AuthenticationBehavioralDataType behavior = AuthenticationEvaluatorUtil.getBehavior(focus);

            FocusType focusBefore = focus.clone();
            Integer failedLogins = behavior.getFailedLogins();

            if (failedLogins == null) {
                failedLogins = 1;
            } else {
                failedLogins++;
            }

            behavior.setFailedLogins(failedLogins);

            LoginEventType event = new LoginEventType();
            event.setTimestamp(clock.currentTimeXMLGregorianCalendar());
            event.setFrom(SecurityUtil.getCurrentConnectionInformation().getRemoteHostAddress());

            behavior.setLastFailedLogin(event);
            focusProfileService.updateFocus(principal, computeModifications(focusBefore, principal.getFocus()));
        }

        recordAuthenticationFailure(name, focus, channel, reason);
    }

    private Class<? extends FocusType> getFocusType() {
        Class<? extends FocusType> focusType = UserType.class;
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (actualAuthentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) actualAuthentication;
            ModuleAuthentication moduleAuthentication = getProcessingModule(mpAuthentication);
            if (moduleAuthentication != null && moduleAuthentication.getFocusType() != null) {
                focusType = WebComponentUtil.qnameToClass(prismContext, moduleAuthentication.getFocusType(), FocusType.class);
            }
        }
        return focusType;
    }

    protected void recordAuthenticationFailure(String name, FocusType focus, String channel, String reason) {
        auditProvider.auditLoginFailure(name, focus, createConnectEnvironment(channel), "bad credentials");
    }
}
