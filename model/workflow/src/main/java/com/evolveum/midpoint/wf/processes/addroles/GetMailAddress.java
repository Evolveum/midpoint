/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes.addroles;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

/**
 * @author mederly
 */
public class GetMailAddress implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(GetMailAddress.class);

    public void execute(DelegateExecution execution) {

//        LOGGER.info("Variables:");
//        for (String v : execution.getVariableNames()) {
//            LOGGER.info(" - " + v);
//        }
//        LOGGER.info("Local variables:");
//        for (String v : execution.getVariableNamesLocal()) {
//            LOGGER.info(" - " + v);
//        }

        RoleType role = (RoleType) execution.getVariable(AddRoleAssignmentWrapper.ROLE);
        String mail = getMail(execution, role);
        LOGGER.info("Approver's mail for role " + role + " is " + mail);
        execution.setVariableLocal(AddRoleAssignmentWrapper.APPROVER_MAIL_ADDRESS, mail);
    }

    private String getMail(DelegateExecution execution, RoleType role) {

        OperationResult result = new OperationResult("dummy");

        if (role == null) {
            LOGGER.warn("No 'role' local variable.");
            return null;
        }
        if (role.getApproverRef().isEmpty()) {
            return null;
        }

        ObjectReferenceType approverRef = role.getApproverRef().get(0);         // TODO more approvers

        RepositoryService repositoryService = getRepositoryService();
        PrismObject<UserType> approver = null;
        try {
            approver = repositoryService.getObject(UserType.class, approverRef.getOid(), result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Approver " + approverRef.getOid() + " for role " + role + " couldn't be found", e);
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Approver " + approverRef.getOid() + " for role " + role + " couldn't be found due to schema exception", e);
            return null;
        }

        return approver.asObjectable().getEmailAddress();
    }

    public RepositoryService getRepositoryService() {
        RepositoryService repositoryService = SpringApplicationContextHolder
                .getApplicationContext().
                        getBean("repositoryService", RepositoryService.class);

        if (repositoryService == null) {
            throw new SystemException("repositoryService bean could not be found.");
        }
        else
        {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("repositoryService bean has been found: " + repositoryService);
            }
            return repositoryService;
        }
    }

}
