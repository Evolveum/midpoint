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
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by honchar.
 */
public class RoleGovernanceRelationsPanel extends RoleMemberPanel<RoleType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RoleGovernanceRelationsPanel.class);
    private static final String DOT_CLASS = RoleGovernanceRelationsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_APPROVER_RELATION_OBJECTS = DOT_CLASS + "loadApproverRelationObjects";
    private static final String OPERATION_LOAD_OWNER_RELATION_OBJECTS = DOT_CLASS + "loadOwnerRelationObjects";
    private static final String OPERATION_LOAD_MANAGER_RELATION_OBJECTS = DOT_CLASS + "loadManagerRelationObjects";

    private LoadableModel<List<String>> approverRelationObjectsModel;
    private LoadableModel<List<String>> ownerRelationObjectsModel;
    private LoadableModel<List<String>> managerRelationObjectsModel;

    private boolean areModelsInitialized = false;

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
		
		unassignMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.unassignMembersAll"),
				false, new HeaderMenuAction(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeAllMembersPerformed(target);
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

//    @Override
//    protected ObjectDelta getDeleteAssignmentDelta(Class classType) throws SchemaException {
//        ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(classType, "fakeOid",
//                FocusType.F_ASSIGNMENT, getPrismContext(), createMemberAssignmentToModify(RelationTypes.OWNER.getRelation()));
//        delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT, createMemberAssignmentToModify(RelationTypes.APPROVER.getRelation()));
//        delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT, createMemberAssignmentToModify(RelationTypes.MANAGER.getRelation()));
//        return delta;
//    }

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
    protected List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = super.createMembersColumns();
        IColumn<SelectableBean<ObjectType>, String> column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("roleMemberPanel.relation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                                     String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                loadAllRelationModels();
                cellItem.add(new Label(componentId,
                        getRelationValue((FocusType) rowModel.getObject().getValue())));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getRelationValue((FocusType) rowModel.getObject().getValue()));
            }

        };
        columns.add(column);
        return columns;
    }


    private void loadAllRelationModels(){
        if (approverRelationObjectsModel != null) {
            approverRelationObjectsModel.reset();
        } else {
            initApproverRelationObjectsModel();
        }
        if (managerRelationObjectsModel != null) {
            managerRelationObjectsModel.reset();
        } else {
            initManagerRelationObjectsModel();
        }
        if (ownerRelationObjectsModel != null) {
            ownerRelationObjectsModel.reset();
        } else {
            initOwnerRelationObjectsModel();
        }
    }

    private void initApproverRelationObjectsModel(){
        approverRelationObjectsModel = new LoadableModel<List<String>>(false) {
            @Override
            protected List<String> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_APPROVER_RELATION_OBJECTS);

                PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
                rv.setRelation(RelationTypes.APPROVER.getRelation());

                ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
                        .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(rv).build();

                List<PrismObject<FocusType>> approverRelationObjects =
                        WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                return getObjectOidsList(approverRelationObjects);
            }
        };
    }

    private void initOwnerRelationObjectsModel(){
        ownerRelationObjectsModel = new LoadableModel<List<String>>(false) {
            @Override
            protected List<String> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_OWNER_RELATION_OBJECTS);

                
                PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
                rv.setRelation(RelationTypes.OWNER.getRelation());

                ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
                        .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(rv).build();

                List<PrismObject<FocusType>> ownerRelationObjects =
                        WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                return getObjectOidsList(ownerRelationObjects);
            }
        };
    }

    private void initManagerRelationObjectsModel(){
        managerRelationObjectsModel = new LoadableModel<List<String>>(false) {
            @Override
            protected List<String> load() {
                OperationResult result = new OperationResult(OPERATION_LOAD_MANAGER_RELATION_OBJECTS);

                PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
                rv.setRelation(RelationTypes.MANAGER.getRelation());

                ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
                        .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(rv).build();

                List<PrismObject<FocusType>> managerRelationObjects =
                        WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
                return getObjectOidsList(managerRelationObjects);
            }
        };
    }

    private String getRelationValue(FocusType focusObject){
        StringBuilder relations = new StringBuilder();
        if (focusObject == null){
            return "";
        }

        if (approverRelationObjectsModel.getObject().contains(focusObject.getOid())){
            relations.append(createStringResource("RelationTypes.APPROVER").getString());
        }
        if (ownerRelationObjectsModel.getObject().contains(focusObject.getOid())){
            relations.append(relations.length() > 0 ? ", " : "");
            relations.append(createStringResource("RelationTypes.OWNER").getString());
        }
        if (managerRelationObjectsModel.getObject().contains(focusObject.getOid())){
            relations.append(relations.length() > 0 ? ", " : "");
            relations.append(createStringResource("RelationTypes.MANAGER").getString());
        }
        return relations.toString();
    }

    private List<String> getObjectOidsList(List<PrismObject<FocusType>> objectList){
        List<String> oidsList = new ArrayList<>();
        if (objectList == null){
            return oidsList;
        }
        for (PrismObject<FocusType> object : objectList){
            if (object == null){
                continue;
            }
            if (!oidsList.contains(object.getOid())){
                oidsList.add(object.getOid());
            }
        }
        return oidsList;
    }

class RoleRelationSelectionDto implements Serializable {
		
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

}
