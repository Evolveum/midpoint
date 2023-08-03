/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.evaluator;

import com.evolveum.midpoint.authentication.api.AutheticationFailedData;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.FocusAuthenticationResultRecorder;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public abstract class AuthenticationEvaluatorImpl<T extends AbstractAuthenticationContext, A extends Authentication>
        implements AuthenticationEvaluator<T, A> {

    @Autowired private FocusAuthenticationResultRecorder authenticationRecorder;
    @Autowired private ModelAuditRecorder auditRecorder;
    private GuiProfiledPrincipalManager focusProfileService;

    @Autowired
    public void setPrincipalManager(GuiProfiledPrincipalManager focusProfileService) {
        this.focusProfileService = focusProfileService;
    }


    @NotNull
    protected <C extends AbstractAuthenticationContext> MidPointPrincipal getAndCheckPrincipal(ConnectionEnvironment connEnv, C authCtx, boolean supportActivationCheck) {
        ObjectQuery query = authCtx.createFocusQuery();
        String username = authCtx.getUsername();
        if (query == null) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "no username");
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }

        Class<? extends FocusType> clazz = authCtx.getPrincipalType();
        MidPointPrincipal principal;
        try {
            principal = focusProfileService.getPrincipal(query, clazz);
        } catch (ObjectNotFoundException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "no focus");
            throw new UsernameNotFoundException(AuthUtil.generateBadCredentialsMessageKey(SecurityContextHolder.getContext().getAuthentication()));
        } catch (SchemaException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "schema error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (CommunicationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "communication error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (ConfigurationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "configuration error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (SecurityViolationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "security violation");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        } catch (ExpressionEvaluationException e) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "expression error");
            throw new InternalAuthenticationServiceException("web.security.provider.invalid");
        }

        if (principal == null) {
            recordModuleAuthenticationFailure(username, null, connEnv, null, "no focus");
            throw new UsernameNotFoundException(AuthUtil.generateBadCredentialsMessageKey(SecurityContextHolder.getContext().getAuthentication()));
        }

        if (supportActivationCheck && !principal.isEnabled()) {
            recordModuleAuthenticationFailure(principal.getUsername(), principal, connEnv, null, "focus disabled");
            throw new DisabledException("web.security.provider.disabled");
        }
        return principal;
    }

    protected boolean hasNoAuthorizations(MidPointPrincipal principal) {
        for (Authorization auth : principal.getAuthorities()) {
            if (!auth.getAction().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected void recordModuleAuthenticationSuccess(@NotNull MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            boolean audit) {
        authenticationRecorder.recordModuleAuthenticationAttemptSuccess(principal, connEnv);
    }


    protected void recordModuleAuthenticationFailure(String username, MidPointPrincipal principal, @NotNull ConnectionEnvironment connEnv,
            CredentialPolicyType credentialsPolicy, String reason) {
        if (principal != null) {
            authenticationRecorder.recordModuleAuthenticationAttemptFailure(principal, credentialsPolicy, connEnv);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication mpAuthentication) {
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null) {
                moduleAuthentication.setFailureData(new AutheticationFailedData(reason, username));
            }
        }
    }

    protected void auditAuthenticationFailure(String username, ConnectionEnvironment connEnv, String reason) {
        auditRecorder.auditLoginFailure(username, null, connEnv, reason);
    }

    protected void auditAuthenticationSuccess(ObjectType object, ConnectionEnvironment connEnv) {
        auditRecorder.auditLoginSuccess(object, connEnv);
    }


}
