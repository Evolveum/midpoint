/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.MailNonceAuthenticationToken;

import com.evolveum.midpoint.authentication.impl.module.authentication.MailNonceModuleAuthenticationImpl;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.authentication.api.evaluator.context.NonceAuthenticationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class MailNonceProvider extends AbstractCredentialProvider<NonceAuthenticationContext> {

    private static final Trace LOGGER = TraceManager.getTrace(MailNonceProvider.class);

    @Autowired
    private AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken> nonceAuthenticationEvaluator;

    @Autowired
    private SecurityContextManager securityContextManager;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ModelService modelService;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelInteractionService modelInteractionService;

    @Override
    protected AuthenticationEvaluator<NonceAuthenticationContext, UsernamePasswordAuthenticationToken> getEvaluator() {
        return nonceAuthenticationEvaluator;
    }

    @Override
    protected Authentication doAuthenticate(
            Authentication authentication, List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        String enteredUsername = (String) authentication.getPrincipal();
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        Authentication token;
        if (authentication instanceof MailNonceAuthenticationToken) {
            String nonce = (String) authentication.getCredentials();

            UserType user = searchUser(enteredUsername);
            NonceCredentialsPolicyType noncePolicy = getNoncePolicy(user);
            NonceAuthenticationContext authContext = new NonceAuthenticationContext(enteredUsername,
                    focusType, nonce, noncePolicy, requireAssignment, channel);
            token = getEvaluator().authenticate(connEnv, authContext);
            removeNonceAfterSuccessullAuthentication(user);
        } else {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;

    }

    private UserType searchUser(String enteredUsername) {
        if (StringUtils.isBlank(enteredUsername)) {
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }
        UserType user = AuthSequenceUtil.searchUserPrivileged(enteredUsername, securityContextManager, taskManager,
                modelService, prismContext);
        if (user == null) {
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials");
        }
        return user;
    }

    private void removeNonceAfterSuccessullAuthentication(UserType user) {
        securityContextManager.runPrivileged((Producer<Void>) () -> removeNonce(user));
    }

    private Void removeNonce(UserType user) {
        try {
            NonceType nonce = user.getCredentials().getNonce();
            ObjectDelta<UserType> deleteNonce = PrismContext.get().deltaFactory()
                    .object()
                    .createModificationDeleteContainer(UserType.class, user.getOid(),
                            ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_NONCE),
                            nonce.clone());

            Task task = taskManager.createTaskInstance("Remove nonce from user");
            modelService.executeChanges(MiscSchemaUtil.createCollection(deleteNonce), null, task, task.getResult());

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't remove nonce from user {}", user, e);
        }
        return null;
    }

    private NonceCredentialsPolicyType getNoncePolicy(UserType user) {
        if (illegalAuthentication()){
            return null;
        }

        MidpointAuthentication authentication = AuthUtil.getMidpointAuthentication();
        ModuleAuthentication moduleAuth = authentication.getProcessingModuleAuthentication();
        String nameOfCredential = ((MailNonceModuleAuthenticationImpl) moduleAuth).getCredentialName();

        return AuthSequenceUtil.determineNoncePolicy(user.asPrismObject(), nameOfCredential, taskManager, modelInteractionService);
    }

    private boolean illegalAuthentication() {
        MidpointAuthentication authentication = AuthUtil.getMidpointAuthentication();

        ModuleAuthentication moduleAuth = authentication.getProcessingModuleAuthentication();
        if (!(moduleAuth instanceof MailNonceModuleAuthenticationImpl)) {
            LOGGER.debug("Actual processing authentication module isn't MailNonceModuleAuthentication");
            return true;
        }
        String nameOfCredential = ((MailNonceModuleAuthenticationImpl) moduleAuth).getCredentialName();
        if (nameOfCredential == null) {
            LOGGER.debug("Name of credential in processing module is null");
            return true;
        }
        return false;
    }

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof UsernamePasswordAuthenticationToken) {
            return new MailNonceAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MailNonceAuthenticationToken.class.equals(authentication);
    }

    @Override
    public Class getTypeOfCredential() {
        return NonceCredentialsPolicyType.class;
    }

}
