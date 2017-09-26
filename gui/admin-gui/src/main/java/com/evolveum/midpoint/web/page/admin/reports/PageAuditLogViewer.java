package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/auditLogViewer", action = {
		@AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL, label = "PageAuditLogViewer.auth.auditLogViewer.label", description = "PageAuditLogViewer.auth.auditLogViewer.description") })
public class PageAuditLogViewer extends PageBase {

	private static final long serialVersionUID = 1L;
    private static final String ID_PANEL = "auditLogViewerPanel";
	public PageAuditLogViewer() {
        initLayout();
	}

    private void initLayout(){
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_PANEL, null);
        panel.setOutputMarkupId(true);
        add(panel);
    }


}
