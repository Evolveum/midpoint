package com.evolveum.midpoint.web.page.admin.reports;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;


@PageDescriptor(url = "/admin/auditLogDetails", action = {
		@AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
				label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
				description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL,
		label = "PageAuditLogViewer.auth.auditLogViewer.label",
		description = "PageAuditLogViewer.auth.auditLogViewer.description")})
public class PageAuditLogDetails extends PageBase{

	private static final long serialVersionUID = 1L;
	
	private IModel<AuditEventRecordType> recordModel;
	
	public PageAuditLogDetails(final AuditEventRecordType recordType) {
		
		recordModel = new LoadableModel<AuditEventRecordType>(false) {
			
			@Override
			protected AuditEventRecordType load() {
				return recordType;
			}
		};
	
		initLayout();
	}
	
	private void initLayout(){
		//TODO
	}

}
