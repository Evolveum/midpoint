/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.AssignablePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.ResourcesPopup;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_RETURN_PAGE = "returnPage";
    private static final String DOT_CLASS = PageUser.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
    private static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
    private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "modifyAccount";
    private static final String OPERATION_PREPARE_ACCOUNTS = DOT_CLASS + "getAccountsForSubmit";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE = DOT_CLASS + "save";

    private static final String MODAL_ID_RESOURCE = "resourcePopup";
    private static final String MODAL_ID_ASSIGNABLE = "assignablePopup";
    private static final String MODAL_ID_CONFIRM_DELETE_ACCOUNT = "confirmDeleteAccountPopup";
    private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ASSIGNMENT_EDITOR = "assignmentEditor";
    private static final String ID_ASSIGNMENT_LIST = "assignmentList";
    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_USER_FORM = "userForm";
    private static final String ID_ACCOUNTS_DELTAS = "accountsDeltas";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
    private static final String ID_ACCOUNT_LIST = "accountList";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_TASKS = "tasks";
    private static final String ID_ACCOUNT_MENU = "accountMenu";
    private static final String ID_ASSIGNMENT_MENU = "assignmentMenu";
    private static final String ID_ACCOUNT_CHECK_ALL = "accountCheckAll";
    private static final String ID_ASSIGNMENT_CHECK_ALL = "assignmentCheckAll";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);
    private LoadableModel<ObjectWrapper> userModel;
    private LoadableModel<List<UserAccountDto>> accountsModel;
    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
            = new LoadableModel<ExecuteChangeOptionsDto>(false) {

        @Override
        protected ExecuteChangeOptionsDto load() {
            return new ExecuteChangeOptionsDto();
        }
    };

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
        assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

            @Override
            protected List<AssignmentEditorDto> load() {
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
        ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails", null, user, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }

        wrapper.setShowEmpty(!isEditingUser());
        return wrapper;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        PrismObjectPanel userForm = new PrismObjectPanel(ID_USER_FORM, userModel, new PackageResourceReference(
                ImgResources.class, ImgResources.USER_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageUser.description");
            }
        };
        mainForm.add(userForm);

        WebMarkupContainer accounts = new WebMarkupContainer(ID_ACCOUNTS);
        accounts.setOutputMarkupId(true);
        mainForm.add(accounts);
        initAccounts(accounts);

        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        mainForm.add(assignments);
        initAssignments(assignments);

        WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
        tasks.setOutputMarkupId(true);
        mainForm.add(tasks);
        initTasks(tasks);

        initButtons(mainForm);

        initResourceModal();
        initAssignableModal();
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

        // TODO: uncoment later -> check for unsaved changes
        // dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_CANCEL,
        // createStringResource("pageUser.title.confirmCancel"), new
        // AbstractReadOnlyModel<String>() {
        //
        // @Override
        // public String getObject() {
        // return createStringResource("pageUser.message.cancelConfirm",
        // getSelectedAssignments().size()).getString();
        // }
        // }) {
        //
        // @Override
        // public void yesPerformed(AjaxRequestTarget target) {
        // close(target);
        // setResponsePage(PageUsers.class);
        // // deleteAssignmentConfirmedPerformed(target,
        // getSelectedAssignments());
        // }
        // };
        // add(dialog);
    }

    private List<InlineMenuItem> createAssignmentsMenu() {
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
        InlineMenuItem item = new InlineMenuItem(createStringResource("pageUser.menu.assignAccount"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAssignablePopup(target, ResourceType.class);
            }
        });
        items.add(item);
        item = new InlineMenuItem(createStringResource("pageUser.menu.assignRole"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAssignablePopup(target, RoleType.class);
            }
        });
        items.add(item);
        item = new InlineMenuItem(createStringResource("pageUser.menu.assignOrg"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAssignablePopup(target, OrgType.class);
            }
        });
        items.add(item);
        items.add(new InlineMenuItem());
        item = new InlineMenuItem(createStringResource("pageUser.menu.unassign"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAssignmentPerformed(target);
            }
        });
        items.add(item);

        return items;
    }

    private List<InlineMenuItem> createAccountsMenu() {
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
        InlineMenuItem item = new InlineMenuItem(createStringResource("pageUser.button.addAccount"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showModalWindow(MODAL_ID_RESOURCE, target);
            }
        });
        items.add(item);
        items.add(new InlineMenuItem());
        item = new InlineMenuItem(createStringResource("pageUser.button.enable"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateAccountActivation(target, getSelectedAccounts(), true);
            }
        });
        items.add(item);
        item = new InlineMenuItem(createStringResource("pageUser.button.disable"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateAccountActivation(target, getSelectedAccounts(), false);
            }
        });
        items.add(item);
        item = new InlineMenuItem(createStringResource("pageUser.button.unlink"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                unlinkAccountPerformed(target, getSelectedAccounts());
            }
        });
        items.add(item);
        item = new InlineMenuItem(createStringResource("pageUser.button.unlock"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                unlockAccountPerformed(target, getSelectedAccounts());
            }
        });
        items.add(item);
        items.add(new InlineMenuItem());
        item = new InlineMenuItem(createStringResource("pageUser.button.delete"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAccountPerformed(target);
            }
        });
        items.add(item);

        return items;
    }

    private void initAccounts(WebMarkupContainer accounts) {
        //todo implement check all functionality [lazyman]
        AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_ACCOUNT_CHECK_ALL, new Model()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        accounts.add(accountCheckAll);

        InlineMenu accountMenu = new InlineMenu(ID_ACCOUNT_MENU, new Model((Serializable) createAccountsMenu()));
        accounts.add(accountMenu);

        ListView<UserAccountDto> accountList = new ListView<UserAccountDto>(ID_ACCOUNT_LIST, accountsModel) {

            @Override
            protected void populateItem(final ListItem<UserAccountDto> item) {
                PrismObjectPanel account = new PrismObjectPanel("account", new PropertyModel<ObjectWrapper>(
                        item.getModel(), "object"), new PackageResourceReference(ImgResources.class,
                        ImgResources.HDD_PRISM), (Form) PageUser.this.get(ID_MAIN_FORM)) {

                    @Override
                    protected Component createHeader(String id, IModel<ObjectWrapper> model) {
                        return new CheckTableHeader(id, model) {

                            @Override
                            protected List<InlineMenuItem> createMenuItems() {
                                return createDefaultMenuItems(getModel());
                            }
                        };
                    }
                };
                item.add(account);
            }
        };

        accounts.add(accountList);
    }

    private List<UserAccountDto> loadAccountWrappers() {
        List<UserAccountDto> list = new ArrayList<UserAccountDto>();

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<ObjectReferenceType> references = prismUser.asObjectable().getLinkRef();
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
        for (ObjectReferenceType reference : references) {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
            try {
                Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                        ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

                if (reference.getOid() == null) {
                    continue;
                }

                PrismObject<ShadowType> account = getModelService().getObject(ShadowType.class,
                        reference.getOid(), options, task, subResult);
                ShadowType accountType = account.asObjectable();

                OperationResultType fetchResult = accountType.getFetchResult();
                if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
                    showResult(OperationResult.createOperationResult(fetchResult));
                }

                ResourceType resource = accountType.getResource();
                String resourceName = WebMiscUtil.getName(resource);

                ObjectWrapper wrapper = new ObjectWrapper(resourceName, WebMiscUtil.getOrigStringFromPoly(accountType
                        .getName()), account, ContainerStatus.MODIFYING);
                wrapper.setSelectable(true);
                wrapper.setMinimalized(true);
                if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
                    showResultInSession(wrapper.getResult());
                }
                list.add(new UserAccountDto(wrapper, UserDtoStatus.MODIFY));

                subResult.recomputeStatus();
            } catch (ObjectNotFoundException ex) {
                // this is fix for MID-854, full user/accounts/assignments reload if accountRef reference is broken
                // because consistency already fixed it.

                userModel.reset();
                accountsModel.reset();
                assignmentsModel.reset();

//                return list;
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
            } finally {
                subResult.computeStatus();
            }
        }
        result.computeStatus();

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        return list;
    }

    private List<AssignmentEditorDto> loadAssignments() {
        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<AssignmentType> assignments = prismUser.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            ObjectType targetObject = null;
            AssignmentEditorDtoType type = AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION;
            if (assignment.getTarget() != null) {
                // object assignment
                targetObject = assignment.getTarget();
                type = AssignmentEditorDtoType.getType(targetObject.getClass());
            } else if (assignment.getTargetRef() != null) {
                // object assignment through reference
                ObjectReferenceType ref = assignment.getTargetRef();
                PrismObject target = getReference(ref, result);

                if (target != null) {
                    targetObject = (ObjectType) target.asObjectable();
                    type = AssignmentEditorDtoType.getType(target.getCompileTimeClass());
                }
            } else if (assignment.getConstruction() != null) {
                // account assignment through account construction
                ConstructionType construction = assignment.getConstruction();
                if (construction.getResource() != null) {
                    targetObject = construction.getResource();
                } else if (construction.getResourceRef() != null) {
                    ObjectReferenceType ref = construction.getResourceRef();
                    PrismObject target = getReference(ref, result);
                    if (target != null) {
                        targetObject = (ObjectType) target.asObjectable();
                    }
                }
            }

            list.add(new AssignmentEditorDto(targetObject, type, UserDtoStatus.MODIFY, assignment));
        }

        Collections.sort(list);

        return list;
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
            Class type = ObjectType.class;
            if (ref.getType() != null) {
                type = getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
            }
            target = getModelService().getObject(type, ref.getOid(), null, task, subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
            subResult.recordFatalError("Couldn't get assignment target ref.", ex);
        }

        return target;
    }

    private void initAssignments(WebMarkupContainer assignments) {
        //todo implement check all functionality [lazyman]
        AjaxCheckBox assignmentCheckAll = new AjaxCheckBox(ID_ASSIGNMENT_CHECK_ALL, new Model()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        assignments.add(assignmentCheckAll);

        InlineMenu accountMenu = new InlineMenu(ID_ASSIGNMENT_MENU, new Model((Serializable) createAssignmentsMenu()));
        assignments.add(accountMenu);

        ListView<AssignmentEditorDto> assignmentList = new ListView<AssignmentEditorDto>(ID_ASSIGNMENT_LIST,
                assignmentsModel) {

            @Override
            protected void populateItem(final ListItem<AssignmentEditorDto> item) {
                AssignmentEditorPanel assignmentEditor = new AssignmentEditorPanel(ID_ASSIGNMENT_EDITOR,
                        item.getModel());
                item.add(assignmentEditor);
            }
        };
        assignments.add(assignmentList);
    }

    private void initTasks(WebMarkupContainer tasks) {
        List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();
        final TaskDtoProvider taskDtoProvider = new TaskDtoProvider(PageUser.this,
                TaskDtoProviderOptions.minimalOptions());
        taskDtoProvider.setQuery(createTaskQuery(null));
        TablePanel taskTable = new TablePanel<TaskDto>(ID_TASK_TABLE, taskDtoProvider, taskColumns) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                StringValue oidValue = getPageParameters().get(PARAM_USER_ID);

                taskDtoProvider.setQuery(createTaskQuery(oidValue != null ? oidValue.toString() : null));
            }
        };
        tasks.add(taskTable);

        tasks.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return taskDtoProvider.size() > 0;
            }
        });
    }

    private ObjectQuery createTaskQuery(String oid) {
        List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

        if (oid == null) {
            oid = "non-existent";      // TODO !!!!!!!!!!!!!!!!!!!!
        }
        try {
            filters.add(RefFilter.createReferenceEqual(TaskType.class, TaskType.F_OBJECT_REF, getPrismContext(), oid));
            filters.add(NotFilter.createNot(EqualsFilter.createEqual(TaskType.class, getPrismContext(), TaskType.F_EXECUTION_STATUS, TaskExecutionStatusType.CLOSED)));
            filters.add(EqualsFilter.createEqual(TaskType.class, getPrismContext(), TaskType.F_PARENT, null));
        } catch (SchemaException e) {
            throw new SystemException("Unexpected SchemaException when creating task filter", e);
        }

        return new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));
    }

    private List<IColumn<TaskDto, String>> initTaskColumns() {
        List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();

        columns.add(PageTasks.createTaskNameColumn(this, "pageUser.task.name"));
        columns.add(PageTasks.createTaskCategoryColumn(this, "pageUser.task.category"));
        columns.add(PageTasks.createTaskExecutionStatusColumn(this, "pageUser.task.execution"));
        columns.add(PageTasks.createTaskResultStatusColumn(this, "pageUser.task.status"));
        return columns;
    }


    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton("save",
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

        AjaxButton back = new AjaxButton("back", createStringResource("pageUser.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);

        mainForm.add(new ExecuteChangeOptionsPanel(true, ID_EXECUTE_OPTIONS, executeOptionsModel));
    }

    private void showAssignablePopup(AjaxRequestTarget target, Class<? extends ObjectType> type) {
        ModalWindow modal = (ModalWindow) get(MODAL_ID_ASSIGNABLE);
        AssignablePopupContent content = (AssignablePopupContent) modal.get(modal.getContentId());
        content.setType(type);
        showModalWindow(MODAL_ID_ASSIGNABLE, target);
    }

    private void initResourceModal() {
        ModalWindow window = createModalWindow(MODAL_ID_RESOURCE,
                createStringResource("pageUser.title.selectResource"), 1100, 520);

        SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, accountsModel);
        window.setContent(new ResourcesPopup(window.getContentId(), provider) {

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
                addSelectedAccountPerformed(target, newResources);
            }
        });

        add(window);
    }

    private void initAssignableModal() {
        ModalWindow window = createModalWindow(MODAL_ID_ASSIGNABLE,
                createStringResource("pageUser.title.selectAssignable"), 1100, 520);
        window.setContent(new AssignablePopupContent(window.getContentId()) {

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
                addSelectedAssignablePerformed(target, selected);
            }
        });
        add(window);
    }

    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(PageUser.PARAM_USER_ID);
        return userOid != null && StringUtils.isNotEmpty(userOid.toString());
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        // uncoment later -> check for changes to not allow leave the page when
        // some changes were made
        // try{
        // if (userModel.getObject().getOldDelta() != null &&
        // !userModel.getObject().getOldDelta().isEmpty() ||
        // userModel.getObject().getObjectDelta() != null &&
        // !userModel.getObject().getObjectDelta().isEmpty()){
        // showModalWindow(MODAL_ID_CONFIRM_CANCEL, target);
        // } else{
        StringValue orgReturn = getPageParameters().get(PARAM_RETURN_PAGE);
        if (PageOrgStruct.PARAM_ORG_RETURN.equals(orgReturn.toString())) {
            setResponsePage(PageOrgStruct.class);
        } else {
            setResponsePage(PageUsers.class);
        }

        // }
        // }catch(Exception ex){
        // LoggingUtils.logException(LOGGER, "Could not return to user list",
        // ex);
        // }
    }

    private List<ObjectDelta<? extends ObjectType>> modifyAccounts(OperationResult result) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

        List<UserAccountDto> accounts = accountsModel.getObject();
        OperationResult subResult = null;
        for (UserAccountDto account : accounts) {
            try {
                ObjectWrapper accountWrapper = account.getObject();
                ObjectDelta delta = accountWrapper.getObjectDelta();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
                }

                if (!UserDtoStatus.MODIFY.equals(account.getStatus())) {
                    continue;
                }

                if (delta.isEmpty() && (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty())) {
                    continue;
                }

                if (accountWrapper.getOldDelta() != null) {
                    delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());
                }

                //what is this???
//				subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);

                WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Modifying account:\n{}", new Object[]{delta.debugDump(3)});
                }

                deltas.add(delta);
//				subResult.recordSuccess();
            } catch (Exception ex) {
//				if (subResult != null) {
                result.recordFatalError("Couldn't compute account delta.", ex);
//				}
                LoggingUtils.logException(LOGGER, "Couldn't compute account delta", ex);
            }
        }

        return deltas;
    }

    private ArrayList<PrismObject> getAccountsForSubmit(OperationResult result,
                                                        Collection<ObjectDelta<? extends ObjectType>> deltas) {
        List<UserAccountDto> accounts = accountsModel.getObject();
        ArrayList<PrismObject> prismAccounts = new ArrayList<PrismObject>();
        OperationResult subResult = null;
        Task task = createSimpleTask(OPERATION_PREPARE_ACCOUNTS);
        for (UserAccountDto account : accounts) {
            prismAccounts.add(account.getObject().getObject());
            try {
                ObjectWrapper accountWrapper = account.getObject();
                ObjectDelta delta = accountWrapper.getObjectDelta();
                if (delta.isEmpty()) {
                    if (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty()) {
                        continue;
                    } else {
                        delta = accountWrapper.getOldDelta();
                    }
                } else {
                    if (accountWrapper.getOldDelta() != null && !accountWrapper.getOldDelta().isEmpty()) {
                        delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());

                    }
                }

                WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
                subResult = result.createSubresult(OPERATION_PREPARE_ACCOUNTS);
                deltas.add(delta);
                subResult.recordSuccess();
            } catch (Exception ex) {
                if (subResult != null) {
                    subResult.recordFatalError("Preparing account failed.", ex);
                }
                LoggingUtils.logException(LOGGER, "Couldn't prepare account for submit", ex);
            }
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
            PrismObject<ShadowType> account = delta.getObjectToAdd();
            WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());

            userType.getLink().add(account.asObjectable());
        }

        PrismObjectDefinition userDef = user.getDefinition();
        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        // handle added assignments
        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assDto : assignments) {
            if (!UserDtoStatus.ADD.equals(assDto.getStatus())) {
                warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
                continue;
            }

            AssignmentType assignment = new AssignmentType();
            PrismContainerValue value = assDto.getNewValue();
            assignment.setupContainerValue(value);
            value.applyDefinition(assignmentDef, false);
            userType.getAssignment().add(assignment.clone());

            // todo remove this block [lazyman] after model is updated - it has
            // to remove resource from accountConstruction
            removeResourceFromAccConstruction(assignment);
        }
    }

    /**
     * remove this method after model is updated - it has to remove resource
     * from accountConstruction
     */
    @Deprecated
    private void removeResourceFromAccConstruction(AssignmentType assignment) {
        ConstructionType accConstruction = assignment.getConstruction();
        if (accConstruction == null || accConstruction.getResource() == null) {
            return;
        }

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(assignment.getConstruction().getResource().getOid());
        ref.setType(ResourceType.COMPLEX_TYPE);
        assignment.getConstruction().setResourceRef(ref);
        assignment.getConstruction().setResource(null);
    }

    private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef) throws SchemaException {
        ReferenceDelta refDelta = new ReferenceDelta(refDef);

        List<UserAccountDto> accounts = accountsModel.getObject();
        for (UserAccountDto accDto : accounts) {
            ObjectWrapper accountWrapper = accDto.getObject();
            ObjectDelta delta = accountWrapper.getObjectDelta();
            PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

            PrismObject<ShadowType> account;
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
                    refValue.setTargetType(ShadowType.COMPLEX_TYPE);
                    refDelta.addValueToDelete(refValue);
                    break;
                default:
                    warn(getString("pageUser.message.illegalAccountState", accDto.getStatus()));
            }
        }

        return refDelta;
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> userDelta, PrismContainerDefinition def)
            throws SchemaException {
        ContainerDelta assDelta = new ContainerDelta(new ItemPath(), UserType.F_ASSIGNMENT, def);

        PrismObject<UserType> user = userModel.getObject().getObject();
        PrismObjectDefinition userDef = user.getDefinition();
        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue newValue = assDto.getNewValue();
            switch (assDto.getStatus()) {
                case ADD:
                case DELETE:
                    newValue.applyDefinition(assignmentDef, false);
                    if (UserDtoStatus.ADD.equals(assDto.getStatus())) {
                        assDelta.addValueToAdd(newValue.clone());
                    } else {
                        assDelta.addValueToDelete(newValue.clone());
                    }
                    break;
                case MODIFY:
                    if (!assDto.isModified()) {
                        LOGGER.trace("Assignment '{}' not modified.", new Object[]{assDto.getName()});
                        continue;
                    }

                    handleModifyAssignmentDelta(assDto, assignmentDef, newValue, userDelta);
                    break;
                default:
                    warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
            }
        }

        if (!assDelta.isEmpty()) {
            userDelta.addModification(assDelta);
        }

        // todo remove this block [lazyman] after model is updated - it has to
        // remove resource from accountConstruction
        Collection<PrismContainerValue> values = assDelta.getValues(PrismContainerValue.class);
        for (PrismContainerValue value : values) {
            AssignmentType ass = new AssignmentType();
            ass.setupContainerValue(value);
            removeResourceFromAccConstruction(ass);
        }

        return assDelta;
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto, PrismContainerDefinition assignmentDef,
                                             PrismContainerValue newValue, ObjectDelta<UserType> userDelta) throws SchemaException {
        LOGGER.debug("Handling modified assignment '{}', computing delta.", new Object[]{assDto.getName()});

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

        for (ItemDelta delta : deltas) {
            ItemPath deltaPath = delta.getPath().rest();
            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

            delta.setParentPath(joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
            delta.applyDefinition(deltaDef);

            userDelta.addModification(delta);
        }
    }

    private ItemPath joinPath(ItemPath path, ItemPath deltaPath) {
        List<ItemPathSegment> newPath = new ArrayList<ItemPathSegment>();

        ItemPathSegment firstDeltaSegment = deltaPath != null ? deltaPath.first() : null;
        if (path != null) {
            for (ItemPathSegment seg : path.getSegments()) {
                if (seg.equals(firstDeltaSegment)) {
                    break;
                }
                newPath.add(seg);
            }
        }
        if (deltaPath != null) {
            newPath.addAll(deltaPath.getSegments());
        }

        return new ItemPath(newPath);
    }

    private void prepareUserDeltaForModify(ObjectDelta<UserType> userDelta) throws SchemaException {
        // handle accounts
        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        PrismObjectDefinition objectDefinition = registry.findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(UserType.F_LINK_REF);
        ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
        if (!refDelta.isEmpty()) {
            userDelta.addModification(refDelta);
        }

        // handle assignments
        PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
        handleAssignmentDeltas(userDelta, def);
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Save user.");

        OperationResult result = new OperationResult(OPERATION_SAVE);
        ObjectWrapper userWrapper = userModel.getObject();
        // todo: improve, delta variable is quickfix for MID-1006
        // redirecting to user list page everytime user is created in repository
        // during user add in gui,
        // and we're not taking care about account/assignment create errors
        // (error message is still displayed)
        ObjectDelta delta;

        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
        ModelExecuteOptions options = executeOptions.createOptions();
        LOGGER.debug("Using options {}.", new Object[]{executeOptions});
        // try {

        try {
            delta = userWrapper.getObjectDelta();
            if (userWrapper.getOldDelta() != null) {
                delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
            }
            delta.setPrismContext(getPrismContext());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
            LoggingUtils.logException(LOGGER, getString("pageUser.message.cantCreateUser"), ex);
            showResult(result);
            return;
        }

        switch (userWrapper.getStatus()) {
            case ADDING:
                try {
                    PrismObject<UserType> user = delta.getObjectToAdd();
                    WebMiscUtil.encryptCredentials(user, true, getMidpointApplication());
                    prepareUserForAdd(user);
                    getPrismContext().adopt(user, UserType.class);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before add user:\n{}", new Object[]{delta.debugDump(3)});
                    }

                    if (!delta.isEmpty()) {
                        getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, result);
                    } else {
                        result.recordSuccess();
                    }
                } catch (Exception ex) {
                    result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
                    LoggingUtils.logException(LOGGER, getString("pageUser.message.cantCreateUser"), ex);
                }
                break;

            case MODIFYING:
                try {
                    WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
                    prepareUserDeltaForModify(delta);

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before modify user:\n{}", new Object[]{delta.debugDump(3)});
                    }

                    List<ObjectDelta<? extends ObjectType>> accountDeltas = modifyAccounts(result);
                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

                    if (!delta.isEmpty()) {
                        deltas.add(delta);
                    }
                    for (ObjectDelta accDelta : accountDeltas) {
                        if (!accDelta.isEmpty()) {
                            accDelta.setPrismContext(getPrismContext());
                            deltas.add(accDelta);
                        }
                    }

                    if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
                        ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class,
                                userWrapper.getObject().getOid(), getPrismContext());
                        deltas.add(emptyDelta);
                        getModelService().executeChanges(deltas, options, task, result);
                    } else if (!deltas.isEmpty()) {
                        getModelService().executeChanges(deltas, options, task, result);
                    } else {
                        result.recordSuccess();
                    }

                } catch (Exception ex) {
                    if (!executeForceDelete(userWrapper, task, options, result)) {
                        result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
                        LoggingUtils.logException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
                    } else {
                        result.recomputeStatus();
                    }
                }
                break;
            // support for add/delete containers (e.g. delete credentials)
            default:
                error(getString("pageUser.message.unsupportedState", userWrapper.getStatus()));
        }

        result.recomputeStatus();
        // } catch (Exception ex) {
        // if (!executeForceDelete(userWrapper, task, options, result)) {
        // result.recordFatalError(getString("pageUser.message.cantCreateUser"),
        // ex);
        // LoggingUtils.logException(LOGGER,
        // getString("pageUser.message.cantCreateUser"), ex);
        // } else{
        // result.recomputeStatus();
        // }
        // }

        boolean userAdded = delta != null && delta.isAdd() && StringUtils.isNotEmpty(delta.getOid());
        if (userAdded || !result.isFatalError()) {
            showResultInSession(result);
            // todo refactor this...what is this for? why it's using some
            // "shadow" param from result???
            PrismObject<UserType> user = userWrapper.getObject();
            UserType userType = user.asObjectable();
            for (ObjectReferenceType ref : userType.getLinkRef()) {
                Object o = findParam("shadow", ref.getOid(), result);
                if (o != null && o instanceof ShadowType) {
                    ShadowType accountType = (ShadowType) o;
                    OperationResultType fetchResult = accountType.getFetchResult();
                    if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
                        showResultInSession(OperationResult.createOperationResult(fetchResult));
                    }
                }
            }
            StringValue returnPage = getPageParameters().get(PARAM_RETURN_PAGE);
            if (!StringUtils.isBlank(returnPage.toString())
                    && PageOrgStruct.PARAM_ORG_RETURN.equals(returnPage.toString())) {
                setResponsePage(PageOrgStruct.class);
            } else {
                setResponsePage(PageUsers.class);
            }
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }

    }

    private boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
                                       OperationResult parentResult) {
        if (executeOptionsModel.getObject().isForce()) {
            OperationResult result = parentResult.createSubresult("Force delete operation");
            // List<UserAccountDto> accountDtos = accountsModel.getObject();
            // List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
            // ObjectDelta<UserType> forceDeleteDelta = null;
            // for (UserAccountDto accDto : accountDtos) {
            // if (accDto.getStatus() == UserDtoStatus.DELETE) {
            // ObjectWrapper accWrapper = accDto.getObject();
            // ReferenceDelta refDelta =
            // ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF,
            // userWrapper.getObject().getDefinition(), accWrapper.getObject());
            // refDeltas.add(refDelta);
            // }
            // }
            // if (!refDeltas.isEmpty()) {
            // forceDeleteDelta =
            // ObjectDelta.createModifyDelta(userWrapper.getObject().getOid(),
            // refDeltas,
            // UserType.class, getPrismContext());
            // }
            // PrismContainerDefinition def =
            // userWrapper.getObject().findContainer(UserType.F_ASSIGNMENT).getDefinition();
            // if (forceDeleteDelta == null) {
            // forceDeleteDelta =
            // ObjectDelta.createEmptyModifyDelta(UserType.class,
            // userWrapper.getObject().getOid(),
            // getPrismContext());
            // }
            try {
                ObjectDelta<UserType> forceDeleteDelta = getChange(userWrapper);
                forceDeleteDelta.setPrismContext(getPrismContext());

                if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
                    getModelService().executeChanges(WebMiscUtil.createDeltaCollection(forceDeleteDelta), options,
                            task, result);
                }
            } catch (Exception ex) {
                result.recordFatalError("Failed to execute delete operation with force.");
                LoggingUtils.logException(LOGGER, "Failed to execute delete operation with force", ex);
                return false;
            }

            result.recomputeStatus();
            result.recordSuccessIfUnknown();
            return true;
        }
        return false;
    }

    private ObjectDelta getChange(ObjectWrapper userWrapper) throws SchemaException {

        List<UserAccountDto> accountDtos = accountsModel.getObject();
        List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
        ObjectDelta<UserType> forceDeleteDelta = null;
        for (UserAccountDto accDto : accountDtos) {
            if (accDto.getStatus() == UserDtoStatus.DELETE) {
                ObjectWrapper accWrapper = accDto.getObject();
                ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, userWrapper
                        .getObject().getDefinition(), accWrapper.getObject());
                refDeltas.add(refDelta);
            } else if (accDto.getStatus() == UserDtoStatus.UNLINK) {
                ObjectWrapper accWrapper = accDto.getObject();
                ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, userWrapper
                        .getObject().getDefinition(), accWrapper.getObject().getOid());
                refDeltas.add(refDelta);
            }
        }
        if (!refDeltas.isEmpty()) {
            forceDeleteDelta = ObjectDelta.createModifyDelta(userWrapper.getObject().getOid(), refDeltas,
                    UserType.class, getPrismContext());
        }
        PrismContainerDefinition def = userWrapper.getObject().findContainer(UserType.F_ASSIGNMENT).getDefinition();
        if (forceDeleteDelta == null) {
            forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userWrapper.getObject().getOid(),
                    getPrismContext());
        }

        handleAssignmentDeltas(forceDeleteDelta, def);
        return forceDeleteDelta;
    }

    private Object object;

    public Object findParam(String param, String oid, OperationResult result) {

        for (OperationResult subResult : result.getSubresults()) {
            if (subResult != null && subResult.getParams() != null) {
                if (subResult.getParams().get(param) != null
                        && subResult.getParams().get(OperationResult.PARAM_OID) != null
                        && subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
                    return subResult.getParams().get(param);
                }
                object = findParam(param, oid, subResult);

            }
        }
        return object;
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

    private List<AssignmentEditorDto> getSelectedAssignments() {
        List<AssignmentEditorDto> selected = new ArrayList<AssignmentEditorDto>();

        List<AssignmentEditorDto> all = assignmentsModel.getObject();
        for (AssignmentEditorDto wrapper : all) {
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
                ShadowType shadow = new ShadowType();
                shadow.setResource(resource);

                RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource.asPrismObject(),
                        LayerType.PRESENTATION, getPrismContext());
                if (refinedSchema == null) {
                    error(getString("pageUser.message.couldntCreateAccountNoSchema", resource.getName()));
                    continue;
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.dump());
                }

                QName objectClass = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition()
                        .getTypeName();
                shadow.setObjectClass(objectClass);

                getPrismContext().adopt(shadow);

                ObjectWrapper wrapper = new ObjectWrapper(WebMiscUtil.getOrigStringFromPoly(resource.getName()), null,
                        shadow.asPrismObject(), ContainerStatus.ADDING);
                if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
                    showResultInSession(wrapper.getResult());
                }

                wrapper.setShowEmpty(true);
                wrapper.setMinimalized(false);
                accountsModel.getObject().add(new UserAccountDto(wrapper, UserDtoStatus.ADD));
                setResponsePage(getPage());
            } catch (Exception ex) {
                error(getString("pageUser.message.couldntCreateAccount", resource.getName(), ex.getMessage()));
                LoggingUtils.logException(LOGGER, "Couldn't create account", ex);
            }
        }

        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS)));
    }

    private void addSelectedResourceAssignPerformed(ResourceType resource) {
        AssignmentType assignment = new AssignmentType();
        ConstructionType construction = new ConstructionType();
        assignment.setConstruction(construction);

        try {
            getPrismContext().adopt(assignment, UserType.class, new ItemPath(UserType.F_ASSIGNMENT));
        } catch (SchemaException e) {
            error(getString("Could not create assignment", resource.getName(), e.getMessage()));
            LoggingUtils.logException(LOGGER, "Couldn't create assignment", e);
            return;
        }

        construction.setResource(resource);

        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        AssignmentEditorDto dto = new AssignmentEditorDto(resource, AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION,
                UserDtoStatus.ADD, assignment);
        assignments.add(dto);

        dto.setMinimized(false);
        dto.setShowEmpty(true);
    }

    private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables) {
        ModalWindow window = (ModalWindow) get(MODAL_ID_ASSIGNABLE);
        window.close(target);

        if (newAssignables.isEmpty()) {
            warn(getString("pageUser.message.noAssignableSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (ObjectType object : newAssignables) {
            try {
                if (object instanceof ResourceType) {
                    addSelectedResourceAssignPerformed((ResourceType) object);
                    continue;
                }

                AssignmentEditorDtoType aType = AssignmentEditorDtoType.getType(object.getClass());

                ObjectReferenceType targetRef = new ObjectReferenceType();
                targetRef.setOid(object.getOid());
                targetRef.setType(aType.getQname());

                AssignmentType assignment = new AssignmentType();
                assignment.setTargetRef(targetRef);

                AssignmentEditorDto dto = new AssignmentEditorDto(object, aType, UserDtoStatus.ADD, assignment);
                dto.setMinimized(false);
                dto.setShowEmpty(true);

                assignments.add(dto);
            } catch (Exception ex) {
                error(getString("pageUser.message.couldntAssignObject", object.getName(), ex.getMessage()));
                LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
            }
        }

        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS)));
    }

    private void updateAccountActivation(AjaxRequestTarget target, List<UserAccountDto> accounts, boolean enabled) {
        if (!isAnyAccountSelected(target)) {
            return;
        }

        for (UserAccountDto account : accounts) {
            ObjectWrapper wrapper = account.getObject();
            ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(
                    ShadowType.F_ACTIVATION));
            if (activation == null) {
                warn(getString("pageUser.message.noActivationFound", wrapper.getDisplayName()));
                continue;
            }

            PropertyWrapper enabledProperty = activation.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
            if (enabledProperty.getValues().size() != 1) {
                warn(getString("pageUser.message.noEnabledPropertyFound", wrapper.getDisplayName()));
                continue;
            }
            ValueWrapper value = enabledProperty.getValues().get(0);
            ActivationStatusType status = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
            value.getValue().setValue(status);

            wrapper.setSelected(false);
        }

        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS)));
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
        target.add(get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS)));
    }

    private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target, List<AssignmentEditorDto> selected) {
        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assignment : selected) {
            if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
                assignments.remove(assignment);
            } else {
                assignment.setStatus(UserDtoStatus.DELETE);
                assignment.setSelected(false);
            }
        }

        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS)));
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
        target.add(get(createComponentPath(ID_MAIN_FORM, ID_ACCOUNTS)));
    }

    private void unlockAccountPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
        if (!isAnyAccountSelected(target)) {
            return;
        }

        for (UserAccountDto account : selected) {
            // TODO: implement unlock
        }
    }

    private void deleteAssignmentPerformed(AjaxRequestTarget target) {
        List<AssignmentEditorDto> selected = getSelectedAssignments();
        if (selected.isEmpty()) {
            warn(getString("pageUser.message.noAssignmentSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        showModalWindow(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT, target);
    }

    // many things could change (e.g. assignments, tasks) - here we deal only with tasks
    @Override
    public PageBase reinitialize() {
        TablePanel taskTable = (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TASKS, ID_TASK_TABLE));
        TaskDtoProvider provider = (TaskDtoProvider) taskTable.getDataTable().getDataProvider();

        provider.clearCache();
        taskTable.modelChanged();
        return this;
    }
}
