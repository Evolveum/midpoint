package com.evolveum.midpoint.web.page.admin.users.component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
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
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class AbstractRoleMemberPanel<T extends AbstractRoleType> extends BasePanel<T>{

	protected enum QueryScope {
		SELECTED, ALL;
	}
	
	protected enum MemberOperation {
		ADD, REMOVE, RECOMPUTE;
	}
	
	protected static Map<Class, Class> objectDetailsMap;

	static {
		objectDetailsMap = new HashMap<>();
		objectDetailsMap.put(UserType.class, PageUser.class);
		objectDetailsMap.put(OrgType.class, PageOrgUnit.class);
		objectDetailsMap.put(RoleType.class, PageRole.class);
		objectDetailsMap.put(ServiceType.class, PageService.class);
		objectDetailsMap.put(ResourceType.class, PageResource.class);
		objectDetailsMap.put(TaskType.class, PageTaskEdit.class);
	}

	private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
	
	protected static final String ID_FORM = "form";
	protected static final String ID_CONTAINER_MANAGER = "managerContainer";
	protected static final String ID_CONTAINER_MEMBER = "memberContainer";
	protected static final String ID_CHILD_TABLE = "childUnitTable";
	protected static final String ID_MANAGER_TABLE = "managerTable";
	protected static final String ID_MEMBER_TABLE = "memberTable";
	
	public AbstractRoleMemberPanel(String id, IModel<T> model, PageBase parentPage) {
		super(id, model);
		setParent(parentPage);
		initLayout();
	}
	
	private void initLayout(){
		Form form = new Form(ID_FORM);
		form.setOutputMarkupId(true);
		add(form);

		initSearch(form);
		
		initMemberTable(form);
		
		initCustomLayout(form);
	}
	
	protected abstract void initCustomLayout(Form form);
	
	protected abstract void initSearch(Form form);

	private void initMemberTable(Form form){
		WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
		memberContainer.setOutputMarkupId(true);
		memberContainer.setOutputMarkupPlaceholderTag(true);
		form.add(memberContainer);

		MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<ObjectType>(
				ID_MEMBER_TABLE, ObjectType.class, null, getPageBase()) {

			@Override
			protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
				detailsPerformed(target, object);

			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				// TODO Auto-generated method stub

			}

			@Override
			protected List<IColumn<SelectableBean<ObjectType>, String>> createColumns() {
				return createMembersColumns();
			}

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				return new ArrayList<>();
			}

			@Override
			protected ObjectQuery createContentQuery() {
				ObjectQuery q = super.createContentQuery();

				ObjectQuery members = createMemberQuery();

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

//		WebMarkupContainer managerContainer = createManagerContainer();
//		form.addOrReplace(managerContainer);
	}
	
	
	private List<InlineMenuItem> createMembersHeaderInlineMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<>();
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.createMember"),
				false, new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						createFocusMemberPerformed(null, target);
					}
				}));

		

		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.addMembers"), false,
				new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						addMembers(null, target);
					}
				}));
		
		headerMenuItems.add(new InlineMenuItem());

		headerMenuItems
				.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.removeMembersSelected"),
						false, new HeaderMenuAction(this) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								removeMembersPerformed(QueryScope.SELECTED, target);
							}
						}));
		headerMenuItems.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.removeMembersAll"),
				false, new HeaderMenuAction(this) {

					@Override
					public void onClick(AjaxRequestTarget target) {
						removeMembersPerformed(QueryScope.ALL, target);
					}
				}));

		headerMenuItems
				.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersSelected"),
						false, new HeaderMenuAction(this) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								recomputeMembersPerformed(QueryScope.SELECTED, target);
							}
						}));
		headerMenuItems
				.add(new InlineMenuItem(createStringResource("TreeTablePanel.menu.recomputeMembersAll"),
						false, new HeaderMenuAction(this) {

							@Override
							public void onClick(AjaxRequestTarget target) {
								recomputeMembersPerformed(QueryScope.ALL, target);
							}
						}));

		return headerMenuItems;
	}
	
	
	protected void createFocusMemberPerformed(final QName relation, AjaxRequestTarget target) {

		ChooseFocusTypeDialogPanel chooseTypePopupContent = new ChooseFocusTypeDialogPanel(
				getPageBase().getMainPopupBodyId()) {

			protected void okPerformed(QName type, AjaxRequestTarget target) {
				initObjectForAdd(null, type, relation, target);

			};
		};

		getPageBase().showMainPopup(chooseTypePopupContent, new Model<String>("Choose type"), target, 300,
				200);

	}
	
	private void initObjectForAdd(ObjectReferenceType parentOrgRef, QName type, QName relation,
			AjaxRequestTarget target) {
		getPageBase().hideMainPopup(target);
		PrismContext prismContext = getPageBase().getPrismContext();
		PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		PrismObject obj = def.instantiate();
		if (parentOrgRef == null) {
			parentOrgRef = createReference(relation);
		}
		ObjectType objType = (ObjectType) obj.asObjectable();
		objType.getParentOrgRef().add(parentOrgRef);

		if (FocusType.class.isAssignableFrom(obj.getCompileTimeClass())) {
			AssignmentType assignment = new AssignmentType();
			assignment.setTargetRef(parentOrgRef);
			((FocusType) objType).getAssignment().add(assignment);
		}

		Class newObjectPageClass = objectDetailsMap.get(obj.getCompileTimeClass());

		Constructor constructor = null;
		try {
			constructor = newObjectPageClass.getConstructor(PrismObject.class);

		} catch (NoSuchMethodException | SecurityException e) {
			throw new SystemException("Unable to locate constructor (PrismObject) in " + newObjectPageClass
					+ ": " + e.getMessage(), e);
		}

		PageBase page;
		try {
			page = (PageBase) constructor.newInstance(obj);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new SystemException("Error instantiating " + newObjectPageClass + ": " + e.getMessage(), e);
		}

		setResponsePage(page);

	}

	protected void addMembers(final QName relation, AjaxRequestTarget target) {

		List<QName> types = new ArrayList<>(ObjectTypes.values().length);
		for (ObjectTypes t : ObjectTypes.values()) {
			types.add(t.getTypeQName());
		}
		FocusBrowserPanel<ObjectType> browser = new FocusBrowserPanel(getPageBase().getMainPopupBodyId(),
				UserType.class, types, true, getPageBase()) {

			@Override
			protected void addPerformed(AjaxRequestTarget target, QName type, List selected) {
				AbstractRoleMemberPanel.this.getPageBase().hideMainPopup(target);
				AbstractRoleMemberPanel.this.addMembersPerformed(type, relation, selected, target);

			}
		};
		browser.setOutputMarkupId(true);

		getPageBase().showMainPopup(browser, new Model<String>("Select members"), target, 900, 700);

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
	
	protected abstract void addMembersPerformed(QName type, QName relation, List selected, AjaxRequestTarget target);
	protected abstract void removeMembersPerformed(QueryScope scope, AjaxRequestTarget target);
	protected abstract void recomputeMembersPerformed(QueryScope scope, AjaxRequestTarget target);
	
	
	
	
	protected void executeMemberOperation(Task operationalTask, QName type, ObjectQuery memberQuery, ObjectDelta delta, String category, AjaxRequestTarget target) {
		
		OperationResult parentResult = operationalTask.getResult();

		try {
			TaskType task = WebComponentUtil.createSingleRecurenceTask(parentResult.getOperation(),
					type, memberQuery, delta, category,
					getPageBase());
			WebModelServiceUtils.runTask(task, operationalTask, parentResult, getPageBase());
		} catch (SchemaException e) {
			parentResult.recordFatalError(parentResult.getOperation(), e);
			LoggingUtils.logException(LOGGER, "Failed to execute operaton " + parentResult.getOperation(), e);
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
	
	protected void detailsPerformed(AjaxRequestTarget targer, ObjectType object) {
		Class responsePage = objectDetailsMap.get(object.getClass());
		if (responsePage == null) {
			error("Could not find proper response page");
			throw new RestartResponseException(getPageBase());
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
		setResponsePage(responsePage, parameters);
	}
	
	private List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
		List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

		IColumn<SelectableBean<ObjectType>, String> column = new AbstractColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.fullName.displayName")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				if (object instanceof UserType) {
					cellItem.add(new Label(componentId,
							WebComponentUtil.getOrigStringFromPoly(((UserType) object).getFullName())));
				} else if (AbstractRoleType.class.isAssignableFrom(object.getClass())) {
					cellItem.add(new Label(componentId, WebComponentUtil
							.getOrigStringFromPoly(((AbstractRoleType) object).getDisplayName())));
				}

			}

		};
		columns.add(column);

		column = new AbstractColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.identifier.description")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				if (object instanceof UserType) {
					cellItem.add(new Label(componentId, ((UserType) object).getEmailAddress()));
				} else if (AbstractRoleType.class.isAssignableFrom(object.getClass())) {
					cellItem.add(new Label(componentId, ((AbstractRoleType) object).getIdentifier()));
				} else {
					cellItem.add(new Label(componentId, object.getDescription()));
				}

			}

		};
		columns.add(column);

		columns.add(new InlineMenuHeaderColumn(createMembersHeaderInlineMenu()));
		return columns;
	}
	
	protected abstract ObjectQuery createMemberQuery();
	
//	protected abstract List<InlineMenuItem> createMembersHeaderInlineMenu();
	
	protected Class qnameToClass(QName type) {
		return getPageBase().getPrismContext().getSchemaRegistry().determineCompileTimeClass(type);
	}

}
