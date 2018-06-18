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
package com.evolveum.midpoint.web.page.admin.roles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by honchar.
 */
public class RoleGovernanceRelationsPanel extends RoleMemberPanel<RoleType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RoleGovernanceRelationsPanel.class);
    private static final String DOT_CLASS = RoleGovernanceRelationsPanel.class.getName() + ".";

    public RoleGovernanceRelationsPanel(String id, IModel<RoleType> model, List<RelationTypes> relations) {
        super(id, model, relations);
    }

    @Override
    protected List<InlineMenuItem> createNewMemberInlineMenuItems() {
        List<InlineMenuItem> createMemberMenuItems = new ArrayList<>();

        createMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.createApprover"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.APPROVER.getRelation(), target);
            }
        }));
        createMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.createOwner"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.OWNER.getRelation(), target);
            }
        }));
        createMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createManager"),
                false, new HeaderMenuAction(this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createFocusMemberPerformed(RelationTypes.MANAGER.getRelation(), target);
            }
        }));
        return createMemberMenuItems;
    }

    @Override
    protected List<InlineMenuItem> assignNewMemberInlineMenuItems() {
        List<InlineMenuItem> assignMemberMenuItems = new ArrayList<>();

        assignMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.assignApprovers"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.APPROVER.getRelation(), target);
                    }
                }));
        assignMemberMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.assignOwners"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.OWNER.getRelation(), target);
                    }
                }));
        assignMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addManagers"), false,
                new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addMembers(RelationTypes.MANAGER.getRelation(), target);
                    }
                }));
        return assignMemberMenuItems;
    }

    protected List<InlineMenuItem> createUnassignMemberInlineMenuItems() {
		List<InlineMenuItem> unassignMenuItems = new ArrayList<>();
		unassignMenuItems
				.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.unassignApproversSelected"),
						false, new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						removeMembersPerformed(QueryScope.SELECTED, Arrays.asList(SchemaConstants.ORG_APPROVER), target);
					}
				}));

		unassignMenuItems
		.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.unassignOwnersSelected"),
				false, new HeaderMenuAction(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeMembersPerformed(QueryScope.SELECTED, Arrays.asList(SchemaConstants.ORG_OWNER), target);
			}
		}));

		unassignMenuItems
		.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.unassignManagersSelected"),
				false, new HeaderMenuAction(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeMembersPerformed(QueryScope.SELECTED, Arrays.asList(SchemaConstants.ORG_MANAGER), target);
			}
		}));

		return unassignMenuItems;
	}

    private void removeAllMembersPerformed(AjaxRequestTarget target) {

    	RoleRelationSelectionPanel relatioNSelectionPanel = new RoleRelationSelectionPanel(getPageBase().getMainPopupBodyId(), new RoleRelationSelectionDto()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfirmPerformed(IModel<RoleRelationSelectionDto> model, AjaxRequestTarget target) {
				getPageBase().hideMainPopup(target);

				RoleRelationSelectionDto relationsSelected = model.getObject();
				ArrayList<QName> relations=  new ArrayList<>();
				if (relationsSelected.isApprover()) {
					relations.add(SchemaConstants.ORG_APPROVER);
				}

				if (relationsSelected.isOwner()) {
					relations.add(SchemaConstants.ORG_OWNER);
				}

				if (relationsSelected.isManager()) {
					relations.add(SchemaConstants.ORG_MANAGER);
				}

				removeMembersPerformed(QueryScope.ALL, relations, target);
			}
		};

		getPageBase().showMainPopup(relatioNSelectionPanel, target);

	}

    @Override
    protected ObjectQuery createAllMemberQuery(List<QName> relations) {
        return super.createDirectMemberQuery(relations);
    }

    @Override
    protected boolean isAuthorizedToUnassignMembers(){
        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_URI);
    }

    @Override
    protected boolean isAuthorizedToAssignMembers(){
        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_URI);
    }

    @Override
    protected boolean isAuthorizedToCreateMembers(){
        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ADD_GOVERNANCE_ACTION_URI);
    }

    @Override
    protected List<InlineMenuItem> createMemberRecomputeInlineMenuItems() {
        return new ArrayList<>();
    }

    @Override
    protected boolean isRelationColumnVisible(){
        return true;
    }

    @Override
    protected boolean isGovernance(){
        return true;
    }

    static class RoleRelationSelectionDto implements Serializable {

		private static final long serialVersionUID = 1L;
		private boolean approver;
		private boolean owner;
		private boolean manager;

		public boolean isApprover() {
			return approver;
		}

		public boolean isManager() {
			return manager;
		}

		public boolean isOwner() {
			return owner;
		}
	}

    @Override
    protected boolean isAuthorizedToUnassignAllMembers(){
        return true;
    }

}
