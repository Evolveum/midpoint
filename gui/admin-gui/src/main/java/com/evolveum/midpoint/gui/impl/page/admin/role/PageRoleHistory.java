/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by honchar
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleHistory")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_HISTORY_URL,
                label = "PageRoleHistory.auth.roleHistory.label",
                description = "PageRoleHistory.auth.roleHistory.description")})
public class PageRoleHistory extends PageRole {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageRoleHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageRoleHistory.class);
    private String date = "";

    public PageRoleHistory(final PrismObject<RoleType> role, String date) {
        super(role);
        this.date = date;
    }

    @Override
    protected FocusDetailsModels<RoleType> createObjectDetailsModels(PrismObject<RoleType> object) {
        return new FocusDetailsModels<RoleType>(createPrismObjectModel(object), this) {
            @Override
            protected boolean isReadonly() {
                return true;
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                String name = null;
                if (getModelObjectType() != null) {
                    name = WebComponentUtil.getName(getModelObjectType());
                }
                return createStringResource("PageUserHistory.title", name, date).getObject();
            }
        };
    }

}

