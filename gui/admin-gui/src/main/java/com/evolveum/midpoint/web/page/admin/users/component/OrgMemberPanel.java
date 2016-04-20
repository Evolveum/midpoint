package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel.MemberOperation;
import com.evolveum.midpoint.web.page.admin.users.component.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class OrgMemberPanel extends AbstractRoleMemberPanel<OrgType> {

	// protected static final String ID_CONTAINER_MANAGER = "managerContainer";
	// protected static final String ID_CONTAINER_MEMBER = "memberContainer";
	// protected static final String ID_CHILD_TABLE = "childUnitTable";
	// protected static final String ID_MANAGER_TABLE = "managerTable";
	// protected static final String ID_MEMBER_TABLE = "memberTable";
	// protected static final String ID_FORM = "form";

	private static final Trace LOGGER = TraceManager.getTrace(OrgMemberPanel.class);

	protected static final String ID_SEARCH_SCOPE = "searchScope";
	protected static final String ID_SEARCH_BY_TYPE = "searchByType";

	protected static final String SEARCH_SCOPE_SUBTREE = "subtree";
	protected static final String SEARCH_SCOPE_ONE = "one";

	protected static final ObjectTypes OBJECT_TYPES_DEFAULT = ObjectTypes.OBJECT;

	protected static final List<String> SEARCH_SCOPE_VALUES = Arrays.asList(SEARCH_SCOPE_SUBTREE,
			SEARCH_SCOPE_ONE);

	protected static final String DOT_CLASS = OrgMemberPanel.class.getName() + ".";
	protected static final String OPERATION_SEARCH_MANAGERS = DOT_CLASS + "searchManagers";
	private static final String ID_MANAGER_SUMMARY = "managerSummary";
	private static final String ID_REMOVE_MANAGER = "removeManager";
	private static final String ID_EDIT_MANAGER = "editManager";

	private static final long serialVersionUID = 1L;

	public OrgMemberPanel(String id, IModel<OrgType> model, PageBase parentPage) {
		super(id, model, parentPage);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initSearch(Form form) {

		DropDownChoice<ObjectTypes> objectType = new DropDownChoice<ObjectTypes>(ID_SEARCH_BY_TYPE,
				Model.of(OBJECT_TYPES_DEFAULT), Arrays.asList(ObjectTypes.values()),
				new EnumChoiceRenderer<ObjectTypes>());
		objectType.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				refreshTable(target);

			}
		});
		objectType.setOutputMarkupId(true);
		form.add(objectType);

		DropDownChoice<String> seachScrope = new DropDownChoice<String>(ID_SEARCH_SCOPE,
				Model.of(SEARCH_SCOPE_SUBTREE), SEARCH_SCOPE_VALUES,
				new StringResourceChoiceRenderer("TreeTablePanel.search.scope"));
		seachScrope.add(new OnChangeAjaxBehavior() {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				refreshTable(target);
			}
		});
		seachScrope.setOutputMarkupId(true);
		form.add(seachScrope);

	}

	// private void initTables(Form form) {

	// WebMarkupContainer memberContainer = new
	// WebMarkupContainer(ID_CONTAINER_MEMBER);
	// memberContainer.setOutputMarkupId(true);
	// memberContainer.setOutputMarkupPlaceholderTag(true);
	// form.add(memberContainer);
	//
	// MainObjectListPanel<ObjectType> childrenListPanel = new
	// MainObjectListPanel<ObjectType>(
	// ID_MEMBER_TABLE, ObjectType.class, null, parentPage) {
	//
	// @Override
	// protected void objectDetailsPerformed(AjaxRequestTarget target,
	// ObjectType object) {
	// detailsPerformed(target, object);
	//
	// }
	//
	// @Override
	// protected void newObjectPerformed(AjaxRequestTarget target) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// protected List<IColumn<SelectableBean<ObjectType>, String>>
	// createColumns() {
	// return createMembersColumns();
	// }
	//
	// @Override
	// protected List<InlineMenuItem> createInlineMenu() {
	// return new ArrayList<>();
	// }
	//
	// @Override
	// protected ObjectQuery createContentQuery() {
	// ObjectQuery q = super.createContentQuery();
	//
	// ObjectQuery members = createMemberQuery();
	//
	// List<ObjectFilter> filters = new ArrayList<>();
	//
	// if (q != null && q.getFilter() != null) {
	// filters.add(q.getFilter());
	// }
	//
	// if (members != null && members.getFilter() != null) {
	// filters.add(members.getFilter());
	// }
	//
	// if (filters.size() == 1) {
	// return ObjectQuery.createObjectQuery(filters.iterator().next());
	// }
	//
	// return ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
	// }
	// };
	// childrenListPanel.setOutputMarkupId(true);
	// memberContainer.add(childrenListPanel);
	//
	// WebMarkupContainer managerContainer = createManagerContainer();
	// form.addOrReplace(managerContainer);
	// }

	@Override
	protected void initCustomLayout(Form form) {
		WebMarkupContainer managerContainer = createManagerContainer();
		form.addOrReplace(managerContainer);
	}

	private WebMarkupContainer createManagerContainer() {
		WebMarkupContainer managerContainer = new WebMarkupContainer(ID_CONTAINER_MANAGER);
		managerContainer.setOutputMarkupId(true);
		managerContainer.setOutputMarkupPlaceholderTag(true);

		RepeatingView view = new RepeatingView(ID_MANAGER_TABLE);
		view.setOutputMarkupId(true);
		ObjectQuery managersQuery = createManagerQuery();

		OperationResult searchManagersResult = new OperationResult(OPERATION_SEARCH_MANAGERS);
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
				FocusType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
		List<PrismObject<FocusType>> managers = WebModelServiceUtils.searchObjects(FocusType.class,
				managersQuery, options, searchManagersResult, getPageBase());
		for (PrismObject<FocusType> manager : managers) {
			ObjectWrapper<FocusType> managerWrapper = ObjectWrapperUtil.createObjectWrapper(
					WebComponentUtil.getEffectiveName(manager, RoleType.F_DISPLAY_NAME), "", manager,
					ContainerStatus.MODIFYING, getPageBase());
			WebMarkupContainer managerMarkup = new WebMarkupContainer(view.newChildId());

			AjaxLink link = new AjaxLink(ID_EDIT_MANAGER) {
				@Override
				public void onClick(AjaxRequestTarget target) {
					FocusSummaryPanel<FocusType> summary = (FocusSummaryPanel<FocusType>) getParent()
							.get(ID_MANAGER_SUMMARY);
					detailsPerformed(target, summary.getModelObject());

				}
			};
			if (manager.getCompileTimeClass().equals(UserType.class)) {
				managerMarkup.add(new UserSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			} else if (manager.getCompileTimeClass().equals(RoleType.class)) {
				managerMarkup.add(new RoleSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			} else if (manager.getCompileTimeClass().equals(OrgType.class)) {
				managerMarkup.add(new OrgSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			} else if (manager.getCompileTimeClass().equals(ServiceType.class)) {
				managerMarkup.add(new ServiceSummaryPanel(ID_MANAGER_SUMMARY, new Model(managerWrapper)));
			}
			link.setOutputMarkupId(true);
			managerMarkup.setOutputMarkupId(true);
			managerMarkup.add(link);
			view.add(managerMarkup);

			AjaxButton removeManager = new AjaxButton(ID_REMOVE_MANAGER) {

				@Override
				public void onClick(AjaxRequestTarget target) {
					FocusSummaryPanel<FocusType> summary = (FocusSummaryPanel<FocusType>) getParent()
							.get(ID_MANAGER_SUMMARY);
					removeManagerPerformed(summary.getModelObject(), target);
					getParent().setVisible(false);
					target.add(getParent());

				}
			};
			removeManager.setOutputMarkupId(true);
			managerMarkup.add(removeManager);
		}

		managerContainer.add(view);
		return managerContainer;
	}

	private void removeManagerPerformed(FocusType manager, AjaxRequestTarget target) {
		OperationResult parentResult = new OperationResult("Remove manager");
		Task task = getPageBase().createSimpleTask("Remove manager");
		try {

			ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(
					manager.asPrismObject().getCompileTimeClass(), manager.getOid(), FocusType.F_ASSIGNMENT,
					getPageBase().getPrismContext(),
					createAssignmentToModify(manager.asPrismObject().getDefinition().getTypeName(),
							SchemaConstants.ORG_MANAGER));

			getPageBase().getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta),
					null, task, parentResult);
			parentResult.computeStatus();
		} catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {

			parentResult.recordFatalError("Failed to remove manager " + e.getMessage(), e);
			LoggingUtils.logException(LOGGER, "Failed to remove manager", e);
			getPageBase().showResult(parentResult);
		}
		target.add(getPageBase().getFeedbackPanel());
	}

	protected void refreshTable(AjaxRequestTarget target) {
		DropDownChoice<ObjectTypes> typeChoice = (DropDownChoice) get(
				createComponentPath(ID_FORM, ID_SEARCH_BY_TYPE));
		ObjectTypes type = typeChoice.getModelObject();
		target.add(get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE)));
		getMemberTable().clearCache();
		getMemberTable().refreshTable(qnameToClass(type.getTypeQName()), target);
	}

	private MainObjectListPanel<ObjectType> getMemberTable() {
		return (MainObjectListPanel<ObjectType>) get(
				createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
	}

	private TablePanel getManagerTable() {
		return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_MANAGER, ID_MANAGER_TABLE));
	}

	private void selectTreeItemPerformed(AjaxRequestTarget target) {

		getMemberTable().refreshTable(null, target);

		Form mainForm = (Form) get(ID_FORM);
		mainForm.addOrReplace(createManagerContainer());
		target.add(mainForm);
	}

	private QName getSearchType() {
		DropDownChoice<ObjectTypes> searchByTypeChoice = (DropDownChoice) get(
				createComponentPath(ID_FORM, ID_SEARCH_BY_TYPE));
		ObjectTypes typeModel = searchByTypeChoice.getModelObject();
		return typeModel.getTypeQName();
	}

	private ObjectQuery createManagerQuery() {

		String oid = getModelObject().getOid();
		PrismReferenceValue referenceFilter = new PrismReferenceValue();
		referenceFilter.setOid(oid);
		referenceFilter.setRelation(SchemaConstants.ORG_MANAGER);
		RefFilter referenceOidFilter;
		try {
			referenceOidFilter = RefFilter.createReferenceEqual(new ItemPath(FocusType.F_PARENT_ORG_REF),
					UserType.class, getPageBase().getPrismContext(), referenceFilter);
			ObjectQuery query = ObjectQuery.createObjectQuery(referenceOidFilter);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
			}
			return query;
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. managers.", e);
			return null;
		}

	}

	private ObjectDelta prepareDelta(MemberOperation operaton, QName type, QName relation,
			OperationResult result, AjaxRequestTarget target) {
		ObjectDelta delta = null;
		try {
			delta = createMemberDelta(operaton, type, relation);
		} catch (SchemaException e) {
			result.recordFatalError("Failed to prepare delta for add members");
			getPageBase().showResult(result);
			target.add(getPageBase().getFeedbackPanel());
		}
		return delta;
	}

	@Override
	protected void addMembersPerformed(QName type, List selected, AjaxRequestTarget target) {
		// OperationResult parentResult = new OperationResult("Add members");
		// Task operationalTask = getPageBase().createSimpleTask("Add members");
		//
		// try {
		Task operationalTask = getPageBase().createSimpleTask("Add members");
		ObjectDelta delta = prepareDelta(MemberOperation.ADD, type, null, operationalTask.getResult(),
				target);
		if (delta == null) {
			return;
		}
		executeMemberOperation(operationalTask, type, createQueryForAdd(selected), delta,
				TaskCategory.EXECUTE_CHANGES, target);
				// TaskType task =
				// WebComponentUtil.createSingleRecurenceTask("Add member(s)",
				// type,
				// createQueryForAdd(selected), delta,
				// TaskCategory.EXECUTE_CHANGES, getPageBase());
				// //createQueryForAdd(selected)
				// WebModelServiceUtils.runTask(task, operationalTask,
				// parentResult, getPageBase());
				// } catch (SchemaException e) {
				// parentResult.recordFatalError("Failed to add members " +
				// e.getMessage(), e);
				// LoggingUtils.logException(LOGGER, "Failed to remove members",
				// e);
				// getPageBase().showResult(parentResult);
				// }

		// target.add(getPageBase().getFeedbackPanel());

	}

	@Override
	protected void removeMembersPerformed(QueryScope scope, AjaxRequestTarget target) {
		// OperationResult parentResult = new OperationResult("Remove members "
		// + scope.name());
		Task operationalTask = getPageBase().createSimpleTask("Remove members " + scope.name());
		// try {

		ObjectDelta delta = prepareDelta(MemberOperation.REMOVE, FocusType.COMPLEX_TYPE, null,
				operationalTask.getResult(), target);
		if (delta != null) {
			executeMemberOperation(operationalTask, FocusType.COMPLEX_TYPE,
					createQueryForMemberAction(scope, true), delta, TaskCategory.EXECUTE_CHANGES, target);
		}

		// TaskType task = WebComponentUtil.createSingleRecurenceTask("Remove
		// focus member(s)",
		// FocusType.COMPLEX_TYPE, createQueryForMemberAction(scope, true),
		// delta, TaskCategory.EXECUTE_CHANGES, //createQueryForFocusRemove()
		// getPageBase());

		delta = prepareDelta(MemberOperation.REMOVE, ObjectType.COMPLEX_TYPE, null,
				operationalTask.getResult(), target);
		if (delta == null) {
			return;
		}
		executeMemberOperation(operationalTask, ObjectType.COMPLEX_TYPE,
				createQueryForMemberAction(scope, false), delta, TaskCategory.EXECUTE_CHANGES, target);
		// WebModelServiceUtils.runTask(task, operationalTask, parentResult,
		// getPageBase());
		//
		// delta =
		// ObjectDelta.createModificationDeleteReference(ObjectType.class,
		// "fakeOid",
		// ObjectType.F_PARENT_ORG_REF, getPageBase().getPrismContext(),
		// createReference(null).asReferenceValue());
		//
		// task = WebComponentUtil.createSingleRecurenceTask("Remove non-focus
		// member(s)",
		// ObjectType.COMPLEX_TYPE, createQueryForMemberAction(scope, false),
		// delta, //createQueryForNonFocusRemove()
		// TaskCategory.EXECUTE_CHANGES, getPageBase());
		// WebModelServiceUtils.runTask(task, operationalTask, parentResult,
		// getPageBase());
		// } catch (SchemaException e) {
		//
		// parentResult.recordFatalError("Failed to remove members " +
		// e.getMessage(), e);
		// LoggingUtils.logException(LOGGER, "Failed to remove members", e);
		// getPageBase().showResult(parentResult);
		// }
		// target.add(getPageBase().getFeedbackPanel());
	}

	@Override
	protected void recomputeMembersPerformed(QueryScope scope, AjaxRequestTarget target) {
		Task operationalTask = getPageBase().createSimpleTask("Recompute members " + scope.name());
		executeMemberOperation(operationalTask, ObjectType.COMPLEX_TYPE,
				createQueryForMemberAction(scope, true), null, TaskCategory.RECOMPUTATION, target);
		// Task operationalTask = getPageBase().createSimpleTask("Recompute
		// members " + scope.name());
		// OperationResult parentResult = operationalTask.getResult();
		//
		// try {
		// TaskType task = WebComponentUtil.createSingleRecurenceTask("Recompute
		// member(s)",
		// ObjectType.COMPLEX_TYPE, createQueryForMemberAction(scope, true),
		// null, TaskCategory.RECOMPUTATION,
		// getPageBase());
		// WebModelServiceUtils.runTask(task, operationalTask, parentResult,
		// getPageBase());
		// } catch (SchemaException e) {
		// parentResult.recordFatalError("Failed to remove members " +
		// e.getMessage(), e);
		// LoggingUtils.logException(LOGGER, "Failed to remove members", e);
		// target.add(getPageBase().getFeedbackPanel());
		// }
		//
		// target.add(getPageBase().getFeedbackPanel());
	}

	@Override
	protected ObjectQuery createMemberQuery() {
		ObjectQuery query = null;

		String oid = getModelObject().getOid();

		List<ObjectFilter> filters = new ArrayList<>();

		DropDownChoice<String> searchScopeChoice = (DropDownChoice) get(
				createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
		String scope = searchScopeChoice.getModelObject();

		QName searchType = getSearchType();

		try {
			OrgFilter org;
			if (OrgType.COMPLEX_TYPE.equals(searchType)) {
				if (SEARCH_SCOPE_ONE.equals(scope)) {
					filters.add(OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL));
				} else {
					filters.add(OrgFilter.createOrg(oid, OrgFilter.Scope.SUBTREE));
				}
			}
			PrismReferenceValue referenceFilter = new PrismReferenceValue();
			referenceFilter.setOid(oid);
			RefFilter referenceOidFilter = RefFilter.createReferenceEqual(
					new ItemPath(FocusType.F_PARENT_ORG_REF), UserType.class, getPageBase().getPrismContext(),
					referenceFilter);
			filters.add(referenceOidFilter);

			query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Searching members of org {} with query:\n{}", oid, query.debugDump());
			}

		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Couldn't prepare query for org. members.", e);
		}

		if (searchType.equals(ObjectType.COMPLEX_TYPE)) {
			return query;
		}

		return ObjectQuery.createObjectQuery(TypeFilter.createType(searchType, query.getFilter()));

	}

	protected ObjectQuery createQueryForMemberAction(QueryScope scope, boolean isFocus) {

		ObjectQuery query = null;
		switch (scope) {
			case SELECTED:
				List<ObjectType> objects = getMemberTable().getSelectedObjects();
				List<String> oids = new ArrayList<>();
				for (ObjectType object : objects) {
					if (satisfyConstraints(isFocus, object.getClass())) {
						oids.add(object.getOid());
					}
				}

				query = ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
				break;
			case ALL:
				query = createQueryForAll(isFocus);
				break;
			default:
				break;
		}
		return query;

	}

	private boolean satisfyConstraints(boolean isFocus, Class type) {
		if (isFocus && FocusType.class.isAssignableFrom(type)) {
			return true;
		}

		if (!isFocus && !FocusType.class.isAssignableFrom(type)) {
			return true;
		}

		return false;
	}

	private ObjectQuery createQueryForAll(boolean isFocus) {
		OrgType org = getModelObject();
		if (!isFocus) {

			PrismReferenceDefinition def = org.asPrismObject().getDefinition()
					.findReferenceDefinition(UserType.F_PARENT_ORG_REF);
			ObjectFilter orgFilter = RefFilter.createReferenceEqual(new ItemPath(ObjectType.F_PARENT_ORG_REF),
					def, ObjectTypeUtil.createObjectRef(org).asReferenceValue());
			TypeFilter typeFilter = TypeFilter.createType(FocusType.COMPLEX_TYPE, null);
			return ObjectQuery
					.createObjectQuery(AndFilter.createAnd(NotFilter.createNot(typeFilter), orgFilter));

		}

		if (isFocus) {
			PrismReferenceDefinition def = org.asPrismObject().getDefinition()
					.findReferenceDefinition(new ItemPath(OrgType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF));
			ObjectFilter orgASsignmentFilter = RefFilter.createReferenceEqual(
					new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), def,
					ObjectTypeUtil.createObjectRef(org).asReferenceValue());
			return ObjectQuery
					.createObjectQuery(TypeFilter.createType(FocusType.COMPLEX_TYPE, orgASsignmentFilter));
		}

		return null;

	}

	private ObjectQuery createQueryForRecompute() {

		List<ObjectType> objects = getMemberTable().getSelectedObjects();
		List<String> oids = new ArrayList<>();
		for (ObjectType object : objects) {
			oids.add(object.getOid());

		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	// private ObjectQuery createQueryForRecomputeAll() {
	// OrgType org = getTreePanel().getSelected().getValue();
	// PrismReferenceDefinition def = org.asPrismObject().getDefinition()
	// .findReferenceDefinition(UserType.F_PARENT_ORG_REF);
	// ObjectFilter orgFilter = RefFilter.createReferenceEqual(new
	// ItemPath(ObjectType.F_PARENT_ORG_REF),
	// def, ObjectTypeUtil.createObjectRef(org).asReferenceValue());
	//
	// return ObjectQuery.createObjectQuery(orgFilter);
	//
	// }

	
	protected ObjectDelta createMemberDelta(MemberOperation operation, QName type, QName relation)
			throws SchemaException {
		Class classType = qnameToClass(type);
		ObjectDelta delta = null;
		switch (operation) {
			case ADD:

				if (isFocus(type)) {

					delta = ObjectDelta.createModificationAddContainer(classType, "fakeOid",
							FocusType.F_ASSIGNMENT, getPageBase().getPrismContext(),
							createAssignmentToModify(type, relation));
				} else {
					delta = ObjectDelta.createModificationAddReference(classType, "fakeOid",
							ObjectType.F_PARENT_ORG_REF, getPageBase().getPrismContext(),
							createReference(relation).asReferenceValue());
				}
				break;

			case REMOVE:
				if (isFocus(type)) {
					delta = ObjectDelta.createModificationDeleteContainer(classType, "fakeOid",
							FocusType.F_ASSIGNMENT, getPageBase().getPrismContext(),
							createAssignmentToModify(FocusType.COMPLEX_TYPE, null));
				} else {
					delta = ObjectDelta.createModificationDeleteReference(classType, "fakeOid",
							ObjectType.F_PARENT_ORG_REF, getPageBase().getPrismContext(),
							createReference(null).asReferenceValue());
				}
				break;
			default:
				break;
		}
		return delta;

	}

	private boolean isFocus(QName type) {
		return FocusType.COMPLEX_TYPE.equals(type) || UserType.COMPLEX_TYPE.equals(type)
				|| RoleType.COMPLEX_TYPE.equals(type) || OrgType.COMPLEX_TYPE.equals(type)
				|| ServiceType.COMPLEX_TYPE.equals(type);
	}

}
