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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

    public static final String PARAM_USER_ID = "userId";
    private static final String OPERATION_LOAD_USER = "pageUser.loadUser";
    private static final String OPERATION_SAVE_USER = "pageUser.saveUser";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);
    private IModel<ObjectWrapper> userModel;
    private IModel<List<ObjectWrapper>> accountsModel;
    private ModalWindow resourcesPopupWindow;
    private ModalWindow rolesPopupWindow;
    private ModalWindow confirmPopupWindow;

    public PageUser() {
        userModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadUserWrapper();
            }
        };
        accountsModel = new LoadableModel<List<ObjectWrapper>>(false) {

            @Override
            protected List<ObjectWrapper> load() {
                return loadAccountWrappers();
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
                Collection<PropertyPath> resolve = MiscUtil.createCollection(
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

        //todo remove and redirect to PageUsers and show result there...
        if (user == null) {
            try {
                UserType userType = new UserType();
                getMidpointApplication().getPrismContext().adopt(userType);
                user = userType.asPrismObject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
                new PackageResourceReference(PageUser.class, "User.png")) {

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
        accordion.getBodyContainer().add(accounts);
        initAccounts(accounts);

        AccordionItem assignments = new AccordionItem("assignments", createStringResource("pageUser.assignments"));
        accordion.getBodyContainer().add(assignments);
        initAssignments(assignments);

        initButtons(mainForm);
        resourcesPopupWindow = createResourcesWindow();
        rolesPopupWindow = createRolesWindow();
        confirmPopupWindow = createConfirmWindow();
    }

    private void initAccounts(AccordionItem accounts) {
        ListView<ObjectWrapper> accountList = new ListView<ObjectWrapper>("accountList",
                accountsModel) {

            @Override
            protected void populateItem(final ListItem<ObjectWrapper> item) {
                PrismObjectPanel account = new PrismObjectPanel("account", item.getModel(),
                        new PackageResourceReference(PageUser.class, "Hdd.png")) {

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

    private List<ObjectWrapper> loadAccountWrappers() {
        List<ObjectWrapper> list = new ArrayList<ObjectWrapper>();

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
            list.add(wrapper);
        }

        return list;
    }

    static IModel<List> createAssignmentsList() {
        return new LoadableModel<List>(false) {

            @Override
            protected List load() {
                return new ArrayList();
            }
        };
    }

    private void initAssignments(AccordionItem assignments) {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new CheckBoxHeaderColumn());
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.type"), "type", "type"));
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.name"), "name", "name"));
        columns.add(new PropertyColumn(createStringResource("pageUser.assignment.active"), "active", "active"));

        ISortableDataProvider provider = new ListDataProvider(createAssignmentsList());
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
        initResourceButtons(mainForm);
    }

    private void initRoleButtons(Form mainForm) {
        AjaxLinkButton addRole = new AjaxLinkButton("addRole",
                createStringResource("pageUser.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                rolesPopupWindow.show(target);
                //target.appendJavaScript("scrollToTop();");
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
                //accountsPopupWindow.show(target);
            }
        };
        mainForm.add(addAccount);

        AjaxLinkButton enableAccount = new AjaxLinkButton("enableAccount",
                createStringResource("pageUser.button.enable")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                enableAccountPerformed(target);
            }
        };
        mainForm.add(enableAccount);

        AjaxLinkButton disableAccount = new AjaxLinkButton("disableAccount",
                createStringResource("pageUser.button.disable")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                disableAccountPerformed(target);
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
    
    private void initResourceButtons(Form mainForm) {
        AjaxLinkButton addResource = new AjaxLinkButton("addResource",
                createStringResource("pageUser.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                resourcesPopupWindow.show(target);
                //target.appendJavaScript("scrollToTop();");
            }
        };
        mainForm.add(addResource);

        AjaxLinkButton deleteResource = new AjaxLinkButton("deleteResource",
                createStringResource("pageUser.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
            	deleteResourcePerformed(target);
            }
        };
        mainForm.add(deleteResource);
    }

    private ModalWindow createResourcesWindow() {
        final ModalWindow resourcesWindow;
        add(resourcesWindow = new ModalWindow("resourcePopup"));

        resourcesWindow.setContent(new ResourcesPopup(PageUser.this.getPageReference(), resourcesWindow.getContentId(),resourcesWindow, PageUser.this));
        resourcesWindow.setResizable(false);
        resourcesWindow.setTitle("Select resource");
        resourcesWindow.setCookieName("Resources popup window");

        resourcesWindow.setInitialWidth(1100);
        resourcesWindow.setWidthUnit("px");

        resourcesWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        resourcesWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                resourcesWindow.close(target);
            }
        });

        return resourcesWindow;
    }

    private ModalWindow createRolesWindow() {
        final ModalWindow rolesWindow;
        add(rolesWindow = new ModalWindow("rolePopup"));
        
        rolesWindow.setContent(new RolesPopup(PageUser.this.getPageReference(), rolesWindow.getContentId(), rolesWindow, PageUser.this));
        rolesWindow.setResizable(false);
        rolesWindow.setTitle("Select Role");
        rolesWindow.setCookieName("Roles popup window");

        rolesWindow.setInitialWidth(1100);
        rolesWindow.setWidthUnit("px");

        rolesWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        rolesWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
            	rolesWindow.close(target);
            }
        });

        return rolesWindow;
    }
    
    public ModalWindow createConfirmWindow(){
    	final ModalWindow confirmPopup;
        add(confirmPopup = new ModalWindow("confirmPopup"));

        confirmPopup.setCookieName("confirm popup window");
        
        confirmPopup.setContent(new ConfirmPopup(confirmPopup.getContentId() ,confirmPopup));
        confirmPopup.setResizable(false);
        confirmPopup.setInitialWidth(30);
        confirmPopup.setInitialHeight(15);
        confirmPopup.setWidthUnit("em");
        confirmPopup.setHeightUnit("em");

        confirmPopup.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        confirmPopup.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
        {
            public boolean onCloseButtonClicked(AjaxRequestTarget target)
            {
                target.appendJavaScript("alert('You can\\'t close this modal window using close button."
                    + " Use the link inside the window instead.');");
                return false;
            }
        });
        return confirmPopup;
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
                    error("Unsupported user delta change '" + delta.getChangeType() + "'."); //todo localize
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save user.", ex);
            ex.printStackTrace();
        }

        showResult(result);
        target.add(getFeedbackPanel());
        //todo implement
    }

    private List<ObjectWrapper> getSelectedAccounts() {
        List<ObjectWrapper> selected = new ArrayList<ObjectWrapper>();

        List<ObjectWrapper> all = accountsModel.getObject();
        for (ObjectWrapper wrapper : all) {
            if (wrapper.isSelected()) {
                selected.add(wrapper);
            }
        }

        return selected;
    }

    private List<ObjectWrapper> getSelectedAssignments() {
        List<ObjectWrapper> selected = new ArrayList<ObjectWrapper>();

        List<ObjectWrapper> all = new ArrayList<ObjectWrapper>();//todo get from model
        for (ObjectWrapper wrapper : all) {
            if (wrapper.isSelected()) {
                selected.add(wrapper);
            }
        }

        return selected;
    }

//    private void recalculatePerformed(AjaxRequestTarget target) {
//        //todo implement
//    }
//
//    private void refreshPerformed(AjaxRequestTarget target) {
//        //todo implement
//    }

    private void deleteAssignmentsPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void enableAccountPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void disableAccountPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void deleteAccountPerformed(AjaxRequestTarget target) {
        //todo implement
    }
    
    private void deleteResourcePerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
