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

import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDtoType;
import org.apache.commons.lang.StringUtils;
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.home.dto.AdminHomeDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
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
	private IModel<AdminHomeDto> model;

	public PageHome() {
		model = new LoadableModel<AdminHomeDto>(true) {

			@Override
			protected AdminHomeDto load() {
                LOGGER.trace("Started home page data loading.");

				OperationResult result = new OperationResult(OPERATION_LOAD_USER);
				Task task = createSimpleTask(OPERATION_LOAD_USER);

				String userOid = SecurityUtils.getPrincipalUser().getOid();
				PrismObject<UserType> prismUser = null;
				try {
					prismUser = getModelService().getObject(UserType.class, userOid, null, task, result);
				} catch (Exception ex) {
					result.recordFatalError("pageHome.message.cantGetUser", ex);
                    LoggingUtils.logException(LOGGER, "Couldn't get user", ex);
				}

				if (prismUser == null) {
					result.recordFatalError("pageHome.message.cantGetUser");
					showResult(result);
				}
				AdminHomeDto dto = new AdminHomeDto();
                LOGGER.trace("Loading accounts.");
				dto.getAccounts().addAll(loadAccounts(prismUser));
                LOGGER.trace("Loading role assignments.");
				dto.getAssignments().addAll(loadRoleAssignments(prismUser));
                LOGGER.trace("Loading resource assignments.");
				dto.getResources().addAll(loadResourceAssignments(prismUser));

                LOGGER.trace("Finished home page data loading.");
				return dto;
			}
		};

		initLayout();
	}

	private void initLayout() {
		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);

		add(accordion);

		AccordionItem personal = new AccordionItem("personal", createStringResource("pageHome.personal"));
		personal.setOutputMarkupId(true);
		accordion.getBodyContainer().add(personal);
		initPersonal(personal);

		AccordionItem resources = new AccordionItem("resources", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return createStringResource("pageHome.resources", model.getObject().getResources().size())
						.getString();
			}
		});
		resources.setOutputMarkupId(true);
		accordion.getBodyContainer().add(resources);
		initResources(resources);

		AccordionItem roles = new AccordionItem("roles", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return createStringResource("pageHome.roles", model.getObject().getAssignments().size())
						.getString();
			}
		});
		roles.setOutputMarkupId(true);
		accordion.getBodyContainer().add(roles);
		initRoles(roles);

		AccordionItem accounts = new AccordionItem("myAccounts", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return createStringResource("pageHome.myAccounts", model.getObject().getAccounts().size())
						.getString();
			}
		});
		accounts.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accounts);
		initAccounts(accounts);
	}

	private List<SimpleAccountDto> loadAccounts(PrismObject<UserType> prismUser) {
		List<SimpleAccountDto> list = new ArrayList<SimpleAccountDto>();
		OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
		Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);

		List<ObjectReferenceType> references = prismUser.asObjectable().getAccountRef();
		for (ObjectReferenceType reference : references) {
			OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
			try {
				
				Collection<ObjectOperationOptions> options = 
					ObjectOperationOptions.createCollection(AccountShadowType.F_RESOURCE, ObjectOperationOption.RESOLVE);
				
				PrismObject<AccountShadowType> account = getModelService().getObject(AccountShadowType.class,
						reference.getOid(), options, task, subResult);
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
				list.add(new SimpleAccountDto(accountType.getName(), resourceName));

				subResult.recomputeStatus();
			} catch (Exception ex) {
				subResult.recordFatalError("Couldn't load account.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
			}
		}
		result.recomputeStatus();
		result.recordSuccessIfUnknown();

		if (!result.isSuccess()) {
			showResult(result);
		}

		return list;
	}

	private List<String> loadResourceAssignments(PrismObject<UserType> prismUser) {
		List<String> list = new ArrayList<String>();
		OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

		List<AssignmentType> assignments = prismUser.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {
			String name = null;
			AccountConstructionType accountConstruction = assignment.getAccountConstruction();
			if (accountConstruction == null) {
				continue;
			}
			if (accountConstruction.getResource() != null) {
				ResourceType resource = accountConstruction.getResource();
				name = resource.getName();
			} else if (accountConstruction.getResourceRef() != null) {
				ObjectReferenceType ref = accountConstruction.getResourceRef();
				OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
				subResult.addParam("resourceName", ref.getOid());
				PrismObject resource = null;
				try {
					Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
					resource = getModelService().getObject(ObjectType.class, ref.getOid(), null, task,
							subResult);
					subResult.recordSuccess();
				} catch (Exception ex) {
					LoggingUtils.logException(LOGGER, "Couldn't get assignment resource ref", ex);
					subResult.recordFatalError("Couldn't get assignment resource ref.", ex);
				}

				if (resource != null) {
					name = WebMiscUtil.getName(resource);
				}
			}

			if (!(list.contains(name))) {
				list.add(name);
			}
		}

		return list;
	}

	private List<SimpleAssignmentDto> loadRoleAssignments(PrismObject<UserType> prismUser) {
		List<SimpleAssignmentDto> list = new ArrayList<SimpleAssignmentDto>();

		OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);
		List<AssignmentType> assignments = prismUser.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {
			String name = null;
			UserAssignmentDtoType type = UserAssignmentDtoType.ACCOUNT_CONSTRUCTION;
			if (assignment.getTarget() != null) {
				ObjectType target = assignment.getTarget();
				name = target.getName();
                type = UserAssignmentDtoType.getType(target.getClass());
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
                    type = UserAssignmentDtoType.getType(target.getCompileTimeClass());
				}
			}
			list.add(new SimpleAssignmentDto(name, type, assignment.getActivation()));
		}

		return list;
	}

	private void initPersonal(AccordionItem personal) {
		final UserType user = SecurityUtils.getPrincipalUser().getUser();
		CredentialsType credentials = user.getCredentials();
		final PasswordType passwordType = credentials.getPassword();

		Label lastLoginDate = new Label("lastLoginDate", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (passwordType.getPreviousSuccessfulLogin() == null) {
					return PageHome.this.getString("pageHome.never");
				}
				return getSimpleDate(MiscUtil
						.asDate(passwordType.getPreviousSuccessfulLogin().getTimestamp()));
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
				if (passwordType.getLastFailedLogin() == null) {
					return PageHome.this.getString("pageHome.never");
				}
				return getSimpleDate(MiscUtil.asDate(passwordType.getLastFailedLogin().getTimestamp()));
			}
		});
		personal.getBodyContainer().add(lastFailDate);

		Label lastFailFrom = new Label("lastFailFrom", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (passwordType.getLastFailedLogin() == null) {
					return PageHome.this.getString("pageHome.undefined");
				}
				return passwordType.getLastFailedLogin().getFrom();
			}
		});
		personal.getBodyContainer().add(lastFailFrom);

		Label passwordExp = new Label("passwordExp", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				ActivationType activation = user.getActivation();
				if (activation == null || activation.getValidTo() == null) {
					return PageHome.this.getString("pageHome.undefined");
				}
				return getSimpleDate(MiscUtil.asDate(user.getActivation().getValidTo()));
			}
		});
		personal.getBodyContainer().add(passwordExp);
	}

	private void initRoles(AccordionItem assignments) {
		List<IColumn<SimpleAssignmentDto>> columns = new ArrayList<IColumn<SimpleAssignmentDto>>();
		columns.add(new PropertyColumn(createStringResource("pageHome.assignment.name"), "assignmentName"));
		columns.add(new AbstractColumn<SimpleAssignmentDto>(
				createStringResource("pageHome.assignment.active")) {

			@Override
			public void populateItem(Item<ICellPopulator<SimpleAssignmentDto>> cellItem, String componentId,
					final IModel<SimpleAssignmentDto> rowModel) {
				cellItem.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

					@Override
					public Object getObject() {
						SimpleAssignmentDto dto = rowModel.getObject();
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

		ISortableDataProvider provider = new ListDataProvider(this, new PropertyModel(model, "assignments"));
		TablePanel assignmentTable = new TablePanel<SimpleAssignmentDto>("assignedRoles", provider, columns);
		assignmentTable.setShowPaging(false);
		assignments.getBodyContainer().add(assignmentTable);
	}

	private void initAccounts(AccordionItem accounts) {
		List<IColumn<SimpleAccountDto>> columns = new ArrayList<IColumn<SimpleAccountDto>>();
		columns.add(new PropertyColumn(createStringResource("pageHome.account.name"), "accountName"));
		columns.add(new PropertyColumn(createStringResource("pageHome.account.resource"), "resourceName"));

		ISortableDataProvider provider = new ListDataProvider(this, new PropertyModel(model, "accounts"));
		TablePanel accountsTable = new TablePanel<SimpleAccountDto>("assignedAccounts", provider, columns);
		accountsTable.setShowPaging(false);
		accounts.getBodyContainer().add(accountsTable);
	}

	private String getSimpleDate(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss");
		return dateFormat.format(date);
	}

	private void initResources(AccordionItem resources) {
		List<IColumn<String>> columns = new ArrayList<IColumn<String>>();
		columns.add(new PropertyColumn(createStringResource("pageHome.resource.name"), "resources"));

		ISortableDataProvider provider = new ListDataProvider(this, new PropertyModel(model, "resources"));
		TablePanel assignedResources = new TablePanel<String>("assignedResources", provider, columns);
		assignedResources.setShowPaging(false);
		resources.getBodyContainer().add(assignedResources);
	}
}
