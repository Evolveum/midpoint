/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.gui.api.GuiConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

public class AuditedLogoutHandler extends SimpleUrlLogoutSuccessHandler {

    private static final Trace LOGGER = TraceManager.getTrace(AuditedLogoutHandler.class);

    @Autowired private TaskManager taskManager;
    @Autowired private AuditService auditService;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private PrismContext prismContext;

    boolean useDefaultUrl = false;

    private boolean useDefaultUrl() {
        return useDefaultUrl;
    }

    @Override
    public void setDefaultTargetUrl(String defaultTargetUrl) {
        super.setDefaultTargetUrl(defaultTargetUrl);
        this.useDefaultUrl = true;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        String targetUrl;
        if (useDefaultUrl()) {
            targetUrl = getDefaultTargetUrl();
        } else {
            targetUrl = GuiConstants.DEFAULT_PATH_AFTER_LOGOUT;
            if (authentication instanceof MidpointAuthentication) {
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
                if (mpAuthentication.getAuthenticationChannel() != null) {
                    targetUrl = mpAuthentication.getAuthenticationChannel().getPathAfterLogout();
                }
            }
        }

        if (response.isCommitted()) {
            LOGGER.debug("Response has already been committed. Unable to redirect to " + targetUrl);
        } else {
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }

        auditEvent(request, authentication);
    }

    private void auditEvent(HttpServletRequest request, Authentication authentication) {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser(authentication);
        PrismObject<? extends FocusType> user = principal != null ? principal.getFocus().asPrismObject() : null;

        String channel = SchemaConstants.CHANNEL_USER_URI;
        String sessionId = request.getRequestedSessionId();
        if (authentication instanceof MidpointAuthentication
                && ((MidpointAuthentication) authentication).getAuthenticationChannel() != null) {
            channel = ((MidpointAuthentication) authentication).getAuthenticationChannel().getChannelId();
            if (((MidpointAuthentication) authentication).getSessionId() != null) {
                sessionId = ((MidpointAuthentication) authentication).getSessionId();
            }
        }
        SystemConfigurationType system = null;
        try {
            system = systemObjectCache.getSystemConfiguration(new OperationResult("LOAD SYSTEM CONFIGURATION")).asObjectable();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get system configuration from cache", e);
        }
        if (!SecurityUtil.isAuditedLoginAndLogout(system, channel)) {
            return;
        }

        Task task = taskManager.createTaskInstance();
        task.setOwner(user);
        task.setChannel(channel);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
        record.setInitiator(user, prismContext);
        record.setParameter(WebComponentUtil.getName(user, false));

        record.setChannel(channel);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.SUCCESS);

        // probably not needed, as audit service would take care of it; but it doesn't hurt so let's keep it here
        record.setHostIdentifier(request.getLocalName());
        record.setRemoteHostAddress(request.getLocalAddr());
        record.setNodeIdentifier(taskManager.getNodeId());
        record.setSessionIdentifier(sessionId);

        auditService.audit(record, task);
    }
}
