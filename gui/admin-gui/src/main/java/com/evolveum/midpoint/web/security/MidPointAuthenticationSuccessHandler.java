/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelfRegistrationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Viliam Repan (lazyman)
 */
public class MidPointAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private String defaultTargetUrl;
    
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private TaskManager taskManager;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
    		throws ServletException, IOException {
    	
    	if (WebModelServiceUtils.isPostAuthenticationEnabled(taskManager, modelInteractionService)) {
    		String requestUrl = request.getRequestURL().toString();
			 if (requestUrl.contains("spring_security_login")) {
				 String target = requestUrl.replace("spring_security_login", "self/postAuthentication");
				 getRedirectStrategy().sendRedirect(request, response, target);
				 return;
			 }
    	}
    	super.onAuthenticationSuccess(request, response, authentication);
    }

	@Override
	protected String getTargetUrlParameter() {
		
    	
    	return defaultTargetUrl;
	}


    @Override
    public void setDefaultTargetUrl(String defaultTargetUrl) {
        this.defaultTargetUrl = defaultTargetUrl;
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(defaultTargetUrl)) {
            return super.determineTargetUrl(request, response);
        }

        return defaultTargetUrl;
    }
}
