/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.helpers;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.security.enforcer.api.ValueAuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Helps with the authorization activities.
 */
@Component
public class AuthorizationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AuthorizationHelper.class);

    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityContextManager securityContextManager;

    public enum RequestedOperation {
        COMPLETE(ModelAuthorizationAction.COMPLETE_WORK_ITEM),
        DELEGATE(ModelAuthorizationAction.DELEGATE_WORK_ITEM);

        final ModelAuthorizationAction action;
        RequestedOperation(ModelAuthorizationAction action) {
            this.action = action;
        }
    }

    /**
     * Returns true if the current principal is authorized to invoke given operation on specified work item.
     */
    public boolean isAuthorized(
            @NotNull CaseWorkItemType workItem,
            @NotNull RequestedOperation operation,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        MidPointPrincipal principal;
        try {
            principal = securityContextManager.getPrincipal();
        } catch (SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get principal", e);
            return false;
        }
        if (principal.getOid() == null) {
            return false;
        }
        try {
            ObjectTypeUtil.checkIn(workItem, CaseType.class);
            return securityEnforcer.isAuthorized(
                    operation.action.getUrl(),
                    null,
                    ValueAuthorizationParameters.of(workItem),
                    null,
                    task, result);
        } catch (CommonException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    /**
     * Returns true if the current principal is authorized to claim given work item.
     */
    public boolean isAuthorizedToClaim(CaseWorkItemType workItem) {
        MidPointPrincipal principal;
        try {
            principal = securityContextManager.getPrincipal();
        } catch (SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get principal", e);
            return false;
        }
        return principal.getOid() != null && isAmongCandidates(principal, workItem);
    }

    private boolean isAmongCandidates(@NotNull MidPointPrincipal principal, @NotNull CaseWorkItemType workItem) {
        var identity = principal.getOid();
        var directMembership = ObjectTypeUtil.getOidsFromRefs(principal.getFocus().getRoleMembershipRef());
        var delegatedIdentitiesAndMembership = principal.getDelegatedMembershipFor(OtherPrivilegesLimitations.Type.CASES);
        for (ObjectReferenceType candidateRef : workItem.getCandidateRef()) {
            var candidateOid = Objects.requireNonNull(candidateRef.getOid());
            if (candidateOid.equals(identity)
                    || directMembership.contains(candidateOid)
                    || delegatedIdentitiesAndMembership.contains(candidateOid)) {
                return true;
            }
        }
        return false;
    }
}
