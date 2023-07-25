/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.CorrelationVerificationToken;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.FocusVerificationToken;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.context.FocusIdentificationAuthenticationContext;
import com.evolveum.midpoint.model.api.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorFactoryRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

public class CorrelationProvider extends MidPointAbstractAuthenticationProvider<PasswordAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationProvider.class);

    @Autowired
    private AuthenticationEvaluator<PasswordAuthenticationContext> passwordAuthenticationEvaluator;

    @Override
    protected AuthenticationEvaluator<PasswordAuthenticationContext> getEvaluator() {
        return passwordAuthenticationEvaluator;
    }

    //TODO move to the evaluator?
    protected CorrelatorFactoryRegistry correlatorFactoryRegistry;

    @Autowired protected CorrelationService correlationService;
    @Autowired private TaskManager taskManager;
    @Autowired private SecurityContextManager securityContextManager;

    @Override
    public Authentication authenticate(Authentication originalAuthentication) throws AuthenticationException {
        return super.authenticate(originalAuthentication);
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }

        ConnectionEnvironment connEnv = createEnvironment(channel, authentication);

        try {
            Authentication token = null;
            if (authentication instanceof CorrelationVerificationToken) {

                CompleteCorrelationResult correlationResult =  securityContextManager.runPrivileged( () -> {
                        Task task = taskManager.createTaskInstance("correlate");
                        task.setChannel(SchemaConstants.CHANNEL_LOGIN_RECOVERY_URI);
                        CorrelationVerificationToken correlationToken = (CorrelationVerificationToken) authentication;
                        try {
                            CompleteCorrelationResult result = correlationService.correlate(correlationToken.getPreFocus(),
                                    new CorrelatorDiscriminator(correlationToken.getCorrelatorName(), CorrelationUseType.USERNAME_RECOVERY),
                                    task, task.getResult());
                            return result;//.getOwner();
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        } catch (ExpressionEvaluationException e) {
                            throw new RuntimeException(e);
                        } catch (CommunicationException e) {
                            throw new RuntimeException(e);
                        } catch (SecurityViolationException e) {
                            throw new RuntimeException(e);
                        } catch (ConfigurationException e) {
                            throw new RuntimeException(e);
                        } catch (ObjectNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });


                correlationResult.getOwner();

                UsernamePasswordAuthenticationToken pwdToken = new UsernamePasswordAuthenticationToken(token.getPrincipal(), token.getCredentials());
                pwdToken.setAuthenticated(false);
                return pwdToken;

            } else {
                LOGGER.error("Unsupported authentication {}", authentication);
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }
        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication failed for {}: {}", authentication, e.getMessage());
            throw e;
        }
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

}
