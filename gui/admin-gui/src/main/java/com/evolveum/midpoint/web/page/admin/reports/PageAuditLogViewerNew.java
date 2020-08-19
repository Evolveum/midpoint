/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanelNew;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/auditLogViewerNew", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL, label = "PageAuditLogViewer.auth.auditLogViewer.label", description = "PageAuditLogViewer.auth.auditLogViewer.description") })
public class PageAuditLogViewerNew extends PageBase {

    private static final long serialVersionUID = 1L;
    private static final String ID_AUDIT_LOG_VIEWER_PANEL = "auditLogViewerPanel";

    public PageAuditLogViewerNew(){
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AuditLogViewerPanelNew auditLogViewerPanel = new AuditLogViewerPanelNew(ID_AUDIT_LOG_VIEWER_PANEL);
        auditLogViewerPanel.setOutputMarkupId(true);
        add(auditLogViewerPanel);
    }

}
