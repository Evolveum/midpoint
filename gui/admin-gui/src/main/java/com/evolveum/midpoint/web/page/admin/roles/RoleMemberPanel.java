/*
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
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.InOidFilter;
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
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.input.ObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
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
	private static final String ID_INDIRECT_MEMBERS = "indirectMembers";

	public RoleMemberPanel(String id, IModel<T> model, PageBase pageBase) {
		super(id, TableId.ROLE_MEMEBER_PANEL, model, pageBase);
	}
	
	public RoleMemberPanel(String id, IModel<T> model, List<RelationTypes> relations, PageBase pageBase) {
		super(id, TableId.ROLE_MEMEBER_PANEL, model, relations, pageBase);
	}

	protected boolean isRole() {
		return true;
	}

	protected PrismContext getPrismContext() {
		return getPageBase().getPrismContext();
	}

		private <V> DropDownChoice<V> createDropDown(String id, IModel<V> defaultModel, final List<V> values,
			IChoiceRenderer<V> renderer) {
		DropDownChoice<V> listSelect = new DropDownChoice<V>(id, defaultModel,
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

	private List<OrgType> createTenantList() {
		ObjectQuery query = QueryBuilder.queryFor(OrgType.class, getPrismContext())
				.item(OrgType.F_TENANT).eq(true)
				.build();
		List<PrismObject<OrgType>> orgs = WebModelServiceUtils.searchObjects(OrgType.class, query,
				new OperationResult("Tenant search"), getPageBase());
		List<OrgType> orgTypes = new ArrayList<>();
		for (PrismObject<OrgType> org : orgs) {
			orgTypes.add(org.asObjectable());
		}

		return orgTypes;
	}

	private List<OrgType> createProjectList() {
		ObjectQuery query = QueryBuilder.queryFor(OrgType.class, getPrismContext())
				.item(OrgType.F_TENANT).eq(true)
				.or().item(OrgType.F_TENANT).isNull()
				.build();
		List<PrismObject<OrgType>> orgs = WebModelServiceUtils.searchObjects(OrgType.class, query,
				new OperationResult("Tenant search"), getPageBase());
		List<OrgType> orgTypes = new ArrayList<>();
		for (PrismObject<OrgType> org : orgs) {
			orgTypes.add(org.asObjectable());
		}
		return orgTypes;
	}

	private MainObjectListPanel<FocusType> getMemberTable() {
		return (MainObjectListPanel<FocusType>) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
	}

	protected AssignmentType createMemberAssignmentToModify(QName relation) throws SchemaException {
		AssignmentType assignmentToModify = createAssignmentToModify(relation);

		DropDownChoice<OrgType> tenantChoice = (DropDownChoice<OrgType>) get(ID_TENANT);
		OrgType tenant = tenantChoice.getModelObject();
		if (tenant != null) {
			assignmentToModify.setTenantRef(ObjectTypeUtil.createObjectRef(tenant.getOid(), ObjectTypes.ORG));
		}
		DropDownChoice<OrgType> projectChoice = (DropDownChoice<OrgType>) get(ID_PROJECT);
		OrgType project = projectChoice.getModelObject();
		if (project != null) {
			assignmentToModify.setOrgRef(ObjectTypeUtil.createObjectRef(project.getOid(), ObjectTypes.ORG));
		}

		return assignmentToModify;
	}

	private ObjectQuery getActionQuery(QueryScope scope) {
		switch (scope) {
			case ALL:
				return createAllMemberQuery();
			case ALL_DIRECT:
				return createDirectMemberQuery( );
			case SELECTED:
				return createRecomputeQuery();
		}

		return null;
	}

	protected ObjectQuery createAllMemberQuery() {
		return QueryBuilder.queryFor(FocusType.class, getPrismContext())
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
				.ref(createReferenceValuesList()).build();
	}

	private List<PrismReferenceValue> createReferenceValuesList() {
		List<PrismReferenceValue> referenceValuesList = new ArrayList<>();
		if (relations != null && relations.size() > 0){
			for (RelationTypes relation : relations) {
				PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
				rv.setRelation(relation.getRelation());
				referenceValuesList.add(rv);
			}
		} else {
			PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
			referenceValuesList.add(rv);
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
	protected void initCustomLayout(Form form) {

	}

	@Override
	protected void initSearch(Form form) {
		List<QName> allowedTypes = WebComponentUtil.createFocusTypeList();
		allowedTypes.add(FocusType.COMPLEX_TYPE);
		DropDownChoice<QName> typeSelect = createDropDown(ID_OBJECT_TYPE, Model.of(FocusType.COMPLEX_TYPE),
				allowedTypes, new QNameChoiceRenderer());
		add(typeSelect);
		

		DropDownChoice<OrgType> tenant = createDropDown(ID_TENANT, new Model(),
				createTenantList(), new ObjectTypeChoiceRenderer<OrgType>());
		add(tenant);
		tenant.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isRole();
			}
		});
		
		DropDownChoice<OrgType> project = createDropDown(ID_PROJECT, new Model(),
				createProjectList(), new ObjectTypeChoiceRenderer<OrgType>());
		add(project);
		
		project.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isRole();
			}
		});
		
		CheckBoxPanel includeIndirectMembers = new CheckBoxPanel(ID_INDIRECT_MEMBERS, new Model<Boolean>(false)) {
			private static final long serialVersionUID = 1L;

			public void onUpdate(AjaxRequestTarget target) {
				refreshTable(target);
			}
		};
		add(includeIndirectMembers);
		includeIndirectMembers.add(new VisibleBehaviour(this::isRole));		// TODO shouldn't we hide also the label?

	}

	@Override
	protected void addMembersPerformed(QName type, QName relation, List selected, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask(getTaskName("Add", null));
		ObjectDelta delta = prepareDelta(type, relation, MemberOperation.ADD, operationalTask.getResult());
		executeMemberOperation(operationalTask, type, createQueryForAdd(selected), delta,
				TaskCategory.EXECUTE_CHANGES, target);

	}

	private ObjectDelta prepareDelta(QName type, QName relation, MemberOperation operation, OperationResult result) {
		Class classType = WebComponentUtil.qnameToClass(getPrismContext(), type);
		ObjectDelta delta = null;
		try {
			switch (operation) {
				case ADD:

					delta = ObjectDelta.createModificationAddContainer(classType, "fakeOid",
							FocusType.F_ASSIGNMENT, getPrismContext(), createMemberAssignmentToModify(relation));

					break;

				case REMOVE:
					delta = getDeleteAssignmentDelta(classType);
					break;
			}
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + operation.name(), e);
			result.recordFatalError("Failed to prepare delta for operation " + operation.name(), e);
		}
		return delta;
	}

	protected ObjectDelta getDeleteAssignmentDelta(Class classType) throws SchemaException {
		return ObjectDelta.createModificationDeleteContainer(classType, "fakeOid",
				FocusType.F_ASSIGNMENT, getPrismContext(), createMemberAssignmentToModify(null));
	}

	@Override
	protected void removeMembersPerformed(QueryScope scope, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask(getTaskName("Remove", scope));
		ObjectDelta delta = prepareDelta(FocusType.COMPLEX_TYPE, null, MemberOperation.REMOVE, operationalTask.getResult());
		executeMemberOperation(operationalTask, FocusType.COMPLEX_TYPE, getActionQuery(scope), delta,
				TaskCategory.EXECUTE_CHANGES, target);

	}

	@Override
	protected void recomputeMembersPerformed(QueryScope scope, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask(getTaskName("Recompute", scope));
		executeMemberOperation(operationalTask, FocusType.COMPLEX_TYPE, getActionQuery(scope), null,
				TaskCategory.RECOMPUTATION, target);

	}

	@Override
	protected ObjectQuery createContentQuery() {
		boolean indirect = ((CheckBoxPanel) get(createComponentPath(ID_INDIRECT_MEMBERS))).getValue();

		return indirect ? createAllMemberQuery() : createDirectMemberQuery();
		
	}

	protected ObjectQuery createDirectMemberQuery() {
		ObjectQuery query;

		String oid = getModelObject().getOid();
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, getPrismContext())
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
				.ref(createReferenceValuesList());
		DropDownChoice<OrgType> tenantChoice = (DropDownChoice) get(createComponentPath(ID_TENANT));
		OrgType tenant = tenantChoice.getModelObject();
		if (tenant != null) {
			q = q.and().item(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF).ref(createReference(tenant).asReferenceValue());
		}

		DropDownChoice<OrgType> projectChoice = (DropDownChoice) get(createComponentPath(ID_PROJECT));
		OrgType project = projectChoice.getModelObject();
		if (project != null) {
			q = q.and().item(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(createReference(project).asReferenceValue());
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
}
