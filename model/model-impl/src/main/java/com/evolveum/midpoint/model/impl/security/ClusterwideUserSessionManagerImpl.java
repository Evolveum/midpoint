/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.model.api.authentication.ClusterwideUserSessionManager;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.impl.ClusterRestService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.ClusterExecutionOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        clusterExecutionHelper.execute((client, result1) -> {
            client.path(ClusterRestService.EVENT_TERMINATE_SESSION);
            Response response = client.post(terminateSessionEvent.toEventType());
            LOGGER.info("Remote-node user session termination finished with status {}, {}",
                    response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase());
            response.close();
        }, new ClusterExecutionOptions().tryNodesInTransition(), "session termination", result);
    }

    @Override
    @NotNull
    public List<UserSessionManagementType> getLoggedInPrincipals(Task task, OperationResult result) {

        List<UserSessionManagementType> loggedUsers = guiProfiledPrincipalManager.getLocalLoggedInPrincipals();

        Map<String, UserSessionManagementType> usersMap = loggedUsers.stream()
                .collect(Collectors.toMap(key -> key.getFocus().getOid(), value -> value));

        // We try to invoke this call also on nodes that are in transition. We want to get
        // information as complete as realistically possible.
        clusterExecutionHelper.execute((client, result1) -> {
            client.path(ClusterRestService.EVENT_LIST_USER_SESSION);
            Response response = client.get();
            LOGGER.info("Remote-node retrieval of user sessions finished with status {}, {}",
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
