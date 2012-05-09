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

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.admin.users.dto.UserRoleDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

    public static final String PARAM_USER_ID = "userId";
    private static final String DOT_CLASS = PageUser.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
    private static final String OPERATION_SAVE_USER = DOT_CLASS + "saveUser";

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
                Collection<PropertyPath> resolve = com.evolveum.midpoint.util.MiscUtil.createCollection(
                        new PropertyPath(UserType.F_ACCOUNT),
                        new PropertyPath(UserType.F_ACCOUNT, AccountShadowType.F_RESOURCE)
                );

                TaskManager taskManager = getTaskManager();
                Task task = taskManager.createTaskInstance(OPERATION_LOAD_USER);

                StringValue userOid = getPageParameters().get(PARAM_USER_ID);
                user = getModelService().getObject(UserType.class, userOid.toString(), resolve, task, result);
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get user.", ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        if (user == null) {
            if (isEditingUser()) {
                getSession().error(getString("pageUser.message.cantEditUser"));
            } else {
                getSession().error(getString("pageUser.message.cantNewUser"));
            }

            if (!result.isSuccess()) {
                showResultInSession(result);
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

        PrismObjectPanel userForm = new PrismObjectPanel("userForm", userModel,
                new PackageResourceReference(PageUser.class, "User.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageUser.description");
            }
        };
        mainForm.add(userForm);

        Accordion accordion = new Accordion("accordion");
        accordion.setMultipleSelect(true);
        accordion.setOpenedPanel(0);
        mainForm.add(accordion);

        AccordionItem accounts = new AccordionItem("accounts", createStringResource("pageUser.accounts"));
        accounts.setOutputMarkupId(true);
        accordion.getBodyContainer().add(accounts);
        initAccounts(accounts);

        AccordionItem assignments = new AccordionItem("assignments", createStringResource("pageUser.assignments"));
        assignments.setOutputMarkupId(true);
        accordion.getBodyContainer().add(assignments);
        initAssignments(assignments);

        initButtons(mainForm);

        initResourceModal();
        initRoleModal();
        initConfirmationDialogs();
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
        ListView<UserAccountDto> accountList = new ListView<UserAccountDto>("accountList",
                accountsModel) {

            @Override
            protected void populateItem(final ListItem<UserAccountDto> item) {
                item.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        return !UserDtoStatus.DELETED.equals(item.getModelObject().getStatus());
                    }
                });

                PrismObjectPanel account = new PrismObjectPanel("account",
                        new PropertyModel<ObjectWrapper>(item.getModel(), "object"),
                        new PackageResourceReference(PageUser.class, "Hdd.png"), (Form) PageUser.this.get("mainForm")) {

//                	@Override
//        			protected Panel createOperationPanel(String id) {
//        				return new AccountOperationButtons(id, item.getModel());
//        			}
                };
                item.add(account);
            }
        };

        accounts.getBodyContainer().add(accountList);
    }

    private AccordionItem getAssignmentAccordionItem() {
        Accordion accordion = (Accordion) get("mainForm:accordion");
        return (AccordionItem) accordion.getBodyContainer().get("assignments");
    }

    private AccordionItem getAccountsAccordionItem() {
        Accordion accordion = (Accordion) get("mainForm:accordion");
        return (AccordionItem) accordion.getBodyContainer().get("accounts");
    }

    private List<UserAccountDto> loadAccountWrappers() {
        List<UserAccountDto> list = new ArrayList<UserAccountDto>();

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<AccountShadowType> accounts = prismUser.asObjectable().getAccount();
        for (AccountShadowType account : accounts) {
            String resourceName = null;
            ResourceType resource = account.getResource();
            if (resource != null && StringUtils.isNotEmpty(resource.getName())) {
                resourceName = resource.getName();
            }

            ObjectWrapper wrapper = new ObjectWrapper(resourceName, account.getName(),
                    account.asPrismObject(), ContainerStatus.MODIFYING);
            wrapper.setSelectable(true);
            wrapper.setMinimalized(true);
            list.add(new UserAccountDto(wrapper, UserDtoStatus.NOT_MODIFIED));
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
                    Task task = getTaskManager().createTaskInstance(OPERATION_LOAD_ASSIGNMENT);
                    target = getModelService().getObject(ObjectType.class, ref.getOid(), null, task, subResult);
                    subResult.recordSuccess();
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
                    subResult.recordFatalError("Couldn't get assignment target ref.", ex);
                }

                if (target != null) {
                    name = MiscUtil.getName(target);
                }

                if (target != null && RoleType.class.isAssignableFrom(target.getCompileTimeClass())) {
                    type = UserAssignmentDto.Type.ROLE;
                }
            }

            list.add(new UserAssignmentDto(name, assignment.getTargetRef(), assignment.getActivation(), type,
                    UserDtoStatus.NOT_MODIFIED));
        }

        return list;
    }

    private void initAssignments(AccordionItem assignments) {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new CheckBoxHeaderColumn());
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.type"), "type", "type"));
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.name"), "name", "name"));
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.active"), "activation", "activation"));

        ISortableDataProvider provider = new ListDataProvider(assignmentsModel);
        TablePanel assignmentTable = new TablePanel("assignmentTable", provider, columns);
        assignmentTable.setShowPaging(false);

        assignments.getBodyContainer().add(assignmentTable);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton save = new AjaxSubmitLinkButton("save",
                createStringResource("pageUser.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, form);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

//        AjaxLinkButton recalculate = new AjaxLinkButton("recalculate",
//                createStringResource("pageUser.button.recalculate")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                recalculatePerformed(target);
//            }
//        };
//        mainForm.add(recalculate);
//
//        AjaxLinkButton refresh = new AjaxLinkButton("refresh",
//                createStringResource("pageUser.button.refresh")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                refreshPerformed(target);
//            }
//        };
//        mainForm.add(refresh);

        AjaxLinkButton cancel = new AjaxLinkButton("cancel",
                createStringResource("pageUser.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);

        initAccountButtons(mainForm);
        initRoleButtons(mainForm);
    }

    private void initRoleButtons(Form mainForm) {
        AjaxLinkButton addRole = new AjaxLinkButton("addRole",
                createStringResource("pageUser.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showModalWindow(MODAL_ID_ROLE, target);
            }
        };
        mainForm.add(addRole);

        AjaxLinkButton deleteRole = new AjaxLinkButton("deleteRole",
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

        AjaxLinkButton deleteAccount = new AjaxLinkButton("deleteAccount",
                createStringResource("pageUser.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAccountPerformed(target);
            }
        };
        mainForm.add(deleteAccount);
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

        return modal;
    }

    private void initResourceModal() {
        ModalWindow window = createModalWindow(MODAL_ID_RESOURCE, createStringResource("pageUser.title.selectResource"));
        window.setContent(new ResourcesPopup(window.getContentId(), this) {

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
                addSelectedAccountPerformed(target, newResources);
            }
        });
        add(window);
    }

    private void initRoleModal() {
        ModalWindow window = createModalWindow(MODAL_ID_ROLE, createStringResource("pageUser.title.selectRole"));
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

    private void savePerformed(AjaxRequestTarget target, Form form) {
        LOGGER.debug("Saving user changes.");
        ObjectWrapper userWrapper = userModel.getObject();

        OperationResult result = new OperationResult(OPERATION_SAVE_USER);
        try {
            Task task = getTaskManager().createTaskInstance(OPERATION_SAVE_USER);
//            encryptCredentials(userWrapper.getObject(), true); //todo encryption

            ObjectDelta delta = userWrapper.getObjectDelta();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta: {}", new Object[]{delta.debugDump(3)});
            }

            switch (delta.getChangeType()) {
                case MODIFY:
                    getModelService().modifyObject(UserType.class, delta.getOid(), delta.getModifications(), task, result);
                    break;
                case ADD:
                    getModelService().addObject(delta.getObjectToAdd(), task, result);
                    break;
                case DELETE:
                default:
                    error(createStringResource("pageUser.message.unsupportedDeltaChange",
                            delta.getChangeType()).getString());
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't save user", ex);
        }

        showResult(result);
        target.add(getFeedbackPanel());

        PrismObjectPanel userForm = (PrismObjectPanel) get("mainForm:userForm");
        userForm.ajaxUpdateFeedback(target);
        //todo implement
    }

    private void encryptCredentials(PrismObject object, boolean encrypt) {
        MidPointApplication application = getMidpointApplication();
        Protector protector = application.getProtector();

        PrismContainer password = object.findContainer(new PropertyPath(SchemaConstantsGenerated.C_CREDENTIALS,
                CredentialsType.F_PASSWORD));
        if (password == null) {
            return;
        }
        PrismProperty protectedStringProperty = password.findProperty(PasswordType.F_PROTECTED_STRING);
        if (protectedStringProperty == null ||
                protectedStringProperty.getRealValue(ProtectedStringType.class) == null) {
            return;
        }

        ProtectedStringType string = (ProtectedStringType) protectedStringProperty.
                getRealValue(ProtectedStringType.class);

        try {
            if (encrypt) {
                protector.encrypt(string);
            } else {
                protector.decrypt(string);
            }
        } catch (EncryptionException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't encrypt protected string", ex);
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

//    private void recalculatePerformed(AjaxRequestTarget target) {
//    }
//
//    private void refreshPerformed(AjaxRequestTarget target) {
//    }

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
                shadow.setObjectClass(getDefaultAccountType(resource));

                getPrismContext().adopt(shadow);

                ObjectWrapper wrapper = new ObjectWrapper(resource.getName(), null, shadow.asPrismObject(),
                        ContainerStatus.ADDING);
                accountsModel.getObject().add(new UserAccountDto(wrapper, UserDtoStatus.ADDED));
            } catch (Exception ex) {
                error(getString("pageUser.message.couldntCreateAccount", resource.getName()));
            }
        }

        target.add(getFeedbackPanel());
        target.add(getAccountsAccordionItem());
    }

    private QName getDefaultAccountType(ResourceType resource) {
        SchemaHandlingType handling = resource.getSchemaHandling();
        if (handling == null) {
            return null;
        }

        List<ResourceAccountTypeDefinitionType> accounts = handling.getAccountType();
        for (ResourceAccountTypeDefinitionType account : accounts) {
            if (account.isDefault()) {
                return account.getObjectClass();
            }
        }

        return null;
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

                assignments.add(new UserAssignmentDto(role.getName(), targetRef, null, UserAssignmentDto.Type.ROLE,
                        UserDtoStatus.ADDED));
            } catch (Exception ex) {
                error(getString("pageUser.message.couldntAddRole", role.getName()));
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

    private void updateAccountActivation(AjaxRequestTarget target, List<UserAccountDto> accounts, boolean enabled) {
        if (accounts.isEmpty()) {
            warn(getString("pageUser.message.noAccountSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        for (UserAccountDto account : accounts) {
            ObjectWrapper wrapper = account.getObject();
            ContainerWrapper activation = wrapper.findContainerWrapper(
                    new PropertyPath(ResourceObjectShadowType.F_ACTIVATION));
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

    private void deleteAccountPerformed(AjaxRequestTarget target) {
        List<UserAccountDto> selected = getSelectedAccounts();
        if (selected.isEmpty()) {
            warn(getString("pageUser.message.noAccountSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        showModalWindow(MODAL_ID_CONFIRM_DELETE_ACCOUNT, target);
    }

    private void showModalWindow(String id, AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(id);
        window.show(target);
    }

    private void deleteAccountConfirmedPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
        Iterator<UserAccountDto> iterator = selected.iterator();
        while (iterator.hasNext()) {
            UserAccountDto account = iterator.next();
            if (UserDtoStatus.ADDED.equals(account.getStatus())) {
                iterator.remove();
            } else {
                account.setStatus(UserDtoStatus.DELETED);
            }
        }
        target.add(getAccountsAccordionItem());
    }

    private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target, List<UserAssignmentDto> selected) {
        Iterator<UserAssignmentDto> iterator = selected.iterator();
        while (iterator.hasNext()) {
            UserAssignmentDto assignment = iterator.next();
            if (UserDtoStatus.ADDED.equals(assignment.getStatus())) {
                iterator.remove();
            } else {
                assignment.setStatus(UserDtoStatus.DELETED);
            }
        }
        target.add(getAssignmentAccordionItem());
    }
}
