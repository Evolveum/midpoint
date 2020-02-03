/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidpointAccessDeniedHandler<SecurityHelper> implements AccessDeniedHandler {

    private AccessDeniedHandler defaultHandler = new AccessDeniedHandlerImpl();

    @Autowired
    private TaskManager taskManager;
    @Autowired
    private AuditService auditService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        boolean ended = handleInternal(request, response, accessDeniedException);
        if (ended) {
            return;
        }

        defaultHandler.handle(request, response, accessDeniedException);
    }

    protected boolean handleInternal(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            return true;
        }

        if (accessDeniedException instanceof CsrfException) {
            // handle invalid csrf token exception gracefully when user tries to log in/out with expired exception
            // handle session timeout for ajax cases -> redirect to base context (login)
            if (WicketRedirectStrategy.isWicketAjaxRequest(request)) {
                WicketRedirectStrategy redirect = new WicketRedirectStrategy();
                redirect.sendRedirect(request, response, request.getContextPath());
            } else {
                response.sendRedirect(request.getContextPath());
            }

            return true;
        }
        return false;
    }
}
