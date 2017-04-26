/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.roles.RoleGovernanceRelationsPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.roles.RoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.RolePolicyPanel;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class RoleMainPanel extends AbstractRoleMainPanel<RoleType> {
	private static final long serialVersionUID = 1L;
	
	public RoleMainPanel(String id, LoadableModel<ObjectWrapper<RoleType>> objectModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, 
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel, 
			LoadableModel<List<AssignmentEditorDto>> inducementsModel, PageAdminFocus<RoleType> parentPage) {
		super(id, objectModel, assignmentsModel, projectionModel, inducementsModel, parentPage);
	}

	@Override
	protected List<ITab> createTabs(final PageAdminObjectDetails<RoleType> parentPage) {
		List<ITab> tabs = super.createTabs(parentPage);

		FocusTabVisibleBehavior authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_GOVERNANCE_URL);

		tabs.add(new PanelTab(parentPage.createStringResource("pageRole.governance"), authorization) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return createGovernancePanel(panelId);
			}

			@Override
			public boolean isVisible() {
				return getObjectWrapper().getStatus() != ContainerStatus.ADDING;
			}
		});

		authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_POLICY_CONSTRAINTS_URL);

		tabs.add(new PanelTab(parentPage.createStringResource("AbstractRoleType.policyConstraints"), authorization) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new RolePolicyPanel(panelId, getObject());
			}
		});

		authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_MEMBERS_URL);

//		tabs.add(new PanelTab(parentPage.createStringResource("pageRole.members"), authorization) {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public WebMarkupContainer createPanel(String panelId) {
//				return new RoleMemberPanel(panelId, new Model<RoleType>(getObject().asObjectable()), getDetailsPage());
//			}
//
//			@Override
//			public boolean isVisible() {
//				return getObjectWrapper().getStatus() != ContainerStatus.ADDING;
//			}
//		});

		return tabs;
	}
	
	@Override
	public AbstractRoleMemberPanel<RoleType> createMemberPanel(String panelId) {
		return new RoleMemberPanel(panelId, new Model<RoleType>(getObject().asObjectable()), getDetailsPage());
	}

	public AbstractRoleMemberPanel<RoleType> createGovernancePanel(String panelId) {
		List<RelationTypes> relationsList = new ArrayList<>();
		relationsList.add(RelationTypes.APPROVER);
		relationsList.add(RelationTypes.OWNER);
		relationsList.add(RelationTypes.MANAGER);

		return new RoleGovernanceRelationsPanel(panelId, new Model<RoleType>(getObject().asObjectable()),
				relationsList, getDetailsPage());
	}

	
	
}
