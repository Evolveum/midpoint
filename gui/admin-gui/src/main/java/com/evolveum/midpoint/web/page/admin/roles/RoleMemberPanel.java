package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.dto.UserListItemDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.RoleMembersStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MisfireActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ThreadStopActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

public class RoleMemberPanel<T extends FocusType> extends SimplePanel<T> {

	private enum QueryScope {
		ALL, SELECTED, TO_ADD
	}

	private static String ID_OBJECT_TYPE = "type";
	private static String ID_TABLE = "table";
	private static String ID_TENANT = "tenant";
	private static String ID_PROJECT = "project";
	private static String ID_BASIC_SEARCH = "basicSearch";

	private static String MODAL_ID_MEMBER = "addMemberPopup";

	private IModel<RoleMemberSearchDto> searchModel;

	private PageBase pageBase;
	private String roleId;

	public RoleMemberPanel(String id, String roleId, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		if (roleId == null) {
			this.roleId = "empty";
		} else {
			this.roleId = roleId;
		}
		searchModel = new LoadableModel<RoleMemberSearchDto>(false) {

			@Override
			protected RoleMemberSearchDto load() {
				RoleMemberSearchDto searchDto = getRoleMemberSearch();
				if (searchDto == null) {
					searchDto = new RoleMemberSearchDto();
					getSession().getSessionStorage().getRoleMembers().setRoleMemberSearch(searchDto);
					return searchDto;
				}
				return searchDto;
			}
		};

		initCustomLayout();
	}

	private RoleMemberSearchDto getRoleMemberSearch() {
		return getSession().getSessionStorage().getRoleMembers().getRoleMemberSearch();
	}

	private PrismContext getPrismContext() {
		return pageBase.getPrismContext();
	}

	private Component getFeedbackPanel() {
		return pageBase.getFeedbackPanel();
	}

	private <V> DropDownChoice createDropDown(String id, String field, final List<V> values) {
		DropDownChoice listSelect = new DropDownChoice(id, new PropertyModel(searchModel, field),
				new AbstractReadOnlyModel<List<V>>() {

					@Override
					public List<V> getObject() {
						return values;
					}
				},

				new IChoiceRenderer<V>() {

					@Override
					public String getDisplayValue(V object) {
						return getStringValue(object);
					}

					@Override
					public String getIdValue(V object, int index) {
						return getValueForId(object);
					};
				});
		listSelect.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				TablePanel table = initTable();
				target.add(table);
				addOrReplace(table);
			}
		});

		return listSelect;
	}

	private <V> String getStringValue(V value) {
		if (value instanceof QName) {
			return ((QName) value).getLocalPart();
		} else if (value instanceof OrgType) {
			return ((OrgType) value).getName().getOrig();
		} else
			return "unknown value: " + value;
	}

	private <V> String getValueForId(V value) {
		if (value instanceof QName) {
			return ((QName) value).toString();
		} else if (value instanceof OrgType) {
			return ((OrgType) value).getName().getOrig();
		} else
			return "unknown value: " + value;

	}

	private void initCustomLayout() {

		BasicSearchPanel<RoleMemberSearchDto> basicSearch = new BasicSearchPanel<RoleMemberSearchDto>(
				ID_BASIC_SEARCH, searchModel) {

			@Override
			protected IModel<String> createSearchTextModel() {
				return new PropertyModel<>(searchModel, RoleMemberSearchDto.F_TEXT);
			}

			@Override
			protected void searchPerformed(AjaxRequestTarget target) {
				filterAccordingToNamePerformed(target);
			}

			@Override
			protected void clearSearchPerformed(AjaxRequestTarget target) {
				clearFilterAccordingToNamePerformed(target);
			}
		};
		add(basicSearch);

		DropDownChoice typeSelect = createDropDown(ID_OBJECT_TYPE, RoleMemberSearchDto.F_TYPE,
				createTypeList());
		add(typeSelect);

		DropDownChoice tenant = createDropDown(ID_TENANT, RoleMemberSearchDto.F_TENANT, createTenantList());
		add(tenant);

		DropDownChoice project = createDropDown(ID_PROJECT, RoleMemberSearchDto.F_PROJECT,
				createProjectList());
		add(project);

		addOrReplace(initTable());

		initDialog();
	}

	private void clearFilterAccordingToNamePerformed(AjaxRequestTarget target) {
		RoleMemberSearchDto dto = searchModel.getObject();
		dto.setText(null);

		TablePanel table = initTable();
		target.add(table);
		
		addOrReplace(table);
	}
	
	private void filterAccordingToNamePerformed(AjaxRequestTarget target){
		TablePanel table = initTable();
		target.add(table);
		addOrReplace(table);
	}

	private void initDialog() {

		UserBrowserDialog<T> dialog = new UserBrowserDialog<T>(MODAL_ID_MEMBER, getClassFromType()) {

			@Override
			public void addPerformed(AjaxRequestTarget target, List<T> selected) {
				super.addPerformed(target, selected);
				addMembers(selected, target);
				target.add(getFeedbackPanel());
			}

			@Override
			protected boolean isCheckBoxVisible() {
				return true;
			}
			
		
		};
		add(dialog);

	}
	
	//TODO: what about possible member list? we need something like distinct..
	private ObjectQuery createMembersQuery(){
		ObjectQuery q = createQuery(false, true);
		NotFilter filter = NotFilter.createNot(RefFilter.createReferenceEqual(FocusType.F_ROLE_MEMBERSHIP_REF, UserType.class, getPrismContext(), roleId));
		return ObjectQuery.createObjectQuery(AndFilter.createAnd(q.getFilter(), filter));
	}
	
	private List<OrgType> createTenantList() {
		ObjectQuery query;
		try {
			query = ObjectQuery.createObjectQuery(
					EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class, getPrismContext(), true));
			List<PrismObject<OrgType>> orgs = WebModelUtils.searchObjects(OrgType.class, query,
					new OperationResult("Tenant search"), pageBase);
			List<OrgType> orgTypes = new ArrayList<>();
			for (PrismObject<OrgType> org : orgs) {
				orgTypes.add(org.asObjectable());
			}

			return orgTypes;
		} catch (SchemaException e) {
			error(getString("pageUsers.message.queryError") + " " + e.getMessage());
			return null;
		}

	}

	private List<OrgType> createProjectList() {
		ObjectQuery query;
		try {
			query = ObjectQuery.createObjectQuery(OrFilter.createOr(
					EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class, getPrismContext(), true),
					EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class, getPrismContext(), null)));
			List<PrismObject<OrgType>> orgs = WebModelUtils.searchObjects(OrgType.class, query,
					new OperationResult("Tenant search"), pageBase);
			List<OrgType> orgTypes = new ArrayList<>();
			for (PrismObject<OrgType> org : orgs) {
				orgTypes.add(org.asObjectable());
			}

			return orgTypes;
		} catch (SchemaException e) {
			error(getString("pageUsers.message.queryError") + " " + e.getMessage());
			return null;
		}

	}

	private TablePanel initTable() {

		QName typeName = searchModel.getObject().getType();
		if (UserType.COMPLEX_TYPE.equals(typeName)) {
			return initUserTable();
		} else if (RoleType.COMPLEX_TYPE.equals(typeName)) {
			return initRoleTable();
		} else {
			return initOrgTable();
		}

		// add(table);
	}

	private Class getClassFromType() {
		QName typeName = searchModel.getObject().getType();
		return getPrismContext().getSchemaRegistry().getCompileTimeClass(typeName);
	}

	private TablePanel initRoleTable() {
		ObjectDataProvider provider = new ObjectDataProvider(RoleMemberPanel.this, RoleType.class) {

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				RoleMembersStorage storage = getSession().getSessionStorage().getRoleMembers();
				storage.setRolesPaging(paging);
			}
		};
		provider.setQuery(createQuery());

		List<IColumn<RoleType, String>> columns = initRoleColumns();
		TablePanel table = new TablePanel<>(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.TABLE_ROLES, 10);
		table.setOutputMarkupId(true);

		RoleMembersStorage storage = getSession().getSessionStorage().getRoleMembers();
		table.setCurrentPage(storage.getRolesPaging());
		getRoleMemberSearch().setType(RoleType.COMPLEX_TYPE);
		return table;

	}

	private TablePanel initOrgTable() {
		ObjectDataProvider provider = new ObjectDataProvider(RoleMemberPanel.this, OrgType.class) {

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				RoleMembersStorage storage = getSession().getSessionStorage().getRoleMembers();
				storage.setRolesPaging(paging);
			}
		};
		provider.setQuery(createQuery());

		List<IColumn<OrgType, String>> columns = initOrgColumns();
		TablePanel table = new TablePanel<>(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.TABLE_ROLES, 10);
		table.setOutputMarkupId(true);

		RoleMembersStorage storage = getSession().getSessionStorage().getRoleMembers();
		table.setCurrentPage(storage.getRolesPaging());
		getRoleMemberSearch().setType(OrgType.COMPLEX_TYPE);
		return table;

	}

	private TablePanel initUserTable() {
		ObjectDataProvider<UserListItemDto, UserType> provider = new ObjectDataProvider<UserListItemDto, UserType>(
				RoleMemberPanel.this, UserType.class) {

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				RoleMembersStorage storage = getSession().getSessionStorage().getRoleMembers();
				storage.setRolesPaging(paging);
			}

			@Override
			public UserListItemDto createDataObjectWrapper(PrismObject<UserType> obj) {
				return createRowDto(obj);
			}
		};
		provider.setQuery(createQuery());
		provider.setOptions(SelectorOptions.createCollection(GetOperationOptions.createResolveNames()));

		List<IColumn<UserListItemDto, String>> columns = initUserColumns();

		TablePanel table = new TablePanel(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.PAGE_USERS_PANEL, 10); // getItemsPerPage
		table.setOutputMarkupId(true);

		RoleMembersStorage storage = getSession().getSessionStorage().getRoleMembers();
		table.setCurrentPage(storage.getRolesPaging());
		getRoleMemberSearch().setType(UserType.COMPLEX_TYPE);

		return table;
	}

	private ObjectQuery createQuery() {
		return createQuery(true, false);
	}
	
	private void addFilter(ObjectFilter filter, List<ObjectFilter> conditions, boolean isNot){
		if (isNot){
			ObjectFilter notFilter = NotFilter.createNot(filter);
			conditions.add(notFilter);
		} else {
			conditions.add(filter);
		}
	}
	
	private ObjectQuery createQuery(boolean useNameFilter, boolean isNot) {
		ObjectQuery query;
		try {
			List<ObjectFilter> conditions = new ArrayList<>();
			PrismReferenceValue roleRef = new PrismReferenceValue();
			roleRef.setOid(roleId);
//			roleRef.setTargetType(RoleType.COMPLEX_TYPE);
			ObjectFilter roleFilter = RefFilter.createReferenceEqual(
					new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF), UserType.class,
					getPrismContext(), roleRef);
			addFilter(roleFilter, conditions, isNot);
			
			RoleMemberSearchDto dto = searchModel.getObject();
			if (dto.getTenant() != null) {
				PrismReferenceValue tenantRef = new PrismReferenceValue();
				tenantRef.setOid(dto.getTenant().getOid());
				tenantRef.setTargetType(OrgType.COMPLEX_TYPE);
				ObjectFilter tenantFilter = RefFilter.createReferenceEqual(
						new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF), UserType.class,
						getPrismContext(), tenantRef);
				addFilter(tenantFilter, conditions, isNot);
			}

			if (dto.getProject() != null) {
				PrismReferenceValue orgRef = new PrismReferenceValue();
				orgRef.setOid(dto.getProject().getOid());
				orgRef.setTargetType(OrgType.COMPLEX_TYPE);
				ObjectFilter projectFilter = RefFilter.createReferenceEqual(
						new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF), UserType.class,
						getPrismContext(), orgRef);
				addFilter(projectFilter, conditions, isNot);
			}

			if (StringUtils.isNotBlank(dto.getText()) && useNameFilter) {
				PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
	            String normalizedString = normalizer.normalize(dto.getText());
				ObjectFilter nameFilter = SubstringFilter.createSubstring(UserType.F_NAME, UserType.class,
						getPrismContext(), PolyStringNormMatchingRule.NAME, normalizedString);
				addFilter(nameFilter, conditions, isNot);
			}
			
			query = ObjectQuery.createObjectQuery(AndFilter.createAnd(conditions));
			
			return query;
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			error(getString("pageUsers.message.queryError") + " " + e.getMessage());
		}
		return null;

	}

	private List<IColumn<RoleType, String>> initRoleColumns() {
		List<IColumn<RoleType, String>> columns = new ArrayList<>();

		IColumn column = new CheckBoxHeaderColumn<RoleType>();
		columns.add(column);

		column = new LinkColumn<SelectableBean<RoleType>>(createStringResource("ObjectType.name"), "name",
				"value.name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
				RoleType role = rowModel.getObject().getValue();
				roleDetailsPerformed(target, role.getOid());
			}
		};
		columns.add(column);

		column = new PropertyColumn(createStringResource("OrgType.displayName"), "value.displayName");
		columns.add(column);

		column = new PropertyColumn(createStringResource("OrgType.identifier"), "value.identifier");
		columns.add(column);

		column = new PropertyColumn(createStringResource("ObjectType.description"), "value.description");
		columns.add(column);

		column = new InlineMenuHeaderColumn(initInlineMenu());
		columns.add(column);

		return columns;
	}

	private List<IColumn<OrgType, String>> initOrgColumns() {
		List<IColumn<OrgType, String>> columns = new ArrayList<>();

		IColumn column = new CheckBoxHeaderColumn<OrgType>();
		columns.add(column);

		column = new LinkColumn<SelectableBean<OrgType>>(createStringResource("ObjectType.name"), "name",
				"value.name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel) {
				OrgType role = rowModel.getObject().getValue();
				orgDetailsPerformed(target, role.getOid());
			}
		};
		columns.add(column);

		column = new PropertyColumn(createStringResource("OrgType.displayName"), "value.displayName");
		columns.add(column);

		column = new PropertyColumn(createStringResource("OrgType.identifier"), "value.identifier");
		columns.add(column);

		column = new PropertyColumn(createStringResource("ObjectType.description"), "value.description");
		columns.add(column);

		column = new InlineMenuHeaderColumn(initInlineMenu());
		columns.add(column);

		return columns;
	}

	private List<IColumn<UserListItemDto, String>> initUserColumns() {
		List<IColumn<UserListItemDto, String>> columns = new ArrayList<IColumn<UserListItemDto, String>>();

		columns.add(new CheckBoxHeaderColumn());
		columns.add(new IconColumn<UserListItemDto>(null) {

			@Override
			protected IModel<String> createIconModel(final IModel<UserListItemDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return rowModel.getObject().getIcon();
					}
				};
			}

			@Override
			protected IModel<String> createTitleModel(final IModel<UserListItemDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						String key = rowModel.getObject().getIconTitle();
						if (key == null) {
							return null;
						}
						return createStringResource(key).getString();
					}
				};
			}
		});

		IColumn column = new LinkColumn<UserListItemDto>(createStringResource("ObjectType.name"),
				UserType.F_NAME.getLocalPart(), UserListItemDto.F_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<UserListItemDto> rowModel) {
				userDetailsPerformed(target, rowModel.getObject().getOid());
			}
		};
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.givenName"),
				UserType.F_GIVEN_NAME.getLocalPart(), UserListItemDto.F_GIVEN_NAME);
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.familyName"),
				UserType.F_FAMILY_NAME.getLocalPart(), UserListItemDto.F_FAMILY_NAME);
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.fullName"),
				UserType.F_FULL_NAME.getLocalPart(), UserListItemDto.F_FULL_NAME);
		columns.add(column);

		column = new PropertyColumn(createStringResource("AssignmentType.tenant"), null,
				UserListItemDto.F_TENANT);
		columns.add(column);

		column = new PropertyColumn(createStringResource("AssignmentType.project"), null,
				UserListItemDto.F_PROJECT);
		columns.add(column);

		column = new PropertyColumn(createStringResource("UserType.emailAddress"), null,
				UserListItemDto.F_EMAIL);
		columns.add(column);

		column = new InlineMenuHeaderColumn(initInlineMenu());
		columns.add(column);

		return columns;
	}

	private List<InlineMenuItem> initInlineMenu() {
		List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
		headerMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.add"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						addMembersPerformed(target, QueryScope.TO_ADD);
					}
				}));

		headerMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.remove"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						removeMembersPerformed(target, QueryScope.SELECTED);
					}
				}));

		headerMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.recompute"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						recomputeMembersPerformed(target, QueryScope.SELECTED);
					}
				}));

		headerMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.removeAll"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						removeMembersPerformed(target, QueryScope.ALL);
					}
				}));

		headerMenuItems.add(new InlineMenuItem(createStringResource("roleMemberPanel.menu.recomputeAll"),
				true, new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						recomputeMembersPerformed(target, QueryScope.ALL);
					}
				}));

		headerMenuItems.add(new InlineMenuItem());

		return headerMenuItems;
	}

	private UserListItemDto createRowDto(PrismObject<UserType> obj) {
		UserType user = obj.asObjectable();

		UserListItemDto dto = new UserListItemDto(user.getOid(),
				WebMiscUtil.getOrigStringFromPoly(user.getName()),
				WebMiscUtil.getOrigStringFromPoly(user.getGivenName()),
				WebMiscUtil.getOrigStringFromPoly(user.getFamilyName()),
				WebMiscUtil.getOrigStringFromPoly(user.getFullName()), user.getEmailAddress());

		PrismContainer<AssignmentType> assignments = obj.findContainer(FocusType.F_ASSIGNMENT);
		StringBuilder tenantBuilder = new StringBuilder();
		StringBuilder orgBuilder = new StringBuilder();
		for (PrismContainerValue<AssignmentType> assignment : assignments.getValues()) {
			if (assignment != null && assignment.asContainerable() != null) {
				ObjectReferenceType ref = assignment.asContainerable().getTenantRef();
				if (ref != null) {
					tenantBuilder.append(WebMiscUtil.getOrigStringFromPoly(ref.getTargetName())).append("\n");
				}
				ObjectReferenceType orgRef = assignment.asContainerable().getOrgRef();
				if (orgRef != null) {
					orgBuilder.append(WebMiscUtil.getOrigStringFromPoly(orgRef.getTargetName())).append("\n");
				}
			}

		}
		dto.setTenant(tenantBuilder.toString());
		dto.setProject(orgBuilder.toString());

		dto.setCredentials(obj.findContainer(UserType.F_CREDENTIALS));
		dto.setIcon(WebMiscUtil.createUserIcon(obj));
		dto.setIconTitle(WebMiscUtil.createUserIconTitle(obj));

		return dto;
	}

	private List<UserListItemDto> getSelectedUsers(AjaxRequestTarget target, UserListItemDto selectedUser) {
		List<UserListItemDto> users;
		if (selectedUser != null) {
			users = new ArrayList<UserListItemDto>();
			users.add(selectedUser);
		} else {
			users = WebMiscUtil.getSelectedData(getTable());
			if (users.isEmpty()) {
				warn(getString("pageUsers.message.nothingSelected"));
				target.add(getFeedbackPanel());
			}
		}

		return users;
	}

	private TablePanel getTable() {
		return (TablePanel) get(createComponentPath(ID_TABLE));
	}

	private void addMembersPerformed(AjaxRequestTarget target, QueryScope scope) {
		UserBrowserDialog window = (UserBrowserDialog) get(MODAL_ID_MEMBER);
		window.setType(getClassFromType());
		window.show(target);
		// ObjectQuery query = createQueryForAdd(target);
		// addMembers(query, target);
	}

	private AssignmentType createAssignmentToModify() throws SchemaException {
		AssignmentType assignmentToModify = new AssignmentType();
		assignmentToModify.setTargetRef(ObjectTypeUtil.createObjectRef(roleId, ObjectTypes.ROLE));
		if (getRoleMemberSearch() != null && getRoleMemberSearch().getTenant() != null) {
			assignmentToModify.setTenantRef(ObjectTypeUtil
					.createObjectRef(getRoleMemberSearch().getTenant().getOid(), ObjectTypes.ORG));
		}
		if (getRoleMemberSearch() != null && getRoleMemberSearch().getProject() != null) {
			assignmentToModify.setOrgRef(ObjectTypeUtil
					.createObjectRef(getRoleMemberSearch().getProject().getOid(), ObjectTypes.ORG));
		}

		getPrismContext().adopt(assignmentToModify);

		return assignmentToModify;
	}

	private void addMembers(List<T> selected, AjaxRequestTarget target) {
		OperationResult parentResult = new OperationResult("Add members");

		try {
			ObjectDelta delta = ObjectDelta.createModificationAddContainer(UserType.class, "fakeOid",
					FocusType.F_ASSIGNMENT, getPrismContext(), createAssignmentToModify());

			execute("Add member(s)", getActionQuery(QueryScope.TO_ADD, selected), delta, parentResult,
					target);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			error(getString("pageUsers.message.nothingSelected") + e.getMessage());
			target.add(getFeedbackPanel());
		}

		parentResult.recordInProgress();
		pageBase.showResult(parentResult);
		target.add(getFeedbackPanel());

	}

	private void removeMembersPerformed(AjaxRequestTarget target, QueryScope scope) {
		OperationResult parentResult = new OperationResult("Remove members");
		try {
			ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(UserType.class, "fakeOid",
					FocusType.F_ASSIGNMENT, getPrismContext(), createAssignmentToModify());

			execute("Remove member(s)", getActionQuery(scope, null), delta, parentResult, target);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			error(getString("pageUsers.message.nothingSelected") + e.getMessage());
			target.add(getFeedbackPanel());
		}
		parentResult.recordInProgress();
		pageBase.showResult(parentResult);
		target.add(getFeedbackPanel());
	}

	private void recomputeMembersPerformed(AjaxRequestTarget target, QueryScope scope) {
		Task operationalTask = pageBase.createSimpleTask("Recompute all members");
		OperationResult parentResult = operationalTask.getResult();

		TaskType task = createTask("Recompute member(s)", getActionQuery(scope, null), null,
				TaskCategory.RECOMPUTATION, target);
		try {
			ObjectDelta<TaskType> delta = ObjectDelta.createAddDelta(task.asPrismObject());
			pageBase.getPrismContext().adopt(delta);
			pageBase.getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null,
					operationalTask, parentResult);
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			// TODO Auto-generated catch block
			error(getString("pageUsers.message.nothingSelected") + e.getMessage());
			target.add(getFeedbackPanel());
		}
		parentResult.recordInProgress();
		pageBase.showResult(parentResult);
		target.add(getFeedbackPanel());
	}

	private void execute(String taskName, ObjectQuery query, ObjectDelta deltaToExecute,
			OperationResult parentResult, AjaxRequestTarget target) {
		Task operationalTask = pageBase.createSimpleTask("Execute changes");

		TaskType task = createTask(taskName, query, deltaToExecute, TaskCategory.EXECUTE_CHANGES, target);
		try {
			ObjectDelta<TaskType> delta = ObjectDelta.createAddDelta(task.asPrismObject());
			pageBase.getPrismContext().adopt(delta);
			pageBase.getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null,
					operationalTask, parentResult);
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
			// TODO Auto-generated catch block
			error(getString("pageUsers.message.nothingSelected") + e.getMessage());
			target.add(getFeedbackPanel());
		}
		// pageBase.showResult(parentResult);
	}

	private ObjectQuery getActionQuery(QueryScope scope, List<T> selected) {
		switch (scope) {
			case ALL:
				return createQuery(false, false);
			case SELECTED:
				return createRecomputeQuery();
			case TO_ADD:
				return createQueryForAdd(selected);
		}

		return null;
	}

	private ObjectQuery createQueryForAdd(List<T> selected) {
		List<String> oids = new ArrayList<>();
		for (T selectable : selected) {
			oids.add(selectable.getOid());
		}

		return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
	}

	private ObjectQuery createRecomputeQuery() {
		Set<String> oids = getFocusOidToRecompute();
		ObjectQuery query = ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
		return query;
	}

	private Set<String> getFocusOidToRecompute() {
		List<Object> availableData = ((ObjectDataProvider) getTable().getDataTable().getDataProvider())
				.getAvailableData();
		Set<String> oids = new HashSet();
		for (Object d : availableData) {
			if (d instanceof SelectableBean) {
				if (((SelectableBean) d).isSelected()) {
					oids.add(((FocusType) ((SelectableBean) d).getValue()).getOid());
				}
			} else if (d instanceof UserListItemDto) {
				if (((UserListItemDto) d).isSelected()) {
					oids.add(((UserListItemDto) d).getOid());
				}
			} else {
				// throw new IllegalStateException("Nothing was selected");
				warn(getString("pageUsers.message.nothingSelected"));
				// target.add(getFeedbackPanel());
			}

		}
		return oids;
	}

	private TaskType createTask(String taskName, ObjectQuery query, ObjectDelta delta, String category,
			AjaxRequestTarget target) {
		TaskType task = new TaskType();

		MidPointPrincipal owner = SecurityUtils.getPrincipalUser();

		ObjectReferenceType ownerRef = new ObjectReferenceType();
		ownerRef.setOid(owner.getOid());
		ownerRef.setType(owner.getUser().COMPLEX_TYPE);
		task.setOwnerRef(ownerRef);

		task.setBinding(TaskBindingType.LOOSE);
		task.setCategory(category);
		task.setExecutionStatus(TaskExecutionStatusType.RUNNABLE);
		task.setRecurrence(TaskRecurrenceType.SINGLE);
		task.setThreadStopAction(ThreadStopActionType.RESTART);
		task.setHandlerUri(pageBase.getTaskService().getHandlerUriForCategory(category));
		ScheduleType schedule = new ScheduleType();
		schedule.setMisfireAction(MisfireActionType.EXECUTE_IMMEDIATELY);
		task.setSchedule(schedule);

		task.setName(WebMiscUtil.createPolyFromOrigString(taskName));

		try {
			PrismObject<TaskType> prismTask = task.asPrismObject();
			ItemPath path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
			PrismProperty objectQuery = prismTask.findOrCreateProperty(path);
			QueryType queryType = QueryJaxbConvertor.createQueryType(query, getPrismContext());
			objectQuery.addRealValue(queryType);

			path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
			PrismProperty objectType = prismTask.findOrCreateProperty(path);
			objectType.setRealValue(searchModel.getObject().getType());

			if (delta != null) {
				path = new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
				PrismProperty objectDelta = prismTask.findOrCreateProperty(path);
				objectDelta.setRealValue(DeltaConvertor.toObjectDeltaType(delta));
			}
		} catch (SchemaException e) {
			error(getString("pageUsers.message.nothingSelected"));
			target.add(getFeedbackPanel());
		}

		return task;
	}

	private void userDetailsPerformed(AjaxRequestTarget target, String oid) {
		setPreviousPage();
		setResponsePage(PageUser.class, getNextPageParams(oid));
	}

	private void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
		setPreviousPage();
		setResponsePage(PageRole.class, getNextPageParams(oid));
	}

	private void orgDetailsPerformed(AjaxRequestTarget target, String oid) {
		setPreviousPage();
		setResponsePage(PageOrgUnit.class, getNextPageParams(oid));
	}

	private void setPreviousPage() {
		getSession().getSessionStorage().setPreviousPage(PageRoles.class);
		// PageParameters previousParams = new PageParameters();
		// previousParams.add(OnePageParameterEncoder.PARAMETER, roleId);
		// getSession().getSessionStorage().setPreviousPageParams(previousParams);
	}

	private PageParameters getNextPageParams(String oid) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, oid);
		return parameters;
	}

	private List<QName> createTypeList() {
		List<QName> types = new ArrayList<>();
		types.add(UserType.COMPLEX_TYPE);
		types.add(RoleType.COMPLEX_TYPE);
		types.add(OrgType.COMPLEX_TYPE);
		return types;
	}

}
