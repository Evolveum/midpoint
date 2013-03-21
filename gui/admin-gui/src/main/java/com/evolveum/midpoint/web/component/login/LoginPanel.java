/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
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
