/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.provider;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.module.authentication.LdapAuthenticationToken;
import com.evolveum.midpoint.web.security.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.MidpointDirContextAdapter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private LdapAuthenticationProvider authenticatorProvider;

    @Autowired
    private ModelAuditRecorder auditProvider;

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
                    throw new BadCredentialsException("LdapAuthentication.incorrect.value");
                }
                MidPointPrincipal midPointPrincipal = (MidPointPrincipal) principal;
                UserType userType = midPointPrincipal.getUser();

                if (userType == null) {
                    throw new BadCredentialsException("LdapAuthentication.bad.user");
                }

                auditProvider.auditLoginSuccess(userType, ConnectionEnvironment.create(SchemaConstants.CHANNEL_GUI_USER_URI));
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
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment, AuthenticationChannel channel) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof MidPointUserProfilePrincipal) {
            return authentication;
        }

        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnviroment(channel);

        try {

            Authentication token;
            if (authentication instanceof LdapAuthenticationToken) {
                String enteredPassword = (String) authentication.getCredentials();


                token = this.authenticatorProvider.authenticate(authentication);
            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            MidPointPrincipal principal = (MidPointPrincipal)token.getPrincipal();

            LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                    authentication.getClass().getSimpleName(), principal.getAuthorities());
            return token;

        } catch (InternalAuthenticationServiceException e) {
            // This sometimes happens ... for unknown reasons the underlying libraries cannot
            // figure out correct exception. Which results to wrong error message (MID-4518)
            // So, be smart here and try to figure out correct error.
            throw processInternalAuthenticationException(e, e);

        } catch (IncorrectResultSizeDataAccessException e) {
            LOGGER.error("Failed to authenticate user {}. Error: {}", authentication.getName(), e.getMessage(), e);
            throw new BadCredentialsException("LdapAuthentication.bad.user", e);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to authenticate user {}. Error: {}", authentication.getName(), e.getMessage(), e);
            auditProvider.auditLoginFailure(authentication.getName(), null, connEnv, "bad credentials");
            throw e;
        }
    }

    @Override
    protected AuthenticationEvaluator getEvaluator() {
        return null;
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
}
