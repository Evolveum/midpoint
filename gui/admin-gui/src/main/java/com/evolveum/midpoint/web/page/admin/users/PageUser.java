/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ChangeType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.SourceType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.admin.users.dto.UserRoleDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

	public static final String PARAM_USER_ID = "userId";
	private static final String DOT_CLASS = PageUser.class.getName() + ".";
	private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
	private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
	private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
	private static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
	private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "modifyAccount";
	private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
	private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";

	private static final String MODAL_ID_RESOURCE = "resourcePopup";
	private static final String MODAL_ID_ROLE = "rolePopup";
	private static final String MODAL_ID_CONFIRM_DELETE_ACCOUNT = "confirmDeleteAccountPopup";
	private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";

	private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);
	private IModel<ObjectWrapper> userModel;
	private IModel<List<UserAccountDto>> accountsModel;
	private IModel<List<UserAssignmentDto>> assignmentsModel;

	public PageUser() {
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

	private ObjectWrapper loadUserWrapper() {
		OperationResult result = new OperationResult(OPERATION_LOAD_USER);
		PrismObject<UserType> user = null;
		try {
			if (!isEditingUser()) {
				UserType userType = new UserType();
				getMidpointApplication().getPrismContext().adopt(userType);
				user = userType.asPrismObject();
			} else {
				Task task = createSimpleTask(OPERATION_LOAD_USER);

				StringValue userOid = getPageParameters().get(PARAM_USER_ID);
				user = getModelService().getObject(UserType.class, userOid.toString(), null, task, result);
			}
			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
		}

		if (!result.isSuccess()) {
			showResultInSession(result);
		}

		if (user == null) {
			if (isEditingUser()) {
				getSession().error(getString("pageUser.message.cantEditUser"));
			} else {
				getSession().error(getString("pageUser.message.cantNewUser"));
			}
			throw new RestartResponseException(PageUsers.class);
		}

		ContainerStatus status = isEditingUser() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
		ObjectWrapper wrapper = new ObjectWrapper(null, null, user, status);
		wrapper.setShowEmpty(!isEditingUser());
		return wrapper;
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		PrismObjectPanel userForm = new PrismObjectPanel("userForm", userModel, new PackageResourceReference(
				ImgResources.class, ImgResources.USER_PRISM), mainForm) {

			@Override
			protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
				return createStringResource("pageUser.description");
			}
		};
		mainForm.add(userForm);

		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setExpanded(true);
		mainForm.add(accordion);

		AccordionItem accounts = new AccordionItem("accountsDeltas", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				int size = 0;
				for (UserAccountDto account : accountsModel.getObject()) {
					if (!UserDtoStatus.DELETE.equals(account.getStatus())) {
						size++;
					}
				}
				return getString("pageUser.accounts", size);
			}
		});
		accounts.setOutputMarkupId(true);
		accordion.getBodyContainer().add(accounts);
		initAccounts(accounts);

		AccordionItem assignments = new AccordionItem("assignmentsList", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				int size = 0;
				for (UserAssignmentDto account : assignmentsModel.getObject()) {
					if (!UserDtoStatus.DELETE.equals(account.getStatus())) {
						size++;
					}
				}
				return getString("pageUser.assignments", size);
			}
		});
		assignments.setOutputMarkupId(true);
		accordion.getBodyContainer().add(assignments);
		initAssignments(assignments);

		initButtons(mainForm);

		initResourceModal();
		initRoleModal();
		initConfirmationDialogs();
	}

	private Integer getSizeOfListObjects(IModel listObjects) {
		return null;
	}

	private void initConfirmationDialogs() {
		ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ACCOUNT,
				createStringResource("pageUser.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageUser.message.deleteAccountConfirm",
								getSelectedAccounts().size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAccountConfirmedPerformed(target, getSelectedAccounts());
			}
		};
		add(dialog);

		dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT,
				createStringResource("pageUser.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageUser.message.deleteAssignmentConfirm",
								getSelectedAssignments().size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAssignmentConfirmedPerformed(target, getSelectedAssignments());
			}
		};
		add(dialog);
	}

	private void initAccounts(AccordionItem accounts) {
		ListView<UserAccountDto> accountList = new ListView<UserAccountDto>("accountList", accountsModel) {

			@Override
			protected void populateItem(final ListItem<UserAccountDto> item) {

				PrismObjectPanel account = new PrismObjectPanel("account", new PropertyModel<ObjectWrapper>(
						item.getModel(), "object"), new PackageResourceReference(ImgResources.class,
						ImgResources.HDD_PRISM), (Form) PageUser.this.get("mainForm")) {

					@Override
					protected Panel createOperationPanel(String id) {
						return new AccountOperationButtons(id, new PropertyModel<ObjectWrapper>(
								item.getModel(), "object")) {

							@Override
							public void deletePerformed(AjaxRequestTarget target) {
								deleteAccountPerformed(target, item.getModel());
							}

							@Override
							public void linkPerformed(AjaxRequestTarget target) {
								unlinkAccountPerformed(target, item.getModel());
							}
						};
					}
				};
				item.add(account);
			}
		};

		accounts.getBodyContainer().add(accountList);
	}

	private AccordionItem getAssignmentAccordionItem() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		return (AccordionItem) accordion.getBodyContainer().get("assignmentsList");
	}

	private AccordionItem getAccountsAccordionItem() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		return (AccordionItem) accordion.getBodyContainer().get("accountsDeltas");
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
				Collection<ObjectOperationOptions> options = ObjectOperationOptions.createCollection(
						AccountShadowType.F_RESOURCE, ObjectOperationOption.RESOLVE);

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
				ObjectWrapper wrapper = new ObjectWrapper(resourceName, accountType.getName(), account,
						ContainerStatus.MODIFYING);
				wrapper.setSelectable(true);
				wrapper.setMinimalized(true);
				list.add(new UserAccountDto(wrapper, UserDtoStatus.MODIFY));

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

	private void initAssignments(AccordionItem assignments) {
		List<IColumn> columns = new ArrayList<IColumn>();
		columns.add(new CheckBoxHeaderColumn());
		columns.add(new IconColumn<UserAssignmentDto>(createStringResource("pageUser.assignment.type")) {

			@Override
			protected IModel<ResourceReference> createIconModel(final IModel<UserAssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<ResourceReference>() {

					@Override
					public ResourceReference getObject() {
						UserAssignmentDto dto = rowModel.getObject();
						switch (dto.getType()) {
							case ROLE:
								return new SharedResourceReference(ImgResources.class, ImgResources.USER_SUIT);
							case OTHER:
							default:
								return new SharedResourceReference(ImgResources.class, ImgResources.DRIVE);
						}
					}
				};
			}

			@Override
			protected IModel<AttributeModifier> createAttribute(final IModel<UserAssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<AttributeModifier>() {

					@Override
					public AttributeModifier getObject() {
						UserAssignmentDto dto = rowModel.getObject();

						if (UserDtoStatus.DELETE.equals(dto.getStatus())) {
							return new AttributeModifier("class", "deletedValue");
						}
						return new AttributeModifier("", "");
					}
				};
			}

		});
		columns.add(new PropertyColumn(createStringResource("pageUser.assignment.name"), "name", "name"));
		columns.add(new AbstractColumn<UserAssignmentDto>(createStringResource("pageUser.assignment.active")) {

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
								strEnabled = PageUser.this.getString("pageUser.assignment.activation.active");
							} else {
								strEnabled = PageUser.this
										.getString("pageUser.assignment.activation.inactive");
							}
						} else {
							strEnabled = PageUser.this.getString("pageUser.assignment.activation.undefined");
						}

						if (activation.getValidFrom() != null && activation.getValidTo() != null) {
							return PageUser.this.getString("pageUser.assignment.activation.enabledFromTo",
									strEnabled, MiscUtil.asDate(activation.getValidFrom()),
									MiscUtil.asDate(activation.getValidTo()));
						} else if (activation.getValidFrom() != null) {
							return PageUser.this.getString("pageUser.assignment.activation.enabledFrom",
									strEnabled, MiscUtil.asDate(activation.getValidFrom()));
						} else if (activation.getValidTo() != null) {
							return PageUser.this.getString("pageUser.assignment.activation.enabledTo",
									strEnabled, MiscUtil.asDate(activation.getValidTo()));
						}

						return "-";
					}
				}));
			}
		});

		ISortableDataProvider provider = new ListDataProvider(this, assignmentsModel);
		TablePanel assignmentTable = new TablePanel("assignmentTable", provider, columns);
		assignmentTable.setShowPaging(false);

		assignments.getBodyContainer().add(assignmentTable);
	}

	private void initButtons(Form mainForm) {
		AjaxSubmitLinkButton save = new AjaxSubmitLinkButton("save", ButtonType.POSITIVE,
				createStringResource("pageUser.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(save);

		AjaxSubmitLinkButton submit = new AjaxSubmitLinkButton("submit", ButtonType.POSITIVE,
				createStringResource("pageUser.button.submit")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				submitPerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(submit);

		// AjaxLinkButton recalculate = new AjaxLinkButton("recalculate",
		// createStringResource("pageUser.button.recalculate")) {
		//
		// @Override
		// public void onClick(AjaxRequestTarget target) {
		// recalculatePerformed(target);
		// }
		// };
		// mainForm.add(recalculate);
		//
		// AjaxLinkButton refresh = new AjaxLinkButton("refresh",
		// createStringResource("pageUser.button.refresh")) {
		//
		// @Override
		// public void onClick(AjaxRequestTarget target) {
		// refreshPerformed(target);
		// }
		// };
		// mainForm.add(refresh);

		AjaxLinkButton back = new AjaxLinkButton("back", createStringResource("pageUser.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(back);

		initAccountButtons(mainForm);
		initRoleButtons(mainForm);
	}

	private void initRoleButtons(Form mainForm) {
		AjaxLinkButton addRole = new AjaxLinkButton("addRole", createStringResource("pageUser.button.add")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				showModalWindow(MODAL_ID_ROLE, target);
			}
		};
		mainForm.add(addRole);

		AjaxLinkButton deleteRole = new AjaxLinkButton("deleteRole", ButtonType.NEGATIVE,
				createStringResource("pageUser.button.delete")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteAssignmentsPerformed(target);
			}
		};
		mainForm.add(deleteRole);
	}

	private void initAccountButtons(Form mainForm) {
		AjaxLinkButton addAccount = new AjaxLinkButton("addAccount",
				createStringResource("pageUser.button.add")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				showModalWindow(MODAL_ID_RESOURCE, target);
			}
		};
		mainForm.add(addAccount);

		AjaxLinkButton enableAccount = new AjaxLinkButton("enableAccount",
				createStringResource("pageUser.button.enable")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				updateAccountActivation(target, getSelectedAccounts(), true);
			}
		};
		mainForm.add(enableAccount);

		AjaxLinkButton disableAccount = new AjaxLinkButton("disableAccount",
				createStringResource("pageUser.button.disable")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				updateAccountActivation(target, getSelectedAccounts(), false);
			}
		};
		mainForm.add(disableAccount);

		AjaxLinkButton unlinkAccount = new AjaxLinkButton("unlinkAccount",
				createStringResource("pageUser.button.unlink")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				unlinkAccountPerformed(target, getSelectedAccounts());
			}
		};
		mainForm.add(unlinkAccount);

		AjaxLinkButton deleteAccount = new AjaxLinkButton("deleteAccount", ButtonType.NEGATIVE,
				createStringResource("pageUser.button.delete")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteAccountPerformed(target);
			}
		};
		mainForm.add(deleteAccount);

		AjaxLinkButton unlockAccount = new AjaxLinkButton("unlockAccount",
				createStringResource("pageUser.button.unlock")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				unlockAccountPerformed(target, getSelectedAccounts());
			}
		};
		mainForm.add(unlockAccount);
	}

	private ModalWindow createModalWindow(String id, IModel<String> title) {
		final ModalWindow modal = new ModalWindow(id);
		add(modal);

		modal.setResizable(false);
		modal.setTitle(title);
		modal.setCookieName(PageUser.class.getSimpleName() + ((int) (Math.random() * 100)));

		modal.setInitialWidth(1100);
		modal.setWidthUnit("px");

		modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

			@Override
			public boolean onCloseButtonClicked(AjaxRequestTarget target) {
				return true;
			}
		});

		modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

			@Override
			public void onClose(AjaxRequestTarget target) {
				modal.close(target);
			}
		});

		modal.add(new AbstractAjaxBehavior() {
			@Override
			public void onRequest() {
			}

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				response.renderOnDomReadyJavaScript("Wicket.Window.unloadConfirmation = false;");
			}
		});

		return modal;
	}

	private void initResourceModal() {
		ModalWindow window = createModalWindow(MODAL_ID_RESOURCE,
				createStringResource("pageUser.title.selectResource"));

		SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, accountsModel);
		window.setContent(new ResourcesPopup(window.getContentId(), provider) {

			@Override
			protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
				addSelectedAccountPerformed(target, newResources);
			}
		});
		add(window);
	}

	private void initRoleModal() {
		ModalWindow window = createModalWindow(MODAL_ID_ROLE,
				createStringResource("pageUser.title.selectRole"));
		window.setContent(new RolesPopup(window.getContentId(), this) {

			@Override
			protected void addPerformed(AjaxRequestTarget target, List<UserRoleDto> roles) {
				addSelectedRolePerformed(target, roles);
			}
		});
		add(window);
	}

	private boolean isEditingUser() {
		StringValue userOid = getPageParameters().get(PageUser.PARAM_USER_ID);
		return userOid != null && StringUtils.isNotEmpty(userOid.toString());
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		setResponsePage(PageUsers.class);
	}

	private void modifyAccounts(OperationResult result) {
		LOGGER.debug("Modifying existing accounts.");
		List<UserAccountDto> accounts = accountsModel.getObject();
		OperationResult subResult = null;
		for (UserAccountDto account : accounts) {
			try {
				ObjectWrapper accountWrapper = account.getObject();
				ObjectDelta delta = accountWrapper.getObjectDelta();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Account delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
				}

				if (!UserDtoStatus.MODIFY.equals(account.getStatus()) || delta.isEmpty()) {
					continue;
				}
				WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());

				subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);
				Task task = createSimpleTask(OPERATION_MODIFY_ACCOUNT);

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Modifying account:\n{}", new Object[] { delta.debugDump(3) });
				}
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), task, subResult);
				subResult.recomputeStatus();
			} catch (Exception ex) {
				if (subResult != null) {
					subResult.recomputeStatus();
					subResult.recordFatalError("Modify account failed.", ex);
				}
				LoggingUtils.logException(LOGGER, "Couldn't modify account", ex);
			}
		}
	}

	private ArrayList<PrismObject> getAccountsForSubmit(OperationResult result,
			Collection<ObjectDelta<? extends ObjectType>> deltas) {
		List<UserAccountDto> accounts = accountsModel.getObject();
		ArrayList<PrismObject> prismAccounts = new ArrayList<PrismObject>();
		for (UserAccountDto account : accounts) {
			prismAccounts.add(account.getObject().getObject());
		}
		return prismAccounts;
	}

	private void prepareUserForAdd(PrismObject<UserType> user) throws SchemaException {
		UserType userType = user.asObjectable();
		// handle added accounts
		List<UserAccountDto> accounts = accountsModel.getObject();
		for (UserAccountDto accDto : accounts) {
			if (!UserDtoStatus.ADD.equals(accDto.getStatus())) {
				warn(getString("pageUser.message.illegalAccountState", accDto.getStatus()));
				continue;
			}

			ObjectWrapper accountWrapper = accDto.getObject();
			ObjectDelta delta = accountWrapper.getObjectDelta();
			PrismObject<AccountShadowType> account = delta.getObjectToAdd();
			WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());

			userType.getAccount().add(account.asObjectable());
		}

		// handle added assignments
		List<UserAssignmentDto> assignments = assignmentsModel.getObject();
		for (UserAssignmentDto assDto : assignments) {
			if (!UserDtoStatus.ADD.equals(assDto.getStatus())) {
				warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
				continue;
			}

			userType.getAssignment().add(assDto.createAssignment());
		}
	}

	private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
			throws SchemaException {
		ReferenceDelta refDelta = new ReferenceDelta(refDef);

		List<UserAccountDto> accounts = accountsModel.getObject();
		for (UserAccountDto accDto : accounts) {
			ObjectWrapper accountWrapper = accDto.getObject();
			ObjectDelta delta = accountWrapper.getObjectDelta();
			PrismReferenceValue refValue = new PrismReferenceValue(null, SourceType.USER_ACTION, null);

			PrismObject<AccountShadowType> account;
			switch (accDto.getStatus()) {
				case ADD:
					account = delta.getObjectToAdd();
					WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());
					refValue.setObject(account);
					refDelta.addValueToAdd(refValue);
					break;
				case DELETE:
					account = accountWrapper.getObject();
					refValue.setObject(account);
					refDelta.addValueToDelete(refValue);
					break;
				case MODIFY:
					// nothing to do, account modifications were applied before
					continue;
				case UNLINK:
					refValue.setOid(delta.getOid());
					refValue.setTargetType(AccountShadowType.COMPLEX_TYPE);
					refDelta.addValueToDelete(refValue);
					break;
				default:
					warn(getString("pageUser.message.illegalAccountState", accDto.getStatus()));
			}
		}

		return refDelta;
	}

	private ContainerDelta prepareUserAssignmentsDeltaForModify(PrismContainerDefinition def)
			throws SchemaException {
		ContainerDelta assDelta = new ContainerDelta(new PropertyPath(), UserType.F_ASSIGNMENT, def);

		List<UserAssignmentDto> assignments = assignmentsModel.getObject();
		for (UserAssignmentDto assDto : assignments) {
			switch (assDto.getStatus()) {
				case ADD:
				case DELETE:
					AssignmentType assignment = assDto.createAssignment();
					PrismContainerValue value = assignment.asPrismContainerValue();
					value.applyDefinition(def, false);
					if (UserDtoStatus.ADD.equals(assDto.getStatus())) {
						assDelta.addValueToAdd(value);
					} else {
						assDelta.addValueToDelete(value.clone());
					}
					break;
				case MODIFY:
					// will be implemented later
					break;
				default:
					warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
			}
		}

		return assDelta;
	}

	private void prepareUserDeltaForModify(ObjectDelta<UserType> userDelta) throws SchemaException {
		// handle accounts
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition objectDefinition = registry
				.findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(UserType.F_ACCOUNT_REF);
		ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			userDelta.addModification(refDelta);
		}

		// handle assignments
		PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
		ContainerDelta assDelta = prepareUserAssignmentsDeltaForModify(def);
		if (!assDelta.isEmpty()) {
			userDelta.addModification(assDelta);
		}
	}

	private void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Submit user.");

		OperationResult result = new OperationResult(OPERATION_SEND_TO_SUBMIT);
		modifyAccounts(result);

		ObjectWrapper userWrapper = userModel.getObject();
		ModelContext changes = null;
		try {
			ObjectDelta delta = userWrapper.getObjectDelta();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("User delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
			}
			Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
			switch (userWrapper.getStatus()) {
				case ADDING:
					PrismObject<UserType> user = delta.getObjectToAdd();
					WebMiscUtil.encryptCredentials(user, true, getMidpointApplication());
					prepareUserForAdd(user);
					getPrismContext().adopt(user, UserType.class);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
					}

					getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), task, result);
					// deltaComponent = new ObjectDeltaComponent(user,
					// userWrapper.getObject().clone(), delta);
					// changes =
					// getModelInteractionService().previewChanges(delta,
					// result);
					// result.recordSuccess();
					break;
				case MODIFYING:
					WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
					prepareUserDeltaForModify(delta);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
					}

					if (!delta.isEmpty()) {
                        getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), task, result);
					} else {
						result.recordSuccessIfUnknown();
					}
					break;
				// delete state is where? wtf?? in next release add there delete
				// state as well as
				// support for add/delete containers (e.g. delete credentials)
				default:
					error(getString("pageUser.message.unsupportedState", userWrapper.getStatus()));
			}

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't submit user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't submit user", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
			target.add(getFeedbackPanel());
		} else {
			showResultInSession(result);
			setResponsePage(PageUsers.class);

		}
	}

	private void submitPerformed(AjaxRequestTarget target) {
		LOGGER.debug("Submit user.");

		OperationResult result = new OperationResult(OPERATION_SEND_TO_SUBMIT);
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		ArrayList<PrismObject> accountsBeforeModify = getAccountsForSubmit(result, deltas);

		ObjectWrapper userWrapper = userModel.getObject();
		ObjectDelta delta = null;
		ModelContext changes = null;
		try {
			delta = userWrapper.getObjectDelta();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("User delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
			}
			Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
			switch (userWrapper.getStatus()) {
				case ADDING:
					PrismContainer password = delta.getObjectToAdd().findContainer(
							SchemaConstants.PATH_PASSWORD);
					if (password == null) {
						result.recordFatalError(getString("pageUser.message.noPassword"));
						break;
					}
					PrismObject<UserType> user = delta.getObjectToAdd();
					WebMiscUtil.encryptCredentials(user, true, getMidpointApplication());
					prepareUserForAdd(user);
					getPrismContext().adopt(user, UserType.class);
					deltas.add(delta);
					changes = getModelInteractionService().previewChanges(deltas, result);
					result.recordSuccess();
					break;
				case MODIFYING:
					WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
					prepareUserDeltaForModify(delta);
					deltas.add(delta);
					changes = getModelInteractionService().previewChanges(deltas, result);
					result.recordSuccess();
					break;
					// TODO: add delete state. Add there delete state as well as
					// support for add/delete containers (e.g. delete credentials)
				
				default:
					error(getString("pageUser.message.unsupportedState", userWrapper.getStatus()));
			}

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't submit user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't submit user", ex);
		}
		if (result.isError()) {
			showResult(result);
			target.add(getFeedbackPanel());
		} else {
			PageSubmit pageSubmit = new PageSubmit(changes, deltas, delta, accountsBeforeModify);
			setResponsePage(pageSubmit);
		}
	}

	private List<UserAccountDto> getSelectedAccounts() {
		List<UserAccountDto> selected = new ArrayList<UserAccountDto>();

		List<UserAccountDto> all = accountsModel.getObject();
		for (UserAccountDto account : all) {
			if (account.getObject().isSelected()) {
				selected.add(account);
			}
		}

		return selected;
	}

	private List<UserAssignmentDto> getSelectedAssignments() {
		List<UserAssignmentDto> selected = new ArrayList<UserAssignmentDto>();

		List<UserAssignmentDto> all = assignmentsModel.getObject();
		for (UserAssignmentDto wrapper : all) {
			if (wrapper.isSelected()) {
				selected.add(wrapper);
			}
		}

		return selected;
	}

	private void addSelectedAccountPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
		ModalWindow window = (ModalWindow) get(MODAL_ID_RESOURCE);
		window.close(target);

		if (newResources.isEmpty()) {
			warn(getString("pageUser.message.noResourceSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		for (ResourceType resource : newResources) {
			try {
				AccountShadowType shadow = new AccountShadowType();
				shadow.setResource(resource);

				RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(
						resource.asPrismObject(), getPrismContext());
				QName objectClass = refinedSchema.getDefaultAccountDefinition().getObjectClassDefinition()
						.getTypeName();
				shadow.setObjectClass(objectClass);

				getPrismContext().adopt(shadow);

				ObjectWrapper wrapper = new ObjectWrapper(resource.getName(), null, shadow.asPrismObject(),
						ContainerStatus.ADDING);
				wrapper.setShowEmpty(true);
				wrapper.setMinimalized(false);
				accountsModel.getObject().add(new UserAccountDto(wrapper, UserDtoStatus.ADD));
				setResponsePage(getPage());
			} catch (Exception ex) {
				error(getString("pageUser.message.couldntCreateAccount", resource.getName(), ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't create account", ex);
			}
		}

		target.add(getFeedbackPanel());
		target.add(getAccountsAccordionItem());
	}

	private void addSelectedRolePerformed(AjaxRequestTarget target, List<UserRoleDto> newRoles) {
		ModalWindow window = (ModalWindow) get(MODAL_ID_ROLE);
		window.close(target);

		if (newRoles.isEmpty()) {
			warn(getString("pageUser.message.noRoleSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		List<UserAssignmentDto> assignments = assignmentsModel.getObject();
		for (UserRoleDto role : newRoles) {
			try {
				ObjectReferenceType targetRef = new ObjectReferenceType();
				targetRef.setOid(role.getOid());
				targetRef.setType(RoleType.COMPLEX_TYPE);

				AssignmentType assignment = new AssignmentType();
				assignment.setTargetRef(targetRef);

				assignments.add(new UserAssignmentDto(role.getName(), UserAssignmentDto.Type.ROLE,
						UserDtoStatus.ADD, assignment));
				setResponsePage(getPage());
			} catch (Exception ex) {
				error(getString("pageUser.message.couldntAddRole", role.getName(), ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't add role", ex);
			}
		}

		target.add(getFeedbackPanel());
		target.add(getAssignmentAccordionItem());
	}

	private void deleteAssignmentsPerformed(AjaxRequestTarget target) {
		List<UserAssignmentDto> selected = getSelectedAssignments();
		if (selected.isEmpty()) {
			warn(getString("pageUser.message.noAssignmentSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		showModalWindow(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT, target);
	}

	private void updateAccountActivation(AjaxRequestTarget target, List<UserAccountDto> accounts,
			boolean enabled) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (UserAccountDto account : accounts) {
			ObjectWrapper wrapper = account.getObject();
			ContainerWrapper activation = wrapper.findContainerWrapper(new PropertyPath(
					ResourceObjectShadowType.F_ACTIVATION));
			if (activation == null) {
				warn(getString("pageUser.message.noActivationFound", wrapper.getDisplayName()));
				continue;
			}

			PropertyWrapper enabledProperty = activation.findPropertyWrapper(ActivationType.F_ENABLED);
			if (enabledProperty.getValues().size() != 1) {
				warn(getString("pageUser.message.noEnabledPropertyFound", wrapper.getDisplayName()));
				continue;
			}
			ValueWrapper value = enabledProperty.getValues().get(0);
			value.getValue().setValue(enabled);

			wrapper.setSelected(false);
		}

		target.add(getAccountsAccordionItem());
		target.add(getFeedbackPanel());
	}

	private boolean isAnyAccountSelected(AjaxRequestTarget target) {
		List<UserAccountDto> selected = getSelectedAccounts();
		if (selected.isEmpty()) {
			warn(getString("pageUser.message.noAccountSelected"));
			target.add(getFeedbackPanel());
			return false;
		}

		return true;
	}

	private void deleteAccountPerformed(AjaxRequestTarget target) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		showModalWindow(MODAL_ID_CONFIRM_DELETE_ACCOUNT, target);
	}

	private void showModalWindow(String id, AjaxRequestTarget target) {
		ModalWindow window = (ModalWindow) get(id);
		window.show(target);
	}

	private void deleteAccountConfirmedPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
		List<UserAccountDto> accounts = accountsModel.getObject();
		for (UserAccountDto account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				accounts.remove(account);
			} else {
				account.setStatus(UserDtoStatus.DELETE);
			}
		}
		target.add(getAccountsAccordionItem());
	}

	private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target, List<UserAssignmentDto> selected) {
		List<UserAssignmentDto> assignments = assignmentsModel.getObject();
		for (UserAssignmentDto assignment : selected) {
			if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
				assignments.remove(assignment);
			} else {
				assignment.setStatus(UserDtoStatus.DELETE);
				assignment.setSelected(false);
			}
		}
		target.appendJavaScript("window.location.reload()");
		target.add(getAssignmentAccordionItem());
	}

	private void unlinkAccountPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (UserAccountDto account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				continue;
			}
			account.setStatus(UserDtoStatus.UNLINK);
		}
		target.add(getAccountsAccordionItem());
	}

	private void unlinkAccountPerformed(AjaxRequestTarget target, IModel<UserAccountDto> model) {
		UserAccountDto dto = model.getObject();
		if (UserDtoStatus.ADD.equals(dto.getStatus())) {
			return;
		}
		dto.setStatus(UserDtoStatus.UNLINK);

		target.add(getAccountsAccordionItem());
	}

	private void deleteAccountPerformed(AjaxRequestTarget target, IModel<UserAccountDto> model) {
		List<UserAccountDto> accounts = accountsModel.getObject();
		UserAccountDto account = model.getObject();

		if (UserDtoStatus.ADD.equals(account.getStatus())) {
			accounts.remove(account);
		} else {
			account.setStatus(UserDtoStatus.DELETE);
		}
		target.appendJavaScript("window.location.reload()");
		target.add(getAccountsAccordionItem());
	}

	private void unlockAccountPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (UserAccountDto account : selected) {
			// TODO: implement unlock
		}
	}
}
