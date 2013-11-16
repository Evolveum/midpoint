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

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author lazyman
 */
public class UserMenuPanel extends BaseSimplePanel {

    private static final String ID_USERNAME = "username";
    private static final String ID_LOGOUT_LINK = "logoutLink";
    private static final String ID_EDIT_PROFILE = "editProfile";
    private static final String ID_RESET_PASSWORDS = "resetPasswords";

    public UserMenuPanel(String id) {
        super(id);

        setRenderBodyOnly(true);
    }

    @Override
    protected void initLayout() {
        Label username = new Label(ID_USERNAME, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getShortUserName();
            }
        });
        username.setRenderBodyOnly(true);
        add(username);

        ExternalLink logoutLink = new ExternalLink(ID_LOGOUT_LINK,
                new Model<String>(RequestCycle.get().getRequest().getContextPath() + "/j_spring_security_logout"),
                createStringResource("UserMenuPanel.logout"));
        add(logoutLink);

        LabeledBookmarkableLink editProfile = new LabeledBookmarkableLink(ID_EDIT_PROFILE,
                new MenuItem(createStringResource("UserMenuPanel.editProfile"), PageDashboard.class));
        add(editProfile);

        LabeledBookmarkableLink resetPasswords = new LabeledBookmarkableLink(ID_RESET_PASSWORDS,
                new MenuItem(createStringResource("UserMenuPanel.resetPasswords"), PageDashboard.class));
        add(resetPasswords);
    }

    private String getShortUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return "Unknown";
        }

        if (principal instanceof MidPointPrincipal) {
            MidPointPrincipal princ = (MidPointPrincipal) principal;

            return WebMiscUtil.getOrigStringFromPoly(princ.getName());
        }

        return principal.toString();
    }
}
