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

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.CountableLoadableModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.util.List;

/**
 * @author semancik
 *
 */
public abstract class AbstractRoleMainPanel<R extends AbstractRoleType> extends FocusMainPanel<R> {
	private static final long serialVersionUID = 1L;
	
	private LoadableModel<List<AssignmentEditorDto>> inducementsModel;
	private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMainPanel.class);
    private static final String DOT_CLASS = AbstractRoleMainPanel.class.getName();
    private static final String OPERATION_CAN_SEARCH_ROLE_MEMBERSHIP_ITEM = DOT_CLASS + "canSearchRoleMembershipItem";

	public AbstractRoleMainPanel(String id, LoadableModel<ObjectWrapper<R>> objectModel, 
			CountableLoadableModel<AssignmentEditorDto> assignmentsModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel, 
			LoadableModel<List<AssignmentEditorDto>> inducementsModel, PageAdminFocus<R> parentPage) {
		super(id, objectModel, assignmentsModel, projectionModel, parentPage);
		this.inducementsModel = inducementsModel;
	}

	@Override
	protected List<ITab> createTabs(final PageAdminObjectDetails<R> parentPage) {
		List<ITab> tabs = super.createTabs(parentPage);

		FocusTabVisibleBehavior authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_INDUCEMENTS_URL);
		tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.inducement"), authorization)
		{
			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new AssignmentTablePanel<R>(panelId, parentPage.createStringResource("FocusType.inducement"), inducementsModel) {
					private static final long serialVersionUID = 1L;

					@Override
					public List<AssignmentType> getAssignmentTypeList() {
						return getObject().asObjectable().getInducement();
					}

					@Override
					public String getExcludeOid() {
						return getObject().getOid();
					}

					@Override
					protected boolean ignoreMandatoryAttributes(){
						return true;
					}
				};
			}
			
			@Override
			public String getCount() {
				return Integer.toString(inducementsModel.getObject() == null ? 0 : inducementsModel.getObject().size());
			}
		});

		authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_MEMBERS_URL);
		tabs.add(new PanelTab(parentPage.createStringResource("pageRole.members"), authorization) {

			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return createMemberPanel(panelId);
			}

			@Override
			public boolean isVisible() {
				return super.isVisible() &&
						getObjectWrapper().getStatus() != ContainerStatus.ADDING &&
						isAllowedToReadRoleMembership(getObjectWrapper().getOid(), parentPage);
			}
		});
		
		return tabs;
	}

	private boolean isAllowedToReadRoleMembership(String abstractRoleOid, PageBase parentPage){
		return isAllowedToReadRoleMembershipItemForType(abstractRoleOid, UserType.class, parentPage)
				|| isAllowedToReadRoleMembershipItemForType(abstractRoleOid, RoleType.class, parentPage)
				|| isAllowedToReadRoleMembershipItemForType(abstractRoleOid, OrgType.class, parentPage)
				|| isAllowedToReadRoleMembershipItemForType(abstractRoleOid, ServiceType.class, parentPage);
	}

	private boolean isAllowedToReadRoleMembershipItemForType(String abstractRoleOid, Class type, PageBase parentPage){
		ObjectQuery query = QueryBuilder.queryFor(type, parentPage.getPrismContext())
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(abstractRoleOid).build();
		Task task = parentPage.createSimpleTask(OPERATION_CAN_SEARCH_ROLE_MEMBERSHIP_ITEM);
		OperationResult result = task.getResult();
		boolean isAllowed = false;
		try {
			isAllowed = parentPage.getModelInteractionService()
                    .canSearch(type, null, null, false, query, task, result);
        } catch (Exception ex){
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check if user is allowed to search for roleMembershipRef item", ex);
        }
        return isAllowed;
    }

	public abstract AbstractRoleMemberPanel<R> createMemberPanel(String panelId);
	
}
