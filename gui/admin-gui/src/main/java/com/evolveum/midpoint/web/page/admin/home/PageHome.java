/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageHome extends PageAdmin {
	private static final String DOT_CLASS = PageHome.class.getName() + ".";
	private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
	private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
	private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
	private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";

	private static final Trace LOGGER = TraceManager.getTrace(PageHome.class);

	private IModel<ObjectWrapper> userModel;
	private IModel<List<UserAccountDto>> accountsModel;
	private IModel<List<UserAssignmentDto>> assignmentsModel;
	PrincipalUser user;

	public PageHome() {
		this.user = SecurityUtils.getPrincipalUser();
		userModel = new LoadableModel<ObjectWrapper>(false) {

			@Override
			protected ObjectWrapper load() {
				return loadUserWrapper();
			}
		};
		accountsModel = new LoadableModel<List<UserAccountDto>>(false) {

			@Override
			protected List<UserAccountDto> load() {
				return loadAccountWrappers();
			}
		};
		assignmentsModel = new LoadableModel<List<UserAssignmentDto>>(false) {

			@Override
			protected List<UserAssignmentDto> load() {
				return loadAssignments();
			}
		};

		initLayout();
	}

	private void initLayout() {
		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);

		add(accordion);

		
		AccordionItem personal = new AccordionItem("personal",
		createStringResource("pageHome.personal"));
		personal.setOutputMarkupId(true);
		accordion.getBodyContainer().add(personal);
		initPersonal(personal);

		AccordionItem roles = new AccordionItem("roles", createStringResource("pageHome.roles"));
		roles.setOutputMarkupId(true);
		accordion.getBodyContainer().add(roles);

		AccordionItem assignments = new AccordionItem("assignments", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return createStringResource("pageHome.assignments", assignmentsModel.getObject().size())
						.getString();
			}
		});
		assignments.setOutputMarkupId(true);
		accordion.getBodyContainer().add(assignments);
		initAssignments(assignments);

		AccordionItem accounts = new AccordionItem("accounts", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return createStringResource("pageHome.accounts", accountsModel.getObject().size())
						.getString();
			}
		});
		accounts.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accounts);
		initAccounts(accounts);
	}

	private ObjectWrapper loadUserWrapper() {
		OperationResult result = new OperationResult(OPERATION_LOAD_USER);
		PrismObject<UserType> userObject = null;
		try {
			Task task = createSimpleTask(OPERATION_LOAD_USER);
            userObject = getModelService().getObject(UserType.class, user.getOid(), null, task, result);
			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
		}

		if (!result.isSuccess()) {
			showResultInSession(result);
		}

		if (userObject == null) {
			getSession().error(getString("pageHome.message.cantGetUser"));
			throw new RestartResponseException(PageHome.class);
		}

		ContainerStatus status = ContainerStatus.MODIFYING;
		ObjectWrapper wrapper = new ObjectWrapper(null, null, userObject, status);

		return wrapper;
	}

	private List<UserAccountDto> loadAccountWrappers() {
		List<UserAccountDto> list = new ArrayList<UserAccountDto>();

		ObjectWrapper user = userModel.getObject();
		PrismObject<UserType> prismUser = user.getObject();
		List<ObjectReferenceType> references = prismUser.asObjectable().getAccountRef();
		OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
		Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
		for (ObjectReferenceType reference : references) {
			OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
			try {
				Collection<PropertyPath> resolve = com.evolveum.midpoint.util.MiscUtil
						.createCollection(new PropertyPath(AccountShadowType.F_RESOURCE));

				PrismObject<AccountShadowType> account = getModelService().getObject(AccountShadowType.class,
						reference.getOid(), resolve, task, subResult);
				AccountShadowType accountType = account.asObjectable();

				OperationResultType fetchResult = accountType.getFetchResult();
				if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
					showResult(OperationResult.createOperationResult(fetchResult));
				}

				String resourceName = null;
				ResourceType resource = accountType.getResource();
				if (resource != null && StringUtils.isNotEmpty(resource.getName())) {
					resourceName = resource.getName();
				}
				ObjectWrapper wrapper = new ObjectWrapper(resourceName, accountType.getName(), account,
						ContainerStatus.MODIFYING);
				wrapper.setSelectable(true);
				wrapper.setMinimalized(true);
				list.add(new UserAccountDto(wrapper, UserDtoStatus.MODIFY));

				subResult.recomputeStatus();
			} catch (Exception ex) {
				subResult.recordFatalError("Couldn't load account.", ex);
			}
		}
		result.recomputeStatus();
		result.recordSuccessIfUnknown();

		if (!result.isSuccess()) {
			showResult(result);
		}

		return list;
	}

	private List<UserAssignmentDto> loadAssignments() {
		List<UserAssignmentDto> list = new ArrayList<UserAssignmentDto>();

		OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

		ObjectWrapper user = userModel.getObject();
		PrismObject<UserType> prismUser = user.getObject();
		List<AssignmentType> assignments = prismUser.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {
			String name = null;
			UserAssignmentDto.Type type = UserAssignmentDto.Type.OTHER;
			if (assignment.getTarget() != null) {
				ObjectType target = assignment.getTarget();
				name = target.getName();
				if (target instanceof RoleType) {
					type = UserAssignmentDto.Type.ROLE;
				}
			} else if (assignment.getTargetRef() != null) {
				ObjectReferenceType ref = assignment.getTargetRef();
				OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
				subResult.addParam("targetRef", ref.getOid());
				PrismObject target = null;
				try {
					Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
					target = getModelService().getObject(ObjectType.class, ref.getOid(), null, task,
							subResult);
					subResult.recordSuccess();
				} catch (Exception ex) {
					LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
					subResult.recordFatalError("Couldn't get assignment target ref.", ex);
				}

				if (target != null) {
					name = WebMiscUtil.getName(target);
				}

				if (target != null && RoleType.class.isAssignableFrom(target.getCompileTimeClass())) {
					type = UserAssignmentDto.Type.ROLE;
				}
			}

			list.add(new UserAssignmentDto(name, type, UserDtoStatus.MODIFY, assignment));
		}

		return list;
	}

	private void initPersonal(AccordionItem personal) {
		final PasswordType passwordType = user.getUser().getCredentials().getPassword();
		
		Label lastLoginDate = new Label("lastLoginDate", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if(passwordType.getPreviousSuccessfulLogin() == null){
					return PageHome.this.getString("pageHome.never");
				}
				return getSimpleDate(MiscUtil.asDate(passwordType.getPreviousSuccessfulLogin().getTimestamp()));
			}
		});
		personal.getBodyContainer().add(lastLoginDate);
		
		Label lastLoginFrom = new Label("lastLoginFrom", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
                if (passwordType.getPreviousSuccessfulLogin() == null) {
                    return PageHome.this.getString("pageHome.undefined");
                }
				return passwordType.getPreviousSuccessfulLogin().getFrom();
			}
		});
		personal.getBodyContainer().add(lastLoginFrom);
		
		Label lastFailDate = new Label("lastFailDate", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if(passwordType.getLastFailedLogin() == null){
					return PageHome.this.getString("pageHome.never");
				}
				return getSimpleDate(MiscUtil.asDate(passwordType.getLastFailedLogin().getTimestamp()));
			}
		});
		personal.getBodyContainer().add(lastFailDate);
		
		Label lastFailFrom = new Label("lastFailFrom", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if(passwordType.getLastFailedLogin() == null){
					return PageHome.this.getString("pageHome.undefined");
				}
				return passwordType.getLastFailedLogin().getFrom();
			}
		});
		personal.getBodyContainer().add(lastFailFrom);
		
		Label passwordExp = new Label("passwordExp", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if(user.getUser().getActivation() == null){
					return PageHome.this.getString("pageHome.undefined");
				}
				return getSimpleDate(MiscUtil.asDate(user.getUser().getActivation().getValidTo()));
			}
		});
		personal.getBodyContainer().add(passwordExp);
	}

	private void initAssignments(AccordionItem assignments) {
		List<IColumn<UserAssignmentDto>> columns = new ArrayList<IColumn<UserAssignmentDto>>();
		columns.add(new PropertyColumn(createStringResource("pageHome.assignment.name"), "name"));
		columns.add(new AbstractColumn<UserAssignmentDto>(createStringResource("pageHome.assignment.active")) {

			@Override
			public void populateItem(Item<ICellPopulator<UserAssignmentDto>> cellItem, String componentId,
					final IModel<UserAssignmentDto> rowModel) {
				cellItem.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

					@Override
					public Object getObject() {
						UserAssignmentDto dto = rowModel.getObject();
						ActivationType activation = dto.getActivation();
						if (activation == null) {
							return "-";
						}

						Boolean enabled = activation.isEnabled();
						String strEnabled;
						if (enabled != null) {
							if (enabled) {
								strEnabled = PageHome.this.getString("pageHome.assignment.activation.active");
							} else {
								strEnabled = PageHome.this
										.getString("pageHome.assignment.activation.inactive");
							}
						} else {
							strEnabled = PageHome.this.getString("pageHome.assignment.activation.undefined");
						}

						if (activation.getValidFrom() != null && activation.getValidTo() != null) {
							return PageHome.this.getString("pageHome.assignment.activation.enabledFromTo",
									strEnabled, MiscUtil.asDate(activation.getValidFrom()),
									MiscUtil.asDate(activation.getValidTo()));
						} else if (activation.getValidFrom() != null) {
							return PageHome.this.getString("pageHome.assignment.activation.enabledFrom",
									strEnabled, MiscUtil.asDate(activation.getValidFrom()));
						} else if (activation.getValidTo() != null) {
							return PageHome.this.getString("pageHome.assignment.activation.enabledTo",
									strEnabled, MiscUtil.asDate(activation.getValidTo()));
						}

						return "-";
					}
				}));
			}
		});

		ISortableDataProvider provider = new ListDataProvider(this, assignmentsModel);
		TablePanel assignmentTable = new TablePanel<UserAssignmentDto>("assignedAssignments", provider, columns);
		assignmentTable.setShowPaging(false);
		assignments.getBodyContainer().add(assignmentTable);
	}
	
	private void initAccounts(AccordionItem accounts){
		List<IColumn<UserAccountDto>> columns = new ArrayList<IColumn<UserAccountDto>>();
		columns.add(new PropertyColumn(createStringResource("pageHome.account.name"), "object.displayName"));
		
		ISortableDataProvider provider = new ListDataProvider(this, accountsModel);
		TablePanel accountsTable = new TablePanel<UserAccountDto>("assignedAccounts", provider, columns);
		accountsTable.setShowPaging(false);
		accounts.getBodyContainer().add(accountsTable);
	}
	
	private String getSimpleDate(Date date){
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
		return dateFormat.format(date);
	}
}
