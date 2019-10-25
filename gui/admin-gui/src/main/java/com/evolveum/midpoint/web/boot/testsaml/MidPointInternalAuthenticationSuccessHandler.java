/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.MidPointAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class MidPointInternalAuthenticationSuccessHandler extends MidPointAuthenticationSuccessHandler {

    @Autowired
    private ModelInteractionService modelInteractionService;

    @Autowired
    private TaskManager taskManager;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        String requestUrl = request.getRequestURL().toString();
        if (requestUrl.contains("internal/spring_security_login")) {
            String target;
            if (WebModelServiceUtils.isPostAuthenticationEnabled(taskManager, modelInteractionService)) {
                target = requestUrl.replace("internal/spring_security_login", "self/postAuthentication");
            } else {
                target = requestUrl.replace("internal/spring_security_login", "admin/dashboard/info");
            }
            getRedirectStrategy().sendRedirect(request, response, target);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
