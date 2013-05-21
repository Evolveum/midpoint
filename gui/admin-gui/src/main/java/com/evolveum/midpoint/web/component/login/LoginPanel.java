/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.help.PageAbout;

import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * @author lazyman
 */
public class LoginPanel extends Panel {

    public LoginPanel(String id) {
        super(id);
        
        add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return true;
            }
        });
        
        add(new Label("username", new LoadableModel<String>() {
            @Override
            protected String load() {
                return getShortUserName();
            }
        }));


        ExternalLink logoutLink = new ExternalLink("logoutLink",
                new Model<String>(RequestCycle.get().getRequest().getContextPath() + "/j_spring_security_logout"), new Model<String>("Logout"));
        add(logoutLink);
        
        AjaxLink<String> helpLink = new AjaxLink<String>("help") {

            @Override
            public void onClick(AjaxRequestTarget target) {
            	setResponsePage(PageAbout.class);
            }
        };
        add(helpLink);
    }

    private String getShortUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return "unknown";
        }

        if (principal instanceof MidPointPrincipal) {
            return WebMiscUtil.getOrigStringFromPoly(((MidPointPrincipal) principal).getName());
        }

        return principal.toString();
    }

    public boolean getIsUserLoggedIn() {
        return isUserInRole("ROLE_USER") || isUserInRole("ROLE_ADMIN");
    }

    public boolean getIsAdminLoggedIn() {
        return isUserInRole("ROLE_ADMIN");
    }

    private boolean isUserInRole(final String role) {
        final Collection<GrantedAuthority> grantedAuthorities = (Collection<GrantedAuthority>) SecurityContextHolder
                .getContext().getAuthentication().getAuthorities();
        for (GrantedAuthority grantedAuthority : grantedAuthorities) {
            if (role.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
