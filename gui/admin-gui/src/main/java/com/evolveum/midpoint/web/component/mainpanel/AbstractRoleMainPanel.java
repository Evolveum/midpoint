/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.web.component.mainpanel;

import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.roles.RolePolicyPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author semancik
 *
 */
public class AbstractRoleMainPanel<R extends AbstractRoleType> extends FocusMainPanel<R> {
	
	private LoadableModel<List<AssignmentEditorDto>> inducementsModel;

	public AbstractRoleMainPanel(String id, LoadableModel<ObjectWrapper<R>> objectModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, LoadableModel<List<FocusProjectionDto>> projectionModel, 
			LoadableModel<List<AssignmentEditorDto>> inducementsModel, PageAdminFocus<R> parentPage) {
		super(id, objectModel, assignmentsModel, projectionModel, parentPage);
		this.inducementsModel = inducementsModel;
	}

	@Override
	protected List createTabs(final PageAdminObjectDetails<R> parentPage) {
		List tabs = super.createTabs(parentPage);
		
		tabs.add(new AbstractTab(parentPage.createStringResource("FocusType.inducement")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new AssignmentTablePanel(panelId, parentPage.createStringResource("FocusType.inducement"), inducementsModel) {

					@Override
					public List<AssignmentType> getAssignmentTypeList() {
						return getObject().asObjectable().getInducement();
					}

					@Override
					public String getExcludeOid() {
						return getObject().getOid();
					}
				};
			}
		});
		
		return tabs;
	}

	
	
}
