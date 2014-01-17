/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.common.security.AuthorizationEvaluator;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MidPointGuiAuthorizationEvaluator extends AuthorizationEvaluator {

    @Override
    public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes)
            throws AccessDeniedException, InsufficientAuthenticationException {

        if (!(object instanceof FilterInvocation)) {
            return;
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        Collection<ConfigAttribute> guiConfigAttr = new ArrayList<>();

        for (PageUrlMapping urlMapping : PageUrlMapping.values()) {
            addSecurityConfig(filterInvocation, guiConfigAttr, urlMapping.getUrl(), urlMapping.getAction());
        }

        Map<String, String[]> actions = DescriptorLoader.getActions();
        for (Map.Entry<String, String[]> entry : actions.entrySet()) {
            addSecurityConfig(filterInvocation, guiConfigAttr, entry.getKey(), entry.getValue());
        }

        if (configAttributes == null && guiConfigAttr.isEmpty()) {
            return;
        }

        super.decide(authentication, object, guiConfigAttr.isEmpty() ? configAttributes : guiConfigAttr);
    }

    private void addSecurityConfig(FilterInvocation filterInvocation, Collection<ConfigAttribute> guiConfigAttr,
                      String url, String[] actions) {

        AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
        if (!matcher.matches(filterInvocation.getRequest()) || actions == null) {
            return;
        }

        for (String action : actions) {
            if (StringUtils.isBlank(action)) {
                continue;
            }

            //all users has permission to access these resources
            if (action.equals(AuthorizationConstants.AUTZ_UI_PERMIT_ALL_URL)) {
                return;
            }

            guiConfigAttr.add(new SecurityConfig(action));
        }
    }
}
