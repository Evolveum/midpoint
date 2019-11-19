/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot.auth;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.boot.auth.module.configuration.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.web.security.OldMidPointAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class MidPointAuthenticationSuccessHandler extends OldMidPointAuthenticationSuccessHandler {

    @Autowired
    private ModelInteractionService modelInteractionService;

    @Autowired
    private TaskManager taskManager;

    private String prefix = "";

    public MidPointAuthenticationSuccessHandler setPrefix(String prefix) {
        this.prefix = "/" + stripSlashes(prefix) + "/";
        return this;
    }

    public MidPointAuthenticationSuccessHandler() {
        setRequestCache(new HttpSessionRequestCache());
    }

    private RequestCache requestCache;

    @Override
    public void setRequestCache(RequestCache requestCache) {
        super.setRequestCache(requestCache);
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (WebModelServiceUtils.isPostAuthenticationEnabled(taskManager, modelInteractionService)
        || savedRequest == null) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        if (savedRequest.getRedirectUrl().contains(ModuleWebSecurityConfiguration.DEFAULT_PREFIX + "/")) {
            String target = savedRequest.getRedirectUrl().substring(0, savedRequest.getRedirectUrl().indexOf(ModuleWebSecurityConfiguration.DEFAULT_PREFIX + "/")) + "/self/dashboard";
            getRedirectStrategy().sendRedirect(request, response, target);
            return;
        }
//            if (requestUrl.contains("spring_security_login")) {
//                target = requestUrl.replace(prefix + "spring_security_login", "self/postAuthentication");
//            } else {
//                target = requestUrl.replace(prefix + "spring_security_login", "self/dashboard");
//            }
//            getRedirectStrategy().sendRedirect(request, response, target);
//            return;
//        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
