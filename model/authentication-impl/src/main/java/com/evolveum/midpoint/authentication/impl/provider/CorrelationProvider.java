/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.CorrelationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.CorrelationVerificationToken;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.model.api.correlator.CandidateOwners;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

public class CorrelationProvider extends MidpointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationProvider.class);


    @Autowired protected CorrelationService correlationService;
    @Autowired private TaskManager taskManager;

    @Autowired private GuiProfiledPrincipalManager focusProfileService;


    @Override
    public Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {

        //TODO what if the user was already authenticated?
        if (!(authentication instanceof CorrelationVerificationToken correlationVerificationToken)) {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        try {
            ModuleAuthentication moduleAuthentication = AuthUtil.getProcessingModule();
            if (!(moduleAuthentication instanceof CorrelationModuleAuthenticationImpl correlationModuleAuthentication)) {
                LOGGER.error("Correlation module authentication is not set");
                throw new AuthenticationServiceException("web.security.provider.unavailable");
            }

            CompleteCorrelationResult correlationResult = correlate(correlationVerificationToken,
                    determineArchetypeOid(),
                    correlationModuleAuthentication.getCandidateOids(),
                    focusType);
            ObjectType owner = correlationResult.getOwner();

            if (owner == null && !candidateOwnerExist(correlationResult)) {
                throw new AuthenticationServiceException("No identity is found.");
            }

            correlationModuleAuthentication.addAttributes(correlationVerificationToken.getDetails());

            correlationModuleAuthentication.setPreFocus(correlationVerificationToken.getPreFocus(focusType,
                    correlationModuleAuthentication.getProcessedAttributes()));
            if (owner != null) {
                correlationModuleAuthentication.rewriteOwner(owner);
                return authentication;
            } else if (isLastCorrelatorProcessing(correlationModuleAuthentication, correlationVerificationToken)) {
                if (candidateOwnerExist(correlationResult)) {
                    rewriteCandidatesToOwners(correlationResult.getCandidateOwnersMap(), correlationModuleAuthentication);
                } else {
                    correlationModuleAuthentication.clearOwners();
                }

                isOwnersNumberUnderRestriction(correlationModuleAuthentication);

                return authentication;
            }

            CandidateOwners owners = correlationResult.getCandidateOwnersMap();
            correlationModuleAuthentication.rewriteCandidateOwners(owners);

            return authentication;
        } catch (Exception e) {
            LOGGER.error("Cannot correlate user, {}", e.getMessage(), e);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

    }

    private boolean isLastCorrelatorProcessing(CorrelationModuleAuthenticationImpl correlationModuleAuthentication,
            CorrelationVerificationToken token) {
        return correlationModuleAuthentication.isLastCorrelator()
                && correlationModuleAuthentication.currentCorrelatorIndexEquals(token.getCurrentCorrelatorIndex());
    }

    private String determineArchetypeOid() {
        Authentication processingAuthentication = SecurityUtil.getAuthentication();
        if (processingAuthentication instanceof MidpointAuthentication mpAuthentication) {
            return mpAuthentication.getArchetypeOid();
        }
        return null;
    }

    private boolean candidateOwnerExist(CompleteCorrelationResult correlationResult) {
        return correlationResult.getCandidateOwnersMap() != null && !correlationResult.getCandidateOwnersMap().isEmpty();
    }

    private void rewriteCandidatesToOwners(@NotNull CandidateOwners candidateOwners,
            CorrelationModuleAuthenticationImpl correlationModuleAuthentication) {
        correlationModuleAuthentication.clearOwners();
        candidateOwners.objectBasedValues()
                .forEach(c -> correlationModuleAuthentication.addOwnerIfNotExist(c.getValue()));
    }

    private void isOwnersNumberUnderRestriction(CorrelationModuleAuthenticationImpl correlationModuleAuthentication) {
        if (correlationModuleAuthentication.getCorrelationMaxUsersNumber() == null) {
            return;
        }
        if (correlationModuleAuthentication.getOwners().size() > correlationModuleAuthentication.getCorrelationMaxUsersNumber()) {
            LOGGER.error("Correlation result owners number exceeds the threshold.");
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }
    }

    private CompleteCorrelationResult correlate(
            CorrelationVerificationToken correlationToken,
            String archetypeOid,
            Set<String> candidatesOids,
            Class<? extends FocusType> focusType) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        Task task = taskManager.createTaskInstance("correlate");
        task.setChannel(SchemaConstants.CHANNEL_IDENTITY_RECOVERY_URI);

            //TODO cadidateOids as a parameter, + define somehow threshold - how many users migh be returned from the correlation
            return correlationService.correlate(
                    correlationToken.getPreFocus(focusType),
                    archetypeOid,
                    candidatesOids,
                    CorrelatorDiscriminator.forIdentityRecovery(correlationToken.getCorrelatorName()),
                    task, task.getResult());
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
        return CorrelationVerificationToken.class.equals(authentication);
    }

}
