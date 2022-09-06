/*
 * Copyright (c) 2010-2018 Evolveum et al. and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.cases;

import java.util.ArrayList;
import java.util.List;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;

/**
 * @author bpowers
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/workItems")
        },
        encoder = PageParametersEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPROVALS_ALL_URL,
                        label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                        description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ALL_WORK_ITEMS_URL,
                        label = "PageWorkItems.auth.WorkItems.label",
                        description = "PageWorkItems.auth.WorkItems.description")
        })
public class PageCaseWorkItems extends PageAdminCaseWorkItems {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItems.class);

    private static final String DOT_CLASS = PageCaseWorkItems.class.getName() + ".";
    private static final String PARAMETER_CASE_ID = "caseId";
    private static final String PARAMETER_CASE_WORK_ITEM_ID = "caseWorkItemId";

    private static final String ID_CASE_WORK_ITEMS_TABLE = "caseWorkItemsTable";

    public PageCaseWorkItems() {
        this(null);
    }

    public PageCaseWorkItems(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CaseWorkItemsPanel workItemsPanel = new CaseWorkItemsPanel(ID_CASE_WORK_ITEMS_TABLE, CaseWorkItemsPanel.View.FULL_LIST) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<InlineMenuItem> createRowActions() {
                List<InlineMenuItem> menu = super.createRowActions();

                List<InlineMenuItem> additionalMenu = PageCaseWorkItems.this.createRowActions();
                if (additionalMenu != null) {
                    menu.addAll(additionalMenu);
                }
                return menu;
            }

            @Override
            protected ObjectFilter getCaseWorkItemsFilter() {
                return PageCaseWorkItems.this.getCaseWorkItemsFilter();
            }
        };
        workItemsPanel.setOutputMarkupId(true);
        add(workItemsPanel);
    }

    protected ObjectFilter getCaseWorkItemsFilter(){
        return null;
    }

    protected List<InlineMenuItem> createRowActions(){
        return new ArrayList<>();
    }

    protected CaseWorkItemsPanel getCaseWorkItemsTable() {
        return (CaseWorkItemsPanel) get(createComponentPath(ID_CASE_WORK_ITEMS_TABLE));
    }

}
