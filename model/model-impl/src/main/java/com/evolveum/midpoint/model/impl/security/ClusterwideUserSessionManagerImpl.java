/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.model.api.authentication.ClusterwideUserSessionManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.util.ClusterServiceConsts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.ClusterExecutionOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;

/**
 * Takes care for clusterwide user session management.
 */
@Component
public class ClusterwideUserSessionManagerImpl implements ClusterwideUserSessionManager {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterwideUserSessionManagerImpl.class);

    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired private GuiProfiledPrincipalManager guiProfiledPrincipalManager;

    @Override
    public void terminateSessions(TerminateSessionEvent terminateSessionEvent, Task task, OperationResult result) {

        guiProfiledPrincipalManager.terminateLocalSessions(terminateSessionEvent);

        // We try to invoke this call also on nodes that are in transition. It is quite important
        // that terminate session is executed on as wide scale as realistically possible.
        clusterExecutionHelper.execute((client, node, result1) -> {
            client.path(ClusterServiceConsts.EVENT_TERMINATE_SESSION);
            var response = client.post(terminateSessionEvent.toEventType());
            LOGGER.info("Remote-node user session termination finished on {} with status {}, {}", node.getNodeIdentifier(),
                    response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase());
            response.close();
        }, new ClusterExecutionOptions().tryNodesInTransition(), "session termination", result);
    }

    @Override
    @NotNull
    public List<UserSessionManagementType> getLoggedInPrincipals(Task task, OperationResult result) {

        List<UserSessionManagementType> loggedUsers = guiProfiledPrincipalManager.getLocalLoggedInPrincipals();

        Map<String, UserSessionManagementType> usersMap = new HashMap<>();
        //fix for mid-6328
        loggedUsers.forEach(loggedUser -> {
            UserSessionManagementType addedUser = usersMap.get(loggedUser.getFocus().getOid());
            if (addedUser != null) {
                addedUser.setActiveSessions(addedUser.getActiveSessions() + loggedUser.getActiveSessions());
                addedUser.getNode().addAll(loggedUser.getNode());
            } else {
                usersMap.put(loggedUser.getFocus().getOid(), loggedUser);
            }
        });

        // We try to invoke this call also on nodes that are in transition. We want to get
        // information as complete as realistically possible.
        clusterExecutionHelper.execute((client, node, result1) -> {
            client.path(ClusterServiceConsts.EVENT_LIST_USER_SESSION);
            var response = client.get();
            LOGGER.debug("Remote-node retrieval of user sessions finished on {} with status {}, {}", node.getNodeIdentifier(),
                    response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase());

            if (response.hasEntity()) {
                UserSessionManagementListType remoteSessionsWrapper = response.readEntity(UserSessionManagementListType.class);
                List<UserSessionManagementType> remoteSessions = remoteSessionsWrapper.getSession();
                for (UserSessionManagementType remoteSession : MiscUtil.emptyIfNull(remoteSessions)) {
                    UserSessionManagementType existingUser = usersMap.get(remoteSession.getFocus().getOid());
                    if (existingUser != null) {
                        existingUser.setActiveSessions(existingUser.getActiveSessions() + remoteSession.getActiveSessions());
                        existingUser.getNode().addAll(remoteSession.getNode());
                    } else {
                        usersMap.put(remoteSession.getFocus().getOid(), remoteSession);
                    }
                }
            }
            response.close();
        }, new ClusterExecutionOptions().tryNodesInTransition(), " list principals from remote nodes ", result);

        return new ArrayList<>(usersMap.values());
    }
}
