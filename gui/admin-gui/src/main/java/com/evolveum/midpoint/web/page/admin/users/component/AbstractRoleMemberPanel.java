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
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeDialogPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.model.Model;

public abstract class AbstractRoleMemberPanel<T extends AbstractRoleType> extends BasePanel<T> {

	private static final long serialVersionUID = 1L;

	protected enum QueryScope {
		SELECTED, ALL, ALL_DIRECT
	}

	protected enum MemberOperation {
		ADD, REMOVE, RECOMPUTE
	}

	private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
	private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_APPROVER_RELATION_OBJECTS = DOT_CLASS + "loadApproverRelationObjects";
	private static final String OPERATION_LOAD_OWNER_RELATION_OBJECTS = DOT_CLASS + "loadOwnerRelationObjects";
	private static final String OPERATION_LOAD_MANAGER_RELATION_OBJECTS = DOT_CLASS + "loadManagerRelationObjects";

	protected static final String ID_FORM = "form";
	protected static final String ID_CONTAINER_MANAGER = "managerContainer";
	protected static final String ID_CONTAINER_MEMBER = "memberContainer";
	protected static final String ID_CHILD_TABLE = "childUnitTable";
	protected static final String ID_MANAGER_TABLE = "managerTable";
	protected static final String ID_MEMBER_TABLE = "memberTable";

	protected List<RelationTypes> relations = new ArrayList<>();
	private TableId tableId;

	private LoadableModel<List<String>> approverRelationObjectsModel;
	private LoadableModel<List<String>> ownerRelationObjectsModel;
	private LoadableModel<List<String>> managerRelationObjectsModel;
	protected LoadableModel<List<String>> memberRelationObjectsModel;

	private boolean areModelsInitialized = false;

	public AbstractRoleMemberPanel(String id, TableId tableId, IModel<T> model) {
		this(id, tableId, model, new ArrayList<>());
	}

	public AbstractRoleMemberPanel(String id, TableId tableId, IModel<T> model,
								   List<RelationTypes> relations) {
		super(id, model);
		this.relations = relations;
		this.tableId = tableId;
	}

	@Override
	protected void onInitialize(){
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {
		Form form = new com.evolveum.midpoint.web.component.form.Form(ID_FORM);
		form.setOutputMarkupId(true);
		add(form);

		initSearch(form);

		loadAllRelationModels();
		initMemberTable(form);

		initCustomLayout(form, getPageBase());
	}

	protected abstract void initCustomLayout(Form form, ModelServiceLocator serviceLocator);

	protected abstract void initSearch(Form form);

	private void initMemberTable(Form form) {
		WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
		memberContainer.setOutputMarkupId(true);
		memberContainer.setOutputMarkupPlaceholderTag(true);
		form.add(memberContainer);

		PageBase pageBase =  getPageBase();
		MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<ObjectType>(
				ID_MEMBER_TABLE, ObjectType.class, tableId, getSearchOptions(), pageBase) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
				detailsPerformed(target, object);
			}

			@Override
			protected PrismObject<ObjectType> getNewObjectListObject(){
				return null;
			}

			@Override
			protected boolean isClickable(IModel<SelectableBean<ObjectType>> rowModel) {
				if (rowModel == null || rowModel.getObject() == null
						|| rowModel.getObject().getValue() == null) {
					return false;
				}
				Class<?> objectClass = rowModel.getObject().getValue().getClass();
				return WebComponentUtil.hasDetailsPage(objectClass);
			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				AbstractRoleMemberPanel.this.createFocusMemberPerformed(null, target);
			}

			@Override
			protected List<IColumn<SelectableBean<ObjectType>, String>> createColumns() {
				return createMembersColumns();
			}

			@Override
			protected IColumn<SelectableBean<ObjectType>, String> createActionsColumn(){
				return new InlineMenuHeaderColumn(createMembersHeaderInlineMenu());
			}

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				return new ArrayList<>();
			}

			@Override
			protected Search createSearch() {
				return SearchFactory.createSearch(getDefaultObjectType(), pageBase);
			}

			@Override
			protected ObjectQuery createContentQuery() {
				ObjectQuery q = super.createContentQuery();

				ObjectQuery members = AbstractRoleMemberPanel.this.createContentQuery();

				List<ObjectFilter> filters = new ArrayList<>();

				if (q != null && q.getFilter() != null) {
					filters.add(q.getFilter());
				}

				if (members != null && members.getFilter() != null) {
					filters.add(members.getFilter());
				}

				if (filters.size() == 1) {
					return ObjectQuery.createObjectQuery(filters.iterator().next());
				}

				return ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
			}
		};
		childrenListPanel.setOutputMarkupId(true);
		memberContainer.add(childrenListPanel);
	}

	private List<InlineMenuItem> createMembersHeaderInlineMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<>();
		if (isAuthorizedToCreateMembers()){
			headerMenuItems.addAll(createNewMemberInlineMenuItems());
		}
		if (isAuthorizedToAssignMembers()){
			headerMenuItems.addAll(assignNewMemberInlineMenuItems());
		}
		if (isAuthorizedToUnassignMembers()) {
			headerMenuItems.addAll(createUnassignMemberInlineMenuItems());
		}
		if (isAuthorizedToUnassignAllMembers()) {
			headerMenuItems.addAll(createUnassignAllMemberInlineMenuItems());
		}
		if (isAuthorizedToRecomputeMembers()) {
			headerMenuItems.addAll(createMemberRecomputeInlineMenuItems());
		}
		return headerMenuItems;
	}

	protected boolean isAuthorizedToCreateMembers(){
		return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_URI);
	}

	protected boolean isAuthorizedToAssignMembers(){
		return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_MEMBER_ACTION_URI);
	}

	protected boolean isAuthorizedToUnassignMembers(){
		return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_MEMBER_TAB_ACTION_URI);
	}

	protected boolean isAuthorizedToUnassignAllMembers(){
		return true;
	}

	protected boolean isAuthorizedToRecomputeMembers(){
		return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI);
	}

	protected List<InlineMenuItem> createNewMemberInlineMenuItems() {
		List<InlineMenuItem> newMemberMenuItems = new ArrayList<>();
		newMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createMember"),
				false, new HeaderMenuAction(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				createFocusMemberPerformed(null, target);
			}
		}));
		return newMemberMenuItems;
	}

	protected List<InlineMenuItem> assignNewMemberInlineMenuItems() {
		List<InlineMenuItem> newMemberMenuItems = new ArrayList<>();
		newMemberMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addMembers"), false,
				new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						addMembers(null, target);
					}
				}));
		return newMemberMenuItems;
	}

	protected List<InlineMenuItem> createMemberRecomputeInlineMenuItems() {
		List<InlineMenuItem> recomputeMenuItems = new ArrayList<>();
        recomputeMenuItems
                .add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersSelected"),
                        false, new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recomputeMembersPerformed(QueryScope.SELECTED, target);
                    }
                }));
        recomputeMenuItems
                .add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersAllDirect"),
                        false, new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recomputeMembersPerformed(QueryScope.ALL_DIRECT, target);

                    }
                }));

        recomputeMenuItems
                .add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersAll"),
                        false, new HeaderMenuAction(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recomputeMembersPerformed(QueryScope.ALL, target);

                    }
                }));

        return recomputeMenuItems;
	}

	protected List<InlineMenuItem> createUnassignMemberInlineMenuItems() {
		List<InlineMenuItem> unassignMenuItems = new ArrayList<>();
		unassignMenuItems
				.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.unassignMembersSelected"),
						false, new HeaderMenuAction(this) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						removeMembersPerformed(QueryScope.SELECTED, null , target);
					}
				}));
		return unassignMenuItems;
	}

	protected List<InlineMenuItem> createUnassignAllMemberInlineMenuItems() {
		List<InlineMenuItem> unassignMenuItems = new ArrayList<>();
		unassignMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.unassignMembersAll"),
				false, new HeaderMenuAction(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeMembersPerformed(QueryScope.ALL, null, target);
			}
		}));
		return unassignMenuItems;
	}


	protected void createFocusMemberPerformed(final QName relation, AjaxRequestTarget target) {

		ChooseFocusTypeDialogPanel chooseTypePopupContent = new ChooseFocusTypeDialogPanel(
				getPageBase().getMainPopupBodyId()) {
			private static final long serialVersionUID = 1L;

			protected void okPerformed(QName type, AjaxRequestTarget target) {
				try {
					initObjectForAdd(null, type, relation, target);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}

			};
		};

		getPageBase().showMainPopup(chooseTypePopupContent, target);

	}

	// TODO: merge this with TreeTablePanel.initObjectForAdd, also see MID-3233
	private void initObjectForAdd(ObjectReferenceType parentOrgRef, QName type, QName relation,
			AjaxRequestTarget target) throws SchemaException {
		getPageBase().hideMainPopup(target);
		PrismContext prismContext = getPageBase().getPrismContext();
		PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		PrismObject obj = def.instantiate();
		if (parentOrgRef == null) {
			parentOrgRef = createReference(relation);
		}
		ObjectType objType = (ObjectType) obj.asObjectable();
		if (FocusType.class.isAssignableFrom(obj.getCompileTimeClass())) {
			AssignmentType assignment = new AssignmentType();
			assignment.setTargetRef(parentOrgRef);
			((FocusType) objType).getAssignment().add(assignment);
		}

		// Set parentOrgRef in any case. This is not strictly correct.
				// The parentOrgRef should be added by the projector. But
				// this is needed to successfully pass through security
				// TODO: fix MID-3234
		if (parentOrgRef.getType() != null &&  OrgType.COMPLEX_TYPE.equals(parentOrgRef.getType())) {
			objType.getParentOrgRef().add(parentOrgRef.clone());
		}

		WebComponentUtil.dispatchToObjectDetailsPage(obj, true, this);
	}

	protected void addMembers(final QName relation, AjaxRequestTarget target) {

		List<QName> types = getNewMemberSupportedTypes();

		ObjectBrowserPanel<ObjectType> browser = new ObjectBrowserPanel(getPageBase().getMainPopupBodyId(),
				UserType.class, types, true, getPageBase()) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void addPerformed(AjaxRequestTarget target, QName type, List selected) {
				AbstractRoleMemberPanel.this.getPageBase().hideMainPopup(target);
				AbstractRoleMemberPanel.this.addMembersPerformed(type, Arrays.asList(relation), selected, target);

			}
		};
		browser.setOutputMarkupId(true);

		getPageBase().showMainPopup(browser, target);

	}

	protected List<QName> getNewMemberSupportedTypes(){
		List<QName> types = WebComponentUtil.createObjectTypeList();
		types.remove(NodeType.COMPLEX_TYPE);
		types.remove(ShadowType.COMPLEX_TYPE);
		return types;
	}

	protected ObjectQuery createQueryForAdd(List selected) {
		List<String> oids = new ArrayList<>();
		for (Object selectable : selected) {
			if (selectable instanceof ObjectType) {
				oids.add(((ObjectType) selectable).getOid());
			}

		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	protected abstract void addMembersPerformed(QName type, List<QName> relation, List selected,
			AjaxRequestTarget target);

	protected abstract void removeMembersPerformed(QueryScope scope, List<QName> relation, AjaxRequestTarget target);

	protected abstract void recomputeMembersPerformed(QueryScope scope, AjaxRequestTarget target);

	protected void executeMemberOperation(Task operationalTask, QName type, ObjectQuery memberQuery,
			ObjectDelta delta, String category, AjaxRequestTarget target) {

		OperationResult parentResult = operationalTask.getResult();

		try {
			ModelExecuteOptions options = TaskCategory.EXECUTE_CHANGES.equals(category)
					? ModelExecuteOptions.createReconcile()		// This was originally in ExecuteChangesTaskHandler, now it's transferred through task extension.
					: null;
			TaskType task = WebComponentUtil.createSingleRecurrenceTask(parentResult.getOperation(), type,
					memberQuery, delta, options, category, getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {
			parentResult.recordFatalError(parentResult.getOperation(), e);
			LoggingUtils.logUnexpectedException(LOGGER,
					"Failed to execute operation " + parentResult.getOperation(), e);
			target.add(getPageBase().getFeedbackPanel());
		}

		target.add(getPageBase().getFeedbackPanel());
	}

	protected AssignmentType createAssignmentToModify(QName relation) throws SchemaException {

		AssignmentType assignmentToModify = new AssignmentType();

		assignmentToModify.setTargetRef(createReference(relation));

		getPageBase().getPrismContext().adopt(assignmentToModify);

		return assignmentToModify;
	}

	protected ObjectReferenceType createReference(QName relation) {
		ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject());
		ref.setRelation(relation);
		return ref;
	}

	protected ObjectReferenceType createReference() {
		ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject());
		return ref;
	}

	protected ObjectReferenceType createReference(ObjectType obj) {
		ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(obj);
		return ref;
	}

	protected void detailsPerformed(AjaxRequestTarget target, ObjectType object) {
		if (WebComponentUtil.hasDetailsPage(object.getClass())) {
			WebComponentUtil.dispatchToObjectDetailsPage(object.getClass(), object.getOid(), this, true);
		} else {
			error("Could not find proper response page");
			throw new RestartResponseException(getPageBase());
		}
	}

	protected List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
		List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

		IColumn<SelectableBean<ObjectType>, String> column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.fullName.displayName")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				cellItem.add(new Label(componentId,
							getMemberObjectDisplayName(object)));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
				return Model.of(getMemberObjectDisplayName(rowModel.getObject().getValue()));
			}

		};
		columns.add(column);

		column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.identifier.description")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				cellItem.add(new Label(componentId, getMemberObjectIdentifier(object)));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
				return Model.of(getMemberObjectIdentifier(rowModel.getObject().getValue()));
			}

		};
		columns.add(column);
		if (isRelationColumnVisible()){
			columns.add(createRelationColumn());
		}
		return columns;
	}

	protected IColumn<SelectableBean<ObjectType>, String> createRelationColumn() {
		return new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
				createStringResource("roleMemberPanel.relation")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
									 String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				cellItem.add(new Label(componentId,
						getRelationValue((FocusType) rowModel.getObject().getValue())));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
				return Model.of(getRelationValue((FocusType) rowModel.getObject().getValue()));
			}

		};
	}

	protected boolean isRelationColumnVisible(){
		return false;
	}

	protected abstract ObjectQuery createContentQuery();

	protected String getTaskName(String operation, QueryScope scope, boolean managers) {
		StringBuilder nameBuilder = new StringBuilder(operation);
		nameBuilder.append(".");
		if (scope != null) {
			nameBuilder.append(scope.name());
			nameBuilder.append(".");
		}
		if (managers) {
			nameBuilder.append("managers");
		} else {
			nameBuilder.append("members");
		}
//		nameBuilder.append(".");
//		nameBuilder
//				.append(WebComponentUtil.getEffectiveName(getModelObject(), AbstractRoleType.F_DISPLAY_NAME));
		return nameBuilder.toString();
	}

	protected String getTaskName(String operation, QueryScope scope) {
		return getTaskName(operation, scope, false);
	}

	private String getMemberObjectDisplayName(ObjectType object){
		if (object == null){
			return "";
		}
		if (object instanceof UserType) {
			return WebComponentUtil.getOrigStringFromPoly(((UserType) object).getFullName());
		} else if (object instanceof AbstractRoleType) {
			return WebComponentUtil
					.getOrigStringFromPoly(((AbstractRoleType) object).getDisplayName());
		} else {
			return "";
		}
	}

	private String getMemberObjectIdentifier(ObjectType object){
		if (object == null){
			return "";
		}
		if (object instanceof UserType) {
			return ((UserType) object).getEmailAddress();
		} else if (object instanceof AbstractRoleType) {
			return ((AbstractRoleType) object).getIdentifier();
		} else {
			return object.getDescription();
		}
	}

	private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions(){
			return SelectorOptions
					.createCollection(GetOperationOptions.createDistinct());
	}

	protected Class getDefaultObjectType(){
		return ObjectType.class;
	}

	protected Form getFormComponent(){
		return (Form) get(ID_FORM);
	}

	protected void loadAllRelationModels(){
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
		if (memberRelationObjectsModel != null) {
			memberRelationObjectsModel.reset();
		} else {
			initMemberRelationObjectsModel();
		}
	}

	private void initApproverRelationObjectsModel(){
		approverRelationObjectsModel = new LoadableModel<List<String>>(false) {
			@Override
			protected List<String> load() {
				OperationResult result = new OperationResult(OPERATION_LOAD_APPROVER_RELATION_OBJECTS);
				return getObjectOidsList(loadMemberObjectsByRelation(result, RelationTypes.APPROVER.getRelation()));
			}
		};
	}

	private void initOwnerRelationObjectsModel(){
		ownerRelationObjectsModel = new LoadableModel<List<String>>(false) {
			@Override
			protected List<String> load() {
				OperationResult result = new OperationResult(OPERATION_LOAD_OWNER_RELATION_OBJECTS);
				return getObjectOidsList(loadMemberObjectsByRelation(result, RelationTypes.OWNER.getRelation()));
			}
		};
	}

	private void initManagerRelationObjectsModel(){
		managerRelationObjectsModel = new LoadableModel<List<String>>(false) {
			@Override
			protected List<String> load() {
				OperationResult result = new OperationResult(OPERATION_LOAD_MANAGER_RELATION_OBJECTS);
				return getObjectOidsList(loadMemberObjectsByRelation(result, RelationTypes.MANAGER.getRelation()));
			}
		};
	}

	protected void initMemberRelationObjectsModel(){
		memberRelationObjectsModel = new LoadableModel<List<String>>(false) {
			@Override
			protected List<String> load() {
				return new ArrayList<>();
			}
		};
	}

	protected List<PrismObject<FocusType>> loadMemberObjectsByRelation(OperationResult result, QName relation){
		PrismReferenceValue rv = new PrismReferenceValue(getModelObject().getOid());
		rv.setRelation(relation);

		ObjectQuery query = QueryBuilder.queryFor(FocusType.class, getPageBase().getPrismContext())
				.item(getMemberRelationQueryItem())
				.ref(rv).build();

		return WebModelServiceUtils.searchObjects(FocusType.class, query, result, getPageBase());
	}

	protected QName[] getMemberRelationQueryItem(){
		return new QName[]{FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF};
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
		if (memberRelationObjectsModel.getObject().contains(focusObject.getOid())){
			relations.append(relations.length() > 0 ? ", " : "");
			relations.append(createStringResource("RelationTypes.MEMBER").getString());
		}
		return relations.toString();
	}

	protected List<String> getObjectOidsList(List<PrismObject<FocusType>> objectList){
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


}
