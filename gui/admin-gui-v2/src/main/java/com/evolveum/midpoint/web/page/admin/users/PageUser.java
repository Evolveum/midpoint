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

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.prism.AccountFooterPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
                return loadAcccountWrappers();
            }
        };

        initLayout();
    }

    private ObjectWrapper loadUserWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);

        PrismObject<UserType> user = null;
        StringValue userOid = getPageParameters().get(PARAM_USER_ID);
        if (userOid != null && StringUtils.isNotEmpty(userOid.toString())) {
            try {
            	Collection<PropertyPath> resolve = MiscUtil.createCollection(
    					new PropertyPath(UserType.F_ACCOUNT),
    					new PropertyPath(UserType.F_ACCOUNT, AccountShadowType.F_RESOURCE)
    				);

                // TODO: task
                user = getModelService().getObject(UserType.class, userOid.toString(), resolve, null, result);
            } catch (Exception ex) {
                //todo handle exception
                ex.printStackTrace();
            } finally {
                result.recomputeStatus();
                showResult(result);
            }
        } else {
            try {
                UserType userType = new UserType();
                getMidpointApplication().getPrismContext().adopt(userType);
                user = userType.asPrismObject();
            } catch (Exception ex) {
                //todo error handling
                ex.printStackTrace();
            }
        }

        if (user == null) {
            showResult(result);
            throw new RestartResponseException(PageUsers.class);
        }

        ObjectWrapper wrapper = new ObjectWrapper(null, null, user,
                com.evolveum.midpoint.web.component.prism.ContainerStatus.MODIFYING);
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
    }

    private void initAccounts(AccordionItem accounts) {
        ListView<ObjectWrapper> accountList = new ListView<ObjectWrapper>("accountList",
                accountsModel) {

            @Override
            protected void populateItem(final ListItem<ObjectWrapper> item) {
                PrismObjectPanel account = new PrismObjectPanel("account", item.getModel(),
                        new PackageResourceReference(PageUser.class, "Hdd.png")) {

                	@Override
        			protected Panel createOperationPanel(String id) {
        				return new AccountOperationButtons(id, item.getModel());
        			}
                	
                    @Override
                    public WebMarkupContainer createFooterPanel(String footerId, IModel<ObjectWrapper> model) {
                        //todo
                        return new AccountFooterPanel(footerId, new Model("some id"),
                                new Model<String>("probably active"));
                    }
                };
                item.add(account);
            }
        };

        accounts.getBodyContainer().add(accountList);
    }

    private List<ObjectWrapper> loadAcccountWrappers() {
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

            ObjectWrapper wrapper = new ObjectWrapper(account.getName(), resourceName,
                    account.asPrismObject(), ContainerStatus.MODIFYING);
            wrapper.setMinimalized(true);
            list.add(wrapper);
        }

        return list;
    }

    private IModel<List> createAssignmentsList() {
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
                onSaveError(target, form);
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
        
        AjaxLinkButton addTask = new AjaxLinkButton("addTask",
                createStringResource("pageUser.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO open popup with accounts
            	//cancelPerformed(target);
            }
        };
        mainForm.add(addTask);
        
        AjaxLinkButton enableTask = new AjaxLinkButton("enableTask",
                createStringResource("pageUser.button.enable")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO enable selected task
            }
        };
        mainForm.add(enableTask);
        
        AjaxLinkButton disableTask = new AjaxLinkButton("disableTask",
                createStringResource("pageUser.button.disable")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO disable selected task
            }
        };
        mainForm.add(disableTask);
        
        AjaxLinkButton deleteTask = new AjaxLinkButton("deleteTask",
                createStringResource("pageUser.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO delete selected task
            }
        };
        mainForm.add(deleteTask);
        
        AjaxLinkButton addResource = new AjaxLinkButton("addResource",
                createStringResource("pageUser.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO open popup with resources
            }
        };
        mainForm.add(addResource);
        
        AjaxLinkButton deleteResource = new AjaxLinkButton("deleteResource",
                createStringResource("pageUser.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO delete selected resources
            }
        };
        mainForm.add(deleteResource);
        
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
        ObjectDelta delta = userWrapper.getObjectDelta();
        if (delta == null || delta.isEmpty()) {

        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("User delta: {}", new Object[]{delta.debugDump(3)});
        }

        try {
            Task task = getTaskManager().createTaskInstance(OPERATION_SAVE_USER);

            switch (delta.getChangeType()) {
                case MODIFY:
                    getModelService().modifyObject(UserType.class, delta.getOid(), delta.getModifications(), task, result);
                    break;
                case ADD:

                    break;
                case DELETE:
                default:
                    error("Unsupported user delta change '" + delta.getChangeType() + "'."); //todo localize
            }
        } catch (Exception ex) {

        }
        //todo implement
    }

//    private void recalculatePerformed(AjaxRequestTarget target) {
//        //todo implement
//    }
//
//    private void refreshPerformed(AjaxRequestTarget target) {
//        //todo implement
//    }

    private void onSaveError(AjaxRequestTarget target, Form form) {
        //todo implement
    }
}
