/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author lazyman
 */
public class AuditedLogoutHandler extends SimpleUrlLogoutSuccessHandler {

    @Autowired
    private TaskManager taskManager;
    @Autowired
    private AuditService auditService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        super.onLogoutSuccess(request, response, authentication);

        auditEvent(request, authentication);
    }

    private void auditEvent(HttpServletRequest request, Authentication authentication) {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser(authentication);
        PrismObject<UserType> user = principal != null ? principal.getUser().asPrismObject() : null;

        Task task = taskManager.createTaskInstance();
        task.setOwner(user);
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
        record.setInitiator(user);
        record.setParameter(WebComponentUtil.getName(user, false));

        record.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.SUCCESS);

        // probably not needed, as audit service would take care of it; but it doesn't hurt so let's keep it here
        record.setHostIdentifier(request.getLocalName());
        record.setRemoteHostAddress(request.getLocalAddr());
        record.setNodeIdentifier(taskManager.getNodeId());
        record.setSessionIdentifier(request.getRequestedSessionId());

        auditService.audit(record, task);
    }
}
