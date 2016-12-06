package com.evolveum.midpoint.web.page.admin.services;

import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractRoleMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ServiceSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

@PageDescriptor(url = "/admin/service", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminServices.AUTH_SERVICES_ALL, label = PageAdminServices.AUTH_SERVICES_ALL_LABEL, description = PageAdminServices.AUTH_SERVICES_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICE_URL, label = "PageService.auth.role.label", description = "PageService.auth.role.description") })
public class PageService extends PageAdminAbstractRole<ServiceType> implements ProgressReportingAwarePage{

	private static final long serialVersionUID = 1L;
	
	public PageService() {
		initialize(null);
	}

	public PageService(final PrismObject<ServiceType> unitToEdit) {
		initialize(unitToEdit);
	}

	public PageService(PageParameters parameters) {
		getPageParameters().overwriteWith(parameters);
		initialize(null);
	}
	
	@Override
	protected ServiceType createNewObject() {
		return new ServiceType();
	}	

	@Override
    public Class<ServiceType> getCompileTimeClass() {
		return ServiceType.class;
	}

	@Override
	protected Class getRestartResponsePage() {
		return PageServices.class;
	}

	@Override
	protected FocusSummaryPanel<ServiceType> createSummaryPanel() {
    	return new ServiceSummaryPanel(ID_SUMMARY_PANEL, getObjectModel());
    }

	@Override
	protected AbstractObjectMainPanel<ServiceType> createMainPanel(String id) {
		return new AbstractRoleMainPanel<ServiceType>(id, getObjectModel(), getAssignmentsModel(), getProjectionModel(), getInducementsModel(), this) {
			private static final long serialVersionUID = 1L;

			@Override
			public AbstractRoleMemberPanel<ServiceType> createMemberPanel(String panelId) {
				return new ServiceMemberPanel(panelId, Model.of(getObject().asObjectable()), PageService.this);
			}
		};
	}

}
