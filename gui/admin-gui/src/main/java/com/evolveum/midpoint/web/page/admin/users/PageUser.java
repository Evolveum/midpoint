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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.CheckTableHeader;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.SimpleErrorPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableOrgPopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignablePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableRolePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ResourcesPopup;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.ContextRelativeResource;
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
@PageDescriptor(url = "/admin/user", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#user",
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description")})
public class PageUser extends PageAdminUsers implements ProgressReportingAwarePage {

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
    private static final String OPERATION_SEARCH_RESOURCE = DOT_CLASS + "searchAccountResource";

    private static final String MODAL_ID_RESOURCE = "resourcePopup";
    private static final String MODAL_ID_ASSIGNABLE = "assignablePopup";
    private static final String MODAL_ID_ASSIGNABLE_ORG = "assignableOrgPopup";
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

    private static final String ID_SUMMARY_PANEL = "summaryPanel";
    private static final String ID_SUMMARY_NAME = "summaryName";
    private static final String ID_SUMMARY_FULL_NAME = "summaryFullName";
    private static final String ID_SUMMARY_GIVEN_NAME = "summaryGivenName";
    private static final String ID_SUMMARY_FAMILY_NAME = "summaryFamilyName";
    private static final String ID_SUMMARY_PHOTO = "summaryPhoto";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    private LoadableModel<ObjectWrapper> userModel;
    private LoadableModel<List<UserAccountDto>> accountsModel;
    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
    private IModel<PrismObject<UserType>> summaryUser;

    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
            = new LoadableModel<ExecuteChangeOptionsDto>(false) {

        @Override
        protected ExecuteChangeOptionsDto load() {
            return new ExecuteChangeOptionsDto();
        }
    };

    private ProgressReporter progressReporter;
    private ObjectDelta delta;                      // used to determine whether to leave this page or stay on it (after operation finishing)

    public PageUser() {
        this(null);
    }

    public PageUser(final PrismObject<UserType> userToEdit) {
        userModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadUserWrapper(userToEdit);
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

    @Override
    protected IModel<String> createPageTitleModel(){
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isEditingUser()){
                    return createStringResource("pageUser.title.newUser").getObject();
                }

                return createStringResource("pageUser.title.editUser").getObject();
            }
        };
    }

    @Override
    protected IModel<String> createPageSubTitleModel(){
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isEditingUser()){
                    return createStringResource("pageUser.subTitle.newUser").getObject();
                }

                String name = null;
                if(userModel != null && userModel.getObject() != null && userModel.getObject().getObject() != null){
                    name = WebMiscUtil.getName(userModel.getObject().getObject());
                }

                return createStringResource("pageUser.subTitle.edituser", name).getObject();
            }
        };
    }

    private ObjectWrapper loadUserWrapper(PrismObject<UserType> userToEdit) {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = null;
        try {
            if (!isEditingUser()) {
                if (userToEdit == null) {
                    UserType userType = new UserType();
                    getMidpointApplication().getPrismContext().adopt(userType);
                    user = userType.asPrismObject();
                } else {
                    user= userToEdit;
                }
            } else {
                Task task = createSimpleTask(OPERATION_LOAD_USER);

                StringValue userOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                Collection options = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
                        GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));

                user = getModelService().getObject(UserType.class, userOid.toString(), options, task, result);
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
        ObjectWrapper wrapper = null;
        try{
        	wrapper = ObjectWrapperUtil.createObjectWrapper("pageUser.userDetails", null, user, status, this);
        } catch (Exception ex){
        	result.recordFatalError("Couldn't get user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
            wrapper = new ObjectWrapper("pageUser.userDetails", null, user, null, status, this);
        }
//        ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails", null, user, status);
        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
            showResultInSession(wrapper.getResult());
        }

        wrapper.setShowEmpty(!isEditingUser());
        return wrapper;
    }

    private void initLayout() {
        final Form mainForm = new Form(ID_MAIN_FORM, true);
        mainForm.setMaxSize(MidPointApplication.USER_PHOTO_MAX_FILE_SIZE);
        mainForm.setMultiPart(true);
        add(mainForm);

        progressReporter = ProgressReporter.create(this, mainForm, "progressPanel");

        initSummaryInfo(mainForm);

        PrismObjectPanel userForm = new PrismObjectPanel(ID_USER_FORM, userModel, new PackageResourceReference(
                ImgResources.class, ImgResources.USER_PRISM), mainForm, this) {

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

    private String getLabelFromPolyString(PolyStringType poly){
        if(poly == null || poly.getOrig() == null){
            return "-";
        } else{
            return poly.getOrig();
        }
    }

    private void initSummaryInfo(Form mainForm){

        WebMarkupContainer summaryContainer = new WebMarkupContainer(ID_SUMMARY_PANEL);
        summaryContainer.setOutputMarkupId(true);

        summaryContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible(){
                if(getPageParameters().get(OnePageParameterEncoder.PARAMETER).isEmpty()){
                    return false;
                } else {
                    return true;
                }
            }
        });

        mainForm.add(summaryContainer);

        summaryUser = new AbstractReadOnlyModel<PrismObject<UserType>>() {

            @Override
            public PrismObject<UserType> getObject() {
                ObjectWrapper user = userModel.getObject();
                return user.getObject();
            }
        };

        summaryContainer.add(new Label(ID_SUMMARY_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_NAME)));
        summaryContainer.add(new Label(ID_SUMMARY_FULL_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_FULL_NAME)));
        summaryContainer.add(new Label(ID_SUMMARY_GIVEN_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_GIVEN_NAME)));
        summaryContainer.add(new Label(ID_SUMMARY_FAMILY_NAME, new PrismPropertyModel<>(summaryUser, UserType.F_FAMILY_NAME)));

        Image img = new Image(ID_SUMMARY_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                if(summaryUser.getObject().asObjectable().getJpegPhoto() != null){
                    return new ByteArrayResource("image/jpeg", summaryUser.getObject().asObjectable().getJpegPhoto());
                } else {
                    return new ContextRelativeResource("img/placeholder.png");
                }

            }
        });
        summaryContainer.add(img);
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
                showAssignableOrgPopup(target);
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

    private void initAccounts(final WebMarkupContainer accounts) {
        InlineMenu accountMenu = new InlineMenu(ID_ACCOUNT_MENU, new Model((Serializable) createAccountsMenu()));
        accounts.add(accountMenu);

        final ListView<UserAccountDto> accountList = new ListView<UserAccountDto>(ID_ACCOUNT_LIST, accountsModel) {

            @Override
            protected void populateItem(final ListItem<UserAccountDto> item) {
                PackageResourceReference packageRef;
                final UserAccountDto dto = item.getModelObject();

                Panel panel;

                if(dto.isLoadedOK()){
                    packageRef = new PackageResourceReference(ImgResources.class,
                            ImgResources.HDD_PRISM);

                    panel = new PrismObjectPanel("account", new PropertyModel<ObjectWrapper>(
                            item.getModel(), "object"), packageRef, (Form) PageUser.this.get(ID_MAIN_FORM), PageUser.this) {

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
                } else{
                    panel = new SimpleErrorPanel("account", item.getModel()){

                        @Override
                        public void onShowMorePerformed(AjaxRequestTarget target){
                            OperationResult fetchResult = dto.getResult();
                            if (fetchResult != null) {
                                showResult(fetchResult);
                                target.add(getPageBase().getFeedbackPanel());
                            }
                        }
                    };
                }

                panel.setOutputMarkupId(true);
                item.add(panel);
            }
        };

        AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_ACCOUNT_CHECK_ALL, new Model()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for(UserAccountDto dto: accountList.getModelObject()){
                    if(dto.isLoadedOK()){
                        ObjectWrapper accModel = dto.getObject();
                        accModel.setSelected(getModelObject());
                    }
                }

                target.add(accounts);
            }
        };
        accounts.add(accountCheckAll);

        accounts.add(accountList);
    }

    private List<UserAccountDto> loadAccountWrappers() {
        List<UserAccountDto> list = new ArrayList<UserAccountDto>();

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<ObjectReferenceType> references = prismUser.asObjectable().getLinkRef();

        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
        for (ObjectReferenceType reference : references) {
            OperationResult subResult = new OperationResult(OPERATION_LOAD_ACCOUNT);
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

                ResourceType resource = accountType.getResource();
                String resourceName = WebMiscUtil.getName(resource);
                
                ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName, WebMiscUtil.getOrigStringFromPoly(accountType
                        .getName()), account, ContainerStatus.MODIFYING, true, this);
//                ObjectWrapper wrapper = new ObjectWrapper(resourceName, WebMiscUtil.getOrigStringFromPoly(accountType
//                        .getName()), account, ContainerStatus.MODIFYING);
                wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
                wrapper.setSelectable(true);
                wrapper.setMinimalized(true);
                
                PrismContainer<ShadowAssociationType> associationContainer = account.findContainer(ShadowType.F_ASSOCIATION);
                if (associationContainer != null && associationContainer.getValues() != null){
                	List<PrismProperty> associations = new ArrayList<>(associationContainer.getValues().size());
                	for (PrismContainerValue associationVal : associationContainer.getValues()){
                		ShadowAssociationType associationType = (ShadowAssociationType) associationVal.asContainerable();
                        // we can safely eliminate fetching from resource, because we need only the name
                		PrismObject<ShadowType> association = getModelService().getObject(ShadowType.class, associationType.getShadowRef().getOid(),
                                SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, subResult);
                		associations.add(association.findProperty(ShadowType.F_NAME));
                	}
                	
                	wrapper.setAssociations(associations);
                	
                }

                wrapper.initializeContainers(this);

                list.add(new UserAccountDto(wrapper, UserDtoStatus.MODIFY));

                subResult.recomputeStatus();
            } catch (ObjectNotFoundException ex) {
                // this is fix for MID-854, full user/accounts/assignments reload if accountRef reference is broken
                // because consistency already fixed it.

                userModel.reset();
                accountsModel.reset();
                assignmentsModel.reset();
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
                list.add(new UserAccountDto(false, getResourceName(reference.getOid()), subResult));
            } finally {
                subResult.computeStatus();
            }
        }

        return list;
    }

    private String getResourceName(String oid){
        OperationResult result = new OperationResult(OPERATION_SEARCH_RESOURCE);
        Task task = createSimpleTask(OPERATION_SEARCH_RESOURCE);

        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());

            PrismObject<ShadowType> shadow = getModelService().getObject(ShadowType.class, oid, options, task, result);
            PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class,
                    shadow.asObjectable().getResourceRef().getOid(), null, task, result);

            if(resource != null){
                return WebMiscUtil.getOrigStringFromPoly(resource.asObjectable().getName());
            }
        } catch (Exception e){
            result.recordFatalError("Account Resource was not found. " + e.getMessage());
            LoggingUtils.logException(LOGGER, "Account Resource was not found.", e);
            showResult(result);
        }

        return "-";
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

            list.add(new AssignmentEditorDto(targetObject, type, UserDtoStatus.MODIFY, assignment, this));
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

        if(!subResult.isHandledError() && !subResult.isSuccess()){
            showResult(subResult);
        }

        return target;
    }

    private void initAssignments(final WebMarkupContainer assignments) {
        InlineMenu accountMenu = new InlineMenu(ID_ASSIGNMENT_MENU, new Model((Serializable) createAssignmentsMenu()));
        assignments.add(accountMenu);

        final ListView<AssignmentEditorDto> assignmentList = new ListView<AssignmentEditorDto>(ID_ASSIGNMENT_LIST,
                assignmentsModel) {

            @Override
            protected void populateItem(final ListItem<AssignmentEditorDto> item) {
                AssignmentEditorPanel assignmentEditor = new AssignmentEditorPanel(ID_ASSIGNMENT_EDITOR,
                        item.getModel());
                item.add(assignmentEditor);
            }
        };
        assignmentList.setOutputMarkupId(true);
        assignments.add(assignmentList);

        AjaxCheckBox assignmentCheckAll = new AjaxCheckBox(ID_ASSIGNMENT_CHECK_ALL, new Model()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for(AssignmentEditorDto item: assignmentList.getModelObject()){
                    item.setSelected(this.getModelObject());
                }

                target.add(assignments);
            }
        };
        assignmentCheckAll.setOutputMarkupId(true);
        assignments.add(assignmentCheckAll);
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
                StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

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
            filters.add(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class, getPrismContext(), oid));
            filters.add(NotFilter.createNot(EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS, TaskType.class, getPrismContext(), null, TaskExecutionStatusType.CLOSED)));
            filters.add(EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, getPrismContext(), null));
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


    private void initButtons(final Form mainForm) {
        AjaxSubmitButton saveButton = new AjaxSubmitButton("save",
                createStringResource("pageUser.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                progressReporter.onSaveSubmit();
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        progressReporter.registerSaveButton(saveButton);
        mainForm.add(saveButton);

        AjaxSubmitButton abortButton = new AjaxSubmitButton("abort",
                createStringResource("pageUser.button.abort")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                progressReporter.onAbortSubmit(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        progressReporter.registerAbortButton(abortButton);
        mainForm.add(abortButton);

        AjaxButton back = new AjaxButton("back", createStringResource("pageUser.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);

        mainForm.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true, false));
    }

    private void showAssignablePopup(AjaxRequestTarget target, Class<? extends ObjectType> type) {
        ModalWindow modal = (ModalWindow) get(MODAL_ID_ASSIGNABLE);
        AssignablePopupContent content =  (AssignableRolePopupContent) modal.get(modal.getContentId());
        content.setType(type);
        showModalWindow(MODAL_ID_ASSIGNABLE, target);
        target.add(getFeedbackPanel());
    }
    
    private void showAssignableOrgPopup(AjaxRequestTarget target) {
        ModalWindow modal = (ModalWindow) get(MODAL_ID_ASSIGNABLE_ORG);
        AssignablePopupContent content =  (AssignableOrgPopupContent) modal.get(modal.getContentId());
        content.setType(OrgType.class);
        showModalWindow(MODAL_ID_ASSIGNABLE_ORG, target);
        target.add(getFeedbackPanel());
    }

    private void initResourceModal() {
        ModalWindow window = createModalWindow(MODAL_ID_RESOURCE,
                createStringResource("pageUser.title.selectResource"), 1100, 560);

        final SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, accountsModel){

            @Override
            protected void handlePartialError(OperationResult result) {
                showResult(result);
            }
        };
        window.setContent(new ResourcesPopup(window.getContentId()) {

            @Override
            public SimpleUserResourceProvider getProvider(){
                return provider;
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
                addSelectedAccountPerformed(target, newResources);
            }
        });
        add(window);
    }

    private void initAssignableModal() {
        ModalWindow window = createModalWindow(MODAL_ID_ASSIGNABLE,
                createStringResource("pageUser.title.selectAssignable"), 1100, 560);
        window.setContent(new AssignableRolePopupContent(window.getContentId()) {

            @Override
            protected void handlePartialError(OperationResult result) {
                showResult(result);
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
                addSelectedAssignablePerformed(target, selected, MODAL_ID_ASSIGNABLE);
            }

            @Override
            protected PrismObject<UserType> getUserDefinition() {
                return userModel.getObject().getObject();
            }
        });
        add(window);
        
        window = createModalWindow(MODAL_ID_ASSIGNABLE_ORG,
                createStringResource("pageUser.title.selectAssignable"), 1150, 600);
        window.setContent(new AssignableOrgPopupContent(window.getContentId()) {

            @Override
            protected void handlePartialError(OperationResult result) {
                showResult(result);
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
                addSelectedAssignablePerformed(target, selected, MODAL_ID_ASSIGNABLE_ORG);
            }
        });
        add(window);
    }

    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
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
        if (PageOrgTree.PARAM_ORG_RETURN.equals(orgReturn.toString())) {
            setResponsePage(PageOrgTree.class);
        } else {
            setResponsePage(new PageUsers(false));
        }

        // }
        // }catch(Exception ex){
        // LoggingUtils.logException(LOGGER, "Could not return to user list",
        // ex);
        // }
    }

    private List<ObjectDelta<? extends ObjectType>> modifyAccounts(OperationResult result) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

        List<UserAccountDto> accounts = accountsModel.getObject();
        OperationResult subResult = null;
        for (UserAccountDto account : accounts) {
            if(!account.isLoadedOK())
                continue;

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
            if(!accDto.isLoadedOK()){
                continue;
            }

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
        ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

        List<UserAccountDto> accounts = accountsModel.getObject();
        for (UserAccountDto accDto : accounts) {
            if(accDto.isLoadedOK()){
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
        }

        return refDelta;
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> userDelta, PrismContainerDefinition def)
            throws SchemaException {
        ContainerDelta assDelta = new ContainerDelta(new ItemPath(), UserType.F_ASSIGNMENT, def, getPrismContext());

        PrismObject<UserType> user = userModel.getObject().getObject();
        PrismObjectDefinition userDef = user.getDefinition();
        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue newValue = assDto.getNewValue();

            switch (assDto.getStatus()) {
                case ADD:
                    newValue.applyDefinition(assignmentDef, false);
                    assDelta.addValueToAdd(newValue.clone());
                    break;
                case DELETE:
                    PrismContainerValue oldValue = assDto.getOldValue();
                    oldValue.applyDefinition(assignmentDef);
                    assDelta.addValueToDelete(oldValue.clone());
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
        	assDelta = userDelta.addModification(assDelta);
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
                if (seg.equivalent(firstDeltaSegment)) {
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
        delta = null;

        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
        ModelExecuteOptions options = executeOptions.createOptions();
        LOGGER.debug("Using options {}.", new Object[]{executeOptions});

        try {
            reviveModels();

            delta = userWrapper.getObjectDelta();
            if (userWrapper.getOldDelta() != null) {
                delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
            LoggingUtils.logException(LOGGER, "Create user failed", ex);
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
                        delta.revive(getPrismContext());

                        Collection<SimpleValidationError> validationErrors = performCustomValidation(user, WebMiscUtil.createDeltaCollection(delta));
                        if(validationErrors != null && !validationErrors.isEmpty()){
                            for(SimpleValidationError error: validationErrors){
                                LOGGER.error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
                                error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
                            }

                            target.add(getFeedbackPanel());
                            return;
                        }

                        progressReporter.executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, result, target);
                    } else {
                        result.recordSuccess();
                    }
                } catch (Exception ex) {
                    result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
                    LoggingUtils.logException(LOGGER, "Create user failed", ex);
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
                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());
                        deltas.add(delta);
                    }
                    for (ObjectDelta accDelta : accountDeltas) {
                        if (!accDelta.isEmpty()) {
                            //accDelta.setPrismContext(getPrismContext());
                            accDelta.revive(getPrismContext());
                            deltas.add(accDelta);
                        }
                    }

                    if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
                        ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class,
                                userWrapper.getObject().getOid(), getPrismContext());
                        deltas.add(emptyDelta);

                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
                        if(validationErrors != null && !validationErrors.isEmpty()){
                            for(SimpleValidationError error: validationErrors){
                                LOGGER.error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
                                error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
                            }

                            target.add(getFeedbackPanel());
                            return;
                        }

                        progressReporter.executeChanges(deltas, options, task, result, target);
                    } else if (!deltas.isEmpty()) {
                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
                        if(validationErrors != null && !validationErrors.isEmpty()){
                            for(SimpleValidationError error: validationErrors){
                                LOGGER.error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
                                error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
                            }

                            target.add(getFeedbackPanel());
                            return;
                        }

                        progressReporter.executeChanges(deltas, options, task, result, target);
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

        if (!result.isInProgress()) {
            finishProcessing(target, result);
        }
    }

    private Collection<SimpleValidationError> performCustomValidation(PrismObject<UserType> user, Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
        Collection<SimpleValidationError> errors = null;

        if(user == null){
            if(userModel != null && userModel.getObject() != null && userModel.getObject().getObject() != null){
                user = userModel.getObject().getObject();

                for (ObjectDelta delta: deltas) {
                    if (UserType.class.isAssignableFrom(delta.getObjectTypeClass())) {  // because among deltas there can be also ShadowType deltas
                        delta.applyTo(user);
                    }
                }
            }
        }

        if(user != null && user.asObjectable() != null){
            for(AssignmentType assignment: user.asObjectable().getAssignment()){
                for(MidpointFormValidator validator: getFormValidatorRegistry().getValidators()){
                    if(errors == null){
                        errors = validator.validateAssignment(assignment);
                    } else {
                        errors.addAll(validator.validateAssignment(assignment));
                    }
                }
            }
        }

        for(MidpointFormValidator validator: getFormValidatorRegistry().getValidators()){
            if(errors == null){
                errors = validator.validateObject(user, deltas);
            } else {
                errors.addAll(validator.validateObject(user, deltas));
            }
        }

        return errors;
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result) {

        boolean userAdded = delta != null && delta.isAdd() && StringUtils.isNotEmpty(delta.getOid());
        if (!executeOptionsModel.getObject().isKeepDisplayingResults() && progressReporter.isAllSuccess() && (userAdded || !result.isFatalError())) {           // TODO
            showResultInSession(result);
            // todo refactor this...what is this for? why it's using some
            // "shadow" param from result???
            PrismObject<UserType> user = userModel.getObject().getObject();
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
                    && PageOrgTree.PARAM_ORG_RETURN.equals(returnPage.toString())) {
                setResponsePage(PageOrgTree.class);
            } else {
                setResponsePage(new PageUsers(false));
            }
        } else {
            showResult(result);
            target.add(getFeedbackPanel());

            // if we only stayed on the page because of displaying results, hide the Save button
            // (the content of the page might not be consistent with reality, e.g. concerning the accounts part...
            // this page was not created with the repeated save possibility in mind)
            if (userAdded || !result.isFatalError()) {
                progressReporter.hideSaveButton(target);
            }
        }
    }


    private void reviveModels() throws SchemaException {
        WebMiscUtil.revive(userModel, getPrismContext());
        WebMiscUtil.revive(accountsModel, getPrismContext());
        WebMiscUtil.revive(assignmentsModel, getPrismContext());
        WebMiscUtil.revive(summaryUser, getPrismContext());
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
                forceDeleteDelta.revive(getPrismContext());

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
            if(!accDto.isLoadedOK()){
                continue;
            }

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

    public Object findParam(String param, String oid, OperationResult result) {

        Object object = null;

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
            if (account.isLoadedOK() && account.getObject().isSelected()) {
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
                    LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.debugDump());
                }

                QName objectClass = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition()
                        .getTypeName();
                shadow.setObjectClass(objectClass);

                getPrismContext().adopt(shadow);

                ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(WebMiscUtil.getOrigStringFromPoly(resource.getName()), null,
                        shadow.asPrismObject(), ContainerStatus.ADDING, this);
//                ObjectWrapper wrapper = new ObjectWrapper(WebMiscUtil.getOrigStringFromPoly(resource.getName()), null,
//                        shadow.asPrismObject(), ContainerStatus.ADDING);
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
                UserDtoStatus.ADD, assignment, this);
        assignments.add(dto);

        dto.setMinimized(false);
        dto.setShowEmpty(true);
    }

    private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables, String popupId) {
        ModalWindow window = (ModalWindow) get(popupId);
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

                AssignmentEditorDto dto = new AssignmentEditorDto(object, aType, UserDtoStatus.ADD, assignment, this);
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
            if(!account.isLoadedOK()){
                continue;
            }

            ObjectWrapper wrapper = account.getObject();
            ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(
                    ShadowType.F_ACTIVATION));
            if (activation == null) {
                warn(getString("pageUser.message.noActivationFound", wrapper.getDisplayName()));
                continue;
            }

            PropertyWrapper enabledProperty = activation.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
            if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
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
        target.add(getFeedbackPanel());
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
