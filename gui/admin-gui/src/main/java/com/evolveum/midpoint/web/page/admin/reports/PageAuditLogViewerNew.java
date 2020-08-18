package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanelNew;

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
