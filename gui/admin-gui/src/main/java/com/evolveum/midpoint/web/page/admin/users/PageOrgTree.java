/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.orgs.AbstractOrgTabPanel;
import com.evolveum.midpoint.web.page.admin.users.component.TreeTablePanel;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/tree", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL,
                label = "PageAdminUsers.auth.orgAll.label",
                description = "PageAdminUsers.auth.orgAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_TREE_URL,
                label = "PageOrgTree.auth.orgTree.label",
                description = "PageOrgTree.auth.orgTree.description")})
public class PageOrgTree extends PageAdmin {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgTree.class);

    /**
     * todo what is this for? [lazyman]
     */
    public static final String PARAM_ORG_RETURN = "org";

    private static final String DOT_CLASS = PageOrgTree.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_UNIT = DOT_CLASS + "loadOrgUnit";

    private static final String ID_TABS = "tabs";
    private static final String ID_ORG_PANEL = "orgPanel";


    public PageOrgTree() {
        initLayout();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private void initLayout() {
        AbstractOrgTabPanel tabbedPanel = new AbstractOrgTabPanel(ID_ORG_PANEL, this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Panel createTreePanel(String id, Model<String> model, PageBase pageBase) {
                return new TreeTablePanel(id, model, PageOrgTree.this);
            }
        };

        tabbedPanel.setOutputMarkupId(true);
        add(tabbedPanel);
    }

    public AbstractOrgTabPanel getTabPanel(){
        return (AbstractOrgTabPanel)get(ID_ORG_PANEL);
    }

}
