/*
 * Copyright (c) 2015-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoleMemberPanel<T extends AbstractRoleType> extends AbstractRoleMemberPanel<T> {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(RoleMemberPanel.class);

	private static final String ID_OBJECT_TYPE = "type";
	private static final String ID_TENANT = "tenant";
	private static final String ID_PROJECT = "project";
	private static final String ID_INDIRECT_MEMBERS_CONTAINER = "indirectMembersContainer";
	private static final String ID_INDIRECT_MEMBERS = "indirectMembers";

	public RoleMemberPanel(String id, IModel<T> model) {
		super(id, TableId.ROLE_MEMEBER_PANEL, model);
	}

	public RoleMemberPanel(String id, IModel<T> model, List<RelationTypes> relations) {
		super(id, TableId.ROLE_MEMEBER_PANEL, model, relations);
	}

	private boolean indirectMembersContainerVisibility() {
		return isRole() && !isGovernance();
	}

	protected boolean isRole() {
		return true;
	}

	protected boolean isGovernance(){
		return false;
	}

	protected PrismContext getPrismContext() {
		return getPageBase().getPrismContext();
	}

		private <V> DropDownChoice<V> createDropDown(String id, IModel<V> defaultModel, final List<V> values,
			IChoiceRenderer<V> renderer) {
		DropDownChoice<V> listSelect = new DropDownChoice<>(id, defaultModel,
            new AbstractReadOnlyModel<List<V>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<V> getObject() {
                    return values;
                }
            }, renderer);

		listSelect.add(new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				refreshTable(target);
			}
		});

		return listSelect;
	}

	protected void refreshTable(AjaxRequestTarget target) {
		DropDownChoice<QName> typeChoice = (DropDownChoice) get(createComponentPath(ID_OBJECT_TYPE));
		QName type = typeChoice.getModelObject();
		getMemberTable().clearCache();
		getMemberTable().refreshTable(WebComponentUtil.qnameToClass(getPrismContext(), type, FocusType.class), target);
	}



	private MainObjectListPanel<FocusType> getMemberTable() {
		return (MainObjectListPanel<FocusType>) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
	}

	protected AssignmentType createMemberAssignmentToModify(QName relation) throws SchemaException {
		AssignmentType assignmentToModify = createAssignmentToModify(relation);

		ChooseTypePanel<OrgType> tenantChoice = (ChooseTypePanel) get(createComponentPath(ID_TENANT));
		ObjectViewDto<OrgType> tenant = tenantChoice.getModelObject();
		if (tenant != null && tenant.getObjectType() != null) {
			assignmentToModify.setTenantRef(ObjectTypeUtil.createObjectRef(tenant.getObjectType().getOid(), ObjectTypes.ORG));
		}
		ChooseTypePanel<OrgType> projectChoice = (ChooseTypePanel) get(createComponentPath(ID_PROJECT));
		ObjectViewDto<OrgType> project = projectChoice.getModelObject();
		if (project != null && project.getObjectType() != null) {
			assignmentToModify.setOrgRef(ObjectTypeUtil.createObjectRef(project.getObjectType().getOid(), ObjectTypes.ORG));
		}

		return assignmentToModify;
	}

	private ObjectQuery getActionQuery(QueryScope scope, List<QName> relations) {
		switch (scope) {
			case ALL:
				return createAllMemberQuery(relations);
			case ALL_DIRECT:
				return createDirectMemberQuery(relations);
			case SELECTED:
				return createRecomputeQuery();
		}

		return null;
	}

	protected ObjectQuery createAllMemberQuery(List<QName> relations) {
		return QueryBuilder.queryFor(FocusType.class, getPrismContext())
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(createReferenceValuesList(relations))
				.build();
	}

	private List<PrismReferenceValue> createReferenceValuesList(List<QName> relations) {
		List<PrismReferenceValue> referenceValuesList = new ArrayList<>();
		if (relations != null && relations.size() > 0){
			if (!CollectionUtils.isEmpty(relations)) {
				for (QName relation : relations) {
					referenceValuesList.add(createReference(relation).asReferenceValue());
				}
			}
		} else {
			referenceValuesList.add(createReference().asReferenceValue());
		}

		return referenceValuesList;

	}

	private ObjectQuery createRecomputeQuery() {
		Set<String> oids = getFocusOidToRecompute();
		ObjectQuery query = ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
		return query;
	}

	private Set<String> getFocusOidToRecompute() {
		List<FocusType> availableData = getMemberTable().getSelectedObjects();
		Set<String> oids = new HashSet<>();
		for (FocusType focus : availableData) {
			oids.add(focus.getOid());

		}
		return oids;
	}

	@Override
	protected void initCustomLayout(Form form, ModelServiceLocator serviceLocator) {

	}

	@Override
	protected void initSearch(Form form) {
		List<QName> allowedTypes = WebComponentUtil.createFocusTypeList();
		allowedTypes.add(FocusType.COMPLEX_TYPE);
		DropDownChoice<QName> typeSelect = createDropDown(ID_OBJECT_TYPE, Model.of(FocusType.COMPLEX_TYPE),
				allowedTypes, new QNameChoiceRenderer());
		add(typeSelect);

		ChooseTypePanel<OrgType> tenant = createParameterPanel(ID_TENANT, true);

//			DropDownChoice<OrgType> tenant = createDropDown(ID_TENANT, new Model(),
//				createTenantList(), new ObjectTypeChoiceRenderer<OrgType>());
		add(tenant);
		tenant.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isRole();
			}
		});

		ChooseTypePanel<OrgType> project = createParameterPanel(ID_PROJECT, false);
		add(project);

		project.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isRole();
			}
		});

		WebMarkupContainer indirectMembersContainer = new WebMarkupContainer(ID_INDIRECT_MEMBERS_CONTAINER);
		indirectMembersContainer.setOutputMarkupId(true);
		indirectMembersContainer.add(new VisibleBehaviour(this::indirectMembersContainerVisibility));
		add(indirectMembersContainer);

		IsolatedCheckBoxPanel includeIndirectMembers = new IsolatedCheckBoxPanel(ID_INDIRECT_MEMBERS, new Model<>(false)) {
			private static final long serialVersionUID = 1L;

			public void onUpdate(AjaxRequestTarget target) {
				refreshTable(target);
			}
		};
		indirectMembersContainer.add(includeIndirectMembers);

	}

	private ChooseTypePanel<OrgType> createParameterPanel(String id, boolean isTenant) {

		ChooseTypePanel<OrgType> orgSelector = new ChooseTypePanel<OrgType>(id, Model.of(new ObjectViewDto())) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void executeCustomAction(AjaxRequestTarget target, OrgType object) {
				refreshTable(target);
			}

			@Override
			protected void executeCustomRemoveAction(AjaxRequestTarget target) {
				refreshTable(target);
			}

			@Override
			protected ObjectQuery getChooseQuery() {
				ObjectFilter tenantFilter = QueryBuilder.queryFor(OrgType.class, getPrismContext()).item(OrgType.F_TENANT).eq(true).buildFilter();

				if (isTenant) {
					return ObjectQuery.createObjectQuery(tenantFilter);
				}
				return ObjectQuery.createObjectQuery(NotFilter.createNot(tenantFilter));

			}

			@Override
			protected boolean isSearchEnabled() {
				return true;
			}

			@Override
			public Class<OrgType> getObjectTypeClass() {
				return OrgType.class;
			}

			@Override
			protected AttributeAppender getInputStyleClass(){
				return AttributeAppender.append("class", "col-md-6");
			}

		};

		return orgSelector;

	}

	@Override
	protected void addMembersPerformed(QName type, List<QName> relation, List selected, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask(getTaskName("Add", null));
		ObjectDelta delta = prepareDelta(type, relation, MemberOperation.ADD, operationalTask.getResult());
		executeMemberOperation(operationalTask, type, createQueryForAdd(selected), delta,
				TaskCategory.EXECUTE_CHANGES, target);

	}

	private ObjectDelta prepareDelta(QName type, List<QName> relations, MemberOperation operation, OperationResult result) {
		Class classType = WebComponentUtil.qnameToClass(getPrismContext(), type);
		ObjectDelta delta = null;
		try {
			switch (operation) {
				case ADD:
					if (relations == null || relations.isEmpty()) {
					delta = ObjectDelta.createModificationAddContainer(classType, "fakeOid",
							FocusType.F_ASSIGNMENT, getPrismContext(), createMemberAssignmentToModify(null));
					} else {
						delta =  ObjectDelta.createEmptyModifyDelta(classType, "fakeOid", getPrismContext());
						
						for (QName relation : relations) {
							delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, createAssignmentToModify(relation));
						}
						
						return delta;  
					}
					break;

				case REMOVE:
					delta = getDeleteAssignmentDelta(relations, classType);
					break;
			}
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + operation.name(), e);
			result.recordFatalError("Failed to prepare delta for operation " + operation.name(), e);
		}
		return delta;
	}

	protected ObjectDelta getDeleteAssignmentDelta(List<QName> relations, Class classType) throws SchemaException {
		if (relations == null || relations.isEmpty()) {
			return ObjectDelta.createModificationDeleteContainer(classType, "fakeOid",
					FocusType.F_ASSIGNMENT, getPrismContext(), createMemberAssignmentToModify(null));
		}
		
		ObjectDelta delta =  ObjectDelta.createEmptyModifyDelta(classType, "fakeOid", getPrismContext());
		
		for (QName relation : relations) {
			delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT, createAssignmentToModify(relation));
		}
		
		return delta;  
		
	}

	@Override
	protected void removeMembersPerformed(QueryScope scope, List<QName> relation, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask(getTaskName("Remove", scope));
		ObjectDelta delta = prepareDelta(FocusType.COMPLEX_TYPE, relation, MemberOperation.REMOVE, operationalTask.getResult());
		executeMemberOperation(operationalTask, FocusType.COMPLEX_TYPE, getActionQuery(scope, relation), delta,
				TaskCategory.EXECUTE_CHANGES, target);

	}

	@Override
	protected void recomputeMembersPerformed(QueryScope scope, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask(getTaskName("Recompute", scope));
		executeMemberOperation(operationalTask, FocusType.COMPLEX_TYPE, getActionQuery(scope, null), null,
				TaskCategory.RECOMPUTATION, target);

	}

	@Override
	protected ObjectQuery createContentQuery() {
		boolean indirect = ((IsolatedCheckBoxPanel) get(createComponentPath(ID_INDIRECT_MEMBERS_CONTAINER, ID_INDIRECT_MEMBERS))).getValue();

		List<QName> relationList = new ArrayList<>();
		if (relations != null) {
			for (RelationTypes relation: relations) {
				relationList.add(relation.getRelation());
			}
		}
		
		return indirect ? createAllMemberQuery(relationList) : createDirectMemberQuery(relationList);

	}

	protected ObjectQuery createDirectMemberQuery(List<QName> relations) {
		ObjectQuery query;

		String oid = getModelObject().getOid();
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, getPrismContext())
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
				.ref(createReferenceValuesList(relations));
		ChooseTypePanel<OrgType> tenantChoice = (ChooseTypePanel) get(createComponentPath(ID_TENANT));
		ObjectViewDto<OrgType> tenant = tenantChoice.getModelObject();
		if (tenant != null && tenant.getObjectType() != null) {
			q = q.and().item(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF).ref(createReference(tenant.getObjectType()).asReferenceValue());
		}

		ChooseTypePanel<OrgType> projectChoice = (ChooseTypePanel) get(createComponentPath(ID_PROJECT));
		ObjectViewDto<OrgType> project = projectChoice.getModelObject();
		if (project != null && project.getObjectType() !=null) {
			q = q.and().item(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(createReference(project.getObjectType()).asReferenceValue());
		}

		query = q.build();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching members of role {} with query:\n{}", oid, query.debugDump());
		}

		DropDownChoice<QName> objectTypeChoice = (DropDownChoice) get(createComponentPath(ID_OBJECT_TYPE));
		QName objectType = objectTypeChoice.getModelObject();
		if (objectType == null || FocusType.COMPLEX_TYPE.equals(objectType)) {
			return query;
		} else {
			return ObjectQuery.createObjectQuery(TypeFilter.createType(objectType, query.getFilter()));
		}
	}

	@Override
	protected List<QName> getNewMemberSupportedTypes(){
		return WebComponentUtil.createFocusTypeList();
	}
}
