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
package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collection;

/**
 *  @author shood
 * */
public class DeleteAllDialog extends ModalWindow{

    private static final Trace LOGGER = TraceManager.getTrace(DeleteAllDialog.class);

    private static final String DOT_CLASS = DeleteAllDialog.class.getName() + ".";
    private static final String OPERATION_SEARCH_ITERATIVE_TASK = DOT_CLASS + "searchIterativeTask";
    private static final String OPERATION_COUNT_TASK = DOT_CLASS + "countObjectsTask";

    private static final String ID_CHB_USERS = "checkboxUsers";
    private static final String ID_CHB_ORG = "checkboxOrg";
    private static final String ID_CHB_ACCOUNT_SHADOW = "checkboxAccountShadow";
    private static final String ID_CHB_ROLE_SHADOW = "checkboxRoleShadow";
    private static final String ID_CHB_ORG_SHADOW = "checkboxOrgShadow";
    private static final String ID_TEXT_USERS = "confirmTextUsers";
    private static final String ID_TEXT_ORGS = "confirmTextOrgUnits";
    private static final String ID_TEXT_ACC_SHADOWS = "confirmTextAccountShadow";
    private static final String ID_TEXT_ROLE_SHADOWS = "confirmTextRoleShadows";
    private static final String ID_TEXT_ORG_SHADOWS = "confirmTextOrgShadows";
    private static final String ID_YES = "yes";
    private static final String ID_NO = "no";
    private static final String ID_TOTAL = "totalCountLabel";
    private static final String EMPTY_LABEL = "";

    private static final String ID_FOCUS_TYPE_ORG = "OrgType";
    private static final String ID_FOCUS_TYPE_ROLE = "RoleType";

    private IModel<String> messageUsers;
    private IModel<String> messageOrgUnits;
    private IModel<String> messageAccountShadows;
    private IModel<String> messageRoleShadows;
    private IModel<String> messageOrgShadows;
    private IModel<String> messageTotal;
    protected IModel<Boolean> deleteUsers = Model.of(Boolean.FALSE);
    protected IModel<Boolean> deleteOrgs = Model.of(Boolean.FALSE);
    protected IModel<Boolean> deleteAccountShadow = Model.of(Boolean.FALSE);
    protected IModel<Boolean> deleteRoleShadow = Model.of(Boolean.FALSE);
    protected IModel<Boolean> deleteOrgShadow = Model.of(Boolean.FALSE);

    private int objectsToDelete = 0;
    private int accountShadowTypeCount = 0;
    private int orgUnitCount = 0;
    private int userCount = 0;
    private int orgShadowCount = 0;
    private int roleShadowCount = 0;

    public DeleteAllDialog(String id, IModel<String> title){
        super(id);

        if(title != null){
            setTitle(title);
        }

        loadMessages();
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(ConfirmationDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        showUnloadConfirmation(false);
        setResizable(false);
        setInitialWidth(550);
        setInitialHeight(350);
        setWidthUnit("px");

        setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                DeleteAllDialog.this.close(target);
            }
        });

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);

        initLayout(content);
    }

    private void loadMessages(){
        messageUsers = new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteUsersMessage();
            }
        };

        messageOrgUnits = new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteOrgUnitsMessage();
            }
        };

        messageAccountShadows = new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteAccountShadowsMessage();
            }
        };

        messageOrgShadows = new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteShadowsMessage(ID_FOCUS_TYPE_ORG);
            }
        };

        messageRoleShadows = new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteShadowsMessage(ID_FOCUS_TYPE_ROLE);
            }
        };

        messageTotal = new LoadableModel<String>() {

            @Override
            protected String load() {
                return createTotalMessage();
            }
        };
    }

    private void initLayout(WebMarkupContainer content){

        CheckBox deleteUsersCheckbox = new CheckBox(ID_CHB_USERS, deleteUsers);
        deleteUsersCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                messageUsers.setObject(createDeleteUsersMessage());
                messageTotal.setObject(createTotalMessage());
                target.add(getLabel(ID_TEXT_USERS));
                target.add(getLabel(ID_TOTAL));
            }
        });
        content.add(deleteUsersCheckbox);

        CheckBox deleteOrgsCheckbox = new CheckBox(ID_CHB_ORG, deleteOrgs);
        deleteOrgsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                messageOrgUnits.setObject(createDeleteOrgUnitsMessage());
                messageTotal.setObject(createTotalMessage());
                target.add(getLabel(ID_TEXT_ORGS));
                target.add(getLabel(ID_TOTAL));
            }
        });
        content.add(deleteOrgsCheckbox);

        CheckBox deleteAccountShadowsCheckbox = new CheckBox(ID_CHB_ACCOUNT_SHADOW, deleteAccountShadow);
        deleteAccountShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                messageAccountShadows.setObject(createDeleteAccountShadowsMessage());
                messageTotal.setObject(createTotalMessage());
                target.add(getLabel(ID_TEXT_ACC_SHADOWS));
                target.add(getLabel(ID_TOTAL));
            }
        });
        content.add(deleteAccountShadowsCheckbox);

        CheckBox deleteOrgShadowsCheckbox = new CheckBox(ID_CHB_ORG_SHADOW, deleteOrgShadow);
        deleteOrgShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                messageOrgShadows.setObject(createDeleteShadowsMessage(ID_FOCUS_TYPE_ORG));
                messageTotal.setObject(createTotalMessage());
                target.add(getLabel(ID_TEXT_ORG_SHADOWS));
                target.add(getLabel(ID_TOTAL));
            }
        });
        content.add(deleteOrgShadowsCheckbox);

        CheckBox deleteRoleShadowsCheckbox = new CheckBox(ID_CHB_ROLE_SHADOW, deleteRoleShadow);
        deleteRoleShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                messageRoleShadows.setObject(createDeleteShadowsMessage(ID_FOCUS_TYPE_ROLE));
                messageTotal.setObject(createTotalMessage());
                target.add(getLabel(ID_TEXT_ROLE_SHADOWS));
                target.add(getLabel(ID_TOTAL));
            }
        });
        content.add(deleteRoleShadowsCheckbox);

        Label usersLabel = new Label(ID_TEXT_USERS, messageUsers);
        usersLabel.setOutputMarkupId(true);
        content.add(usersLabel);

        Label orgsLabel = new Label(ID_TEXT_ORGS, messageOrgUnits);
        orgsLabel.setOutputMarkupId(true);
        content.add(orgsLabel);

        Label accShadowsLabel = new Label(ID_TEXT_ACC_SHADOWS, messageAccountShadows);
        accShadowsLabel.setOutputMarkupId(true);
        content.add(accShadowsLabel);

        Label orgShadowsLabel = new Label(ID_TEXT_ORG_SHADOWS, messageOrgShadows);
        orgShadowsLabel.setOutputMarkupId(true);
        content.add(orgShadowsLabel);

        Label roleShadowsLabel = new Label(ID_TEXT_ROLE_SHADOWS, messageRoleShadows);
        roleShadowsLabel.setOutputMarkupId(true);
        content.add(roleShadowsLabel);

        Label countLabel = new Label(ID_TOTAL, messageTotal);
        countLabel.setOutputMarkupId(true);
        content.add(countLabel);

        AjaxButton yesButton = new AjaxButton(ID_YES, new StringResourceModel("deleteAllDialog.yes",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                yesPerformed(target);
            }
        };
        content.add(yesButton);

        AjaxButton noButton = new AjaxButton(ID_NO, new StringResourceModel("deleteAllDialog.no",
                this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        content.add(noButton);
    }

    private Label getLabel(String ID){
        return (Label)get(getContentId()+":"+ID);
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, new Model<String>(), resourceKey, objects);
    }

    private String createTotalMessage(){
        objectsToDelete = 0;
        if(deleteUsers.getObject()){
            objectsToDelete += userCount;
        }
        if(deleteOrgs.getObject()){
            objectsToDelete += orgUnitCount;
        }
        if(deleteAccountShadow.getObject()){
            objectsToDelete += accountShadowTypeCount;
        }
        if(deleteOrgShadow.getObject()){
            objectsToDelete += orgShadowCount;
        }
        if(deleteRoleShadow.getObject()){
            objectsToDelete += roleShadowCount;
        }

        return createStringResource("pageDebugList.label.totalToDelete", objectsToDelete).getString();
    }

    private String createDeleteUsersMessage(){
        if(!deleteUsers.getObject()){
            return createStringResource("pageDebugList.label.usersDelete", 0).getString();
        }

        userCount = 0;

        Task task = createSimpleTask(OPERATION_COUNT_TASK);
        OperationResult result = new OperationResult(OPERATION_COUNT_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            userCount = getModelService().countObjects(UserType.class, null, options, task, result);

            //We need to substract 1, because we are not deleting user 'Administrator'
            userCount--;
            objectsToDelete += userCount;
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.countSearchProblem"), ex);
        }

        return createStringResource("pageDebugList.label.usersDelete", userCount).getString();
    }

    private String createDeleteOrgUnitsMessage(){
        if(!deleteOrgs.getObject()){
            return createStringResource("pageDebugList.label.orgUnitsDelete", 0).getString();
        }

        orgUnitCount = 0;

        Task task = createSimpleTask(OPERATION_COUNT_TASK);
        OperationResult result = new OperationResult(OPERATION_COUNT_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            orgUnitCount = getModelService().countObjects(OrgType.class, null, options, task, result);

            objectsToDelete += orgUnitCount;
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.countSearchProblem"), ex);
        }

        return createStringResource("pageDebugList.label.orgUnitsDelete", orgUnitCount).getString();
    }

    private String createDeleteAccountShadowsMessage(){
        if(!deleteAccountShadow.getObject()){
            return createStringResource("pageDebugList.label.accountShadowsDelete", 0).getString();
        }

        accountShadowTypeCount = 0;

        Task task = createSimpleTask(OPERATION_SEARCH_ITERATIVE_TASK);
        OperationResult result = new OperationResult(OPERATION_SEARCH_ITERATIVE_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        ResultHandler<ShadowType> accountShadowHandler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                ShadowType shadow = (ShadowType)object.asObjectable();
                if(ShadowKindType.ACCOUNT.equals(shadow.getKind())){
                    accountShadowTypeCount++;
                }
                return true;
            }
        };

        try {
            getModelService().searchObjectsIterative(ShadowType.class, null, accountShadowHandler, options, task, result);
            objectsToDelete += accountShadowTypeCount;
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.countSearchProblem"), ex);
        }

        return createStringResource("pageDebugList.label.accountShadowsDelete", accountShadowTypeCount).getString();
    }

    private String createDeleteShadowsMessage(final String focus){
        if(ID_FOCUS_TYPE_ORG.equals(focus) && !deleteOrgShadow.getObject()){
            return createStringResource("pageDebugList.label.orgShadowsDelete", 0).getString();
        } else if(ID_FOCUS_TYPE_ROLE.equals(focus) && !deleteRoleShadow.getObject()){
            return createStringResource("pageDebugList.label.roleShadowsDelete", 0).getString();
        }

        orgShadowCount = 0;
        roleShadowCount = 0;
        int count = 0;

        Task task = createSimpleTask(OPERATION_SEARCH_ITERATIVE_TASK);
        final OperationResult result = new OperationResult(OPERATION_SEARCH_ITERATIVE_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        ResultHandler<ShadowType> orgShadowHandler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                ShadowType shadow = (ShadowType)object.asObjectable();
                if(!ShadowKindType.ACCOUNT.equals(shadow.getKind())){
                    String oid = shadow.getResourceRef().getOid();

                    PrismObject<ResourceType> resource = WebModelUtils.loadObject(ResourceType.class, oid, result, (PageBase) getPage());

                    if(resource != null && resource.asObjectable() != null){
                        SynchronizationType sync = resource.asObjectable().getSynchronization();

                        for(ObjectSynchronizationType s: sync.getObjectSynchronization()){
                            if(s.getFocusType() != null && focus.equals(s.getFocusType().getLocalPart())){
                                addShadowType(focus);
                            }
                        }
                    }
                }
                return true;
            }
        };

        try {
            getModelService().searchObjectsIterative(ShadowType.class, null, orgShadowHandler, options, task, result);
            if(ID_FOCUS_TYPE_ORG.equals(focus)){
                count = orgShadowCount;
            } else if(ID_FOCUS_TYPE_ROLE.equals(focus)){
                count = roleShadowCount;
            }

            objectsToDelete += count;
        } catch (Exception ex) {
            result.computeStatus(getString("pageDebugList.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("pageDebugList.message.countSearchProblem"), ex);
        }

        if(ID_FOCUS_TYPE_ORG.equals(focus)){
            return createStringResource("pageDebugList.label.orgShadowsDelete", count).getString();
        } else if(ID_FOCUS_TYPE_ROLE.equals(focus)){
            return createStringResource("pageDebugList.label.roleShadowsDelete", count).getString();
        } else {
            return EMPTY_LABEL;
        }
    }

    public int getObjectsToDelete(){
        return objectsToDelete;
    }

    private void addShadowType(String focus){
        if(ID_FOCUS_TYPE_ORG.equals(focus)){
            orgShadowCount++;
        } else if(ID_FOCUS_TYPE_ROLE.equals(focus)){
            roleShadowCount++;
        }
    }

    public Task createSimpleTask(String operation){
        return null;
    }

    public ModelService getModelService(){
        return null;
    }

    public void yesPerformed(AjaxRequestTarget target) {

    }

    public void noPerformed(AjaxRequestTarget target) {
        close(target);
    }
}
