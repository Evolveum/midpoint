/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/auditLogViewer")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL, label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL, label = "PageAuditLogViewer.auth.auditLogViewer.label", description = "PageAuditLogViewer.auth.auditLogViewer.description")
        })
@CollectionInstance(identifier = "allAudits", applicableForType = AuditEventRecordType.class,
        display = @PanelDisplay(label = "PageAuditLogViewer.menuName", singularLabel = "ObjectType.AuditEventRecordType"))
public class PageAuditLogViewer extends PageBase {

    private static final long serialVersionUID = 1L;
    private static final String ID_AUDIT_LOG_VIEWER_PANEL = "auditLogViewerPanel";

    public PageAuditLogViewer() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AuditLogViewerPanel auditLogViewerPanel = new AuditLogViewerPanel(ID_AUDIT_LOG_VIEWER_PANEL);
        auditLogViewerPanel.setOutputMarkupId(true);
        add(auditLogViewerPanel);
    }

}
