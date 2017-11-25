/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users;

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
import com.evolveum.midpoint.web.page.admin.users.component.OrgMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminUsers.AUTH_ORG_ALL, label = PageAdminUsers.AUTH_ORG_ALL_LABEL, description = PageAdminUsers.AUTH_ORG_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL, label = "PageOrgUnit.auth.orgUnit.label", description = "PageOrgUnit.auth.orgUnit.description") })
public class PageOrgUnit extends PageAdminAbstractRole<OrgType> implements ProgressReportingAwarePage {

	private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);

	public PageOrgUnit() {
		initialize(null);
	}

	public PageOrgUnit(final PrismObject<OrgType> unitToEdit) {
		initialize(unitToEdit);
	}

	public PageOrgUnit(final PrismObject<OrgType> unitToEdit, boolean isReadonly) {
		initialize(unitToEdit, isReadonly);
	}

	public PageOrgUnit(PageParameters parameters) {
		getPageParameters().overwriteWith(parameters);
		initialize(null);
	}

	@Override
	protected OrgType createNewObject() {
		return new OrgType();
	}

	@Override
	public Class<OrgType> getCompileTimeClass() {
		return OrgType.class;
	}

	@Override
	protected Class getRestartResponsePage() {
		return PageOrgTree.class;
	}

	@Override
	protected FocusSummaryPanel<OrgType> createSummaryPanel() {
    	return new OrgSummaryPanel(ID_SUMMARY_PANEL, getObjectModel(), this);
    }

	@Override
	protected AbstractObjectMainPanel<OrgType> createMainPanel(String id) {
		return new AbstractRoleMainPanel<OrgType>(id, getObjectModel(),
				getProjectionModel(), this) {

			@Override
			public AbstractRoleMemberPanel<OrgType> createMemberPanel(String panelId) {
				return new OrgMemberPanel(panelId, Model.of(getObject().asObjectable()));
			}
		};
	}

}
