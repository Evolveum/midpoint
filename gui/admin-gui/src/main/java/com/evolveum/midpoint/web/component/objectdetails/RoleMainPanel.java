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

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.roles.RoleGovernanceMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.RoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class RoleMainPanel extends AbstractRoleMainPanel<RoleType> {
	private static final long serialVersionUID = 1L;

	public RoleMainPanel(String id, LoadableModel<ObjectWrapper<RoleType>> objectModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel,
			PageAdminFocus<RoleType> parentPage) {
		super(id, objectModel, projectionModel, parentPage);
	}

	@Override
	public AbstractRoleMemberPanel<RoleType> createMemberPanel(String panelId) {
		OperationResult result = new OperationResult("Create relation list");
		return new RoleMemberPanel(panelId, new Model<>(getObject().asObjectable()), WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, result, getDetailsPage()));
	}

	@Override
	public AbstractRoleMemberPanel<RoleType> createGovernancePanel(String panelId) {
//		List<RelationTypes> relationsList = new ArrayList<>();
//		relationsList.add(RelationTypes.APPROVER);
//		relationsList.add(RelationTypes.OWNER);
//		relationsList.add(RelationTypes.MANAGER);

		OperationResult result = new OperationResult("Create relation list");
		return new RoleGovernanceMemberPanel(panelId, new Model<>(getObject().asObjectable()), WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, result, getDetailsPage()));
	}
}
