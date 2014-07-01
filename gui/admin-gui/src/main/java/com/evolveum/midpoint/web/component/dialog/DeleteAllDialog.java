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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
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

    private IModel<DeleteAllDto> model = new Model(new DeleteAllDto());

//    private int objectsToDelete = 0;
//    private int accountShadowTypeCount = 0;
//    private int orgUnitCount = 0;
//    private int userCount = 0;
//    private int orgShadowCount = 0;
//    private int roleShadowCount = 0;

    public DeleteAllDialog(String id, IModel<String> title){
        super(id);

        if(title != null){
            setTitle(title);
        }

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

    public IModel<DeleteAllDto> getModel(){
        return model;
    }

    private void updateLabelModel(AjaxRequestTarget target, String labelID){
        LoadableModel<String> model = (LoadableModel<String>)getLabel(labelID).getDefaultModel();
        model.reset();

        model = (LoadableModel<String>)getLabel(ID_TOTAL).getDefaultModel();
        model.reset();

        target.add(getLabel(labelID));
        target.add(getLabel(ID_TOTAL));
    }

    private void initLayout(WebMarkupContainer content){

        CheckBox deleteUsersCheckbox = new CheckBox(ID_CHB_USERS, new PropertyModel<Boolean>(model, DeleteAllDto.F_USERS));
        deleteUsersCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_USERS);
            }
        });
        content.add(deleteUsersCheckbox);

        CheckBox deleteOrgsCheckbox = new CheckBox(ID_CHB_ORG, new PropertyModel<Boolean>(model, DeleteAllDto.F_ORGS));
        deleteOrgsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_ORGS);
            }
        });
        content.add(deleteOrgsCheckbox);

        CheckBox deleteAccountShadowsCheckbox = new CheckBox(ID_CHB_ACCOUNT_SHADOW,
                new PropertyModel<Boolean>(model, DeleteAllDto.F_ACC_SHADOW));
        deleteAccountShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_ACC_SHADOWS);
            }
        });
        content.add(deleteAccountShadowsCheckbox);

        CheckBox deleteOrgShadowsCheckbox = new CheckBox(ID_CHB_ORG_SHADOW,
                new PropertyModel<Boolean>(model, DeleteAllDto.F_ORG_SHADOW));
        deleteOrgShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_ORG_SHADOWS);
            }
        });
        content.add(deleteOrgShadowsCheckbox);

        CheckBox deleteRoleShadowsCheckbox = new CheckBox(ID_CHB_ROLE_SHADOW,
                new PropertyModel<Boolean>(model, DeleteAllDto.F_ROLE_SHADOW));
        deleteRoleShadowsCheckbox.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateLabelModel(target, ID_TEXT_ROLE_SHADOWS);
            }
        });
        content.add(deleteRoleShadowsCheckbox);

        Label usersLabel = new Label(ID_TEXT_USERS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteUsersMessage();
            }
        });
        usersLabel.setOutputMarkupId(true);
        content.add(usersLabel);

        Label orgsLabel = new Label(ID_TEXT_ORGS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteOrgUnitsMessage();
            }
        });
        orgsLabel.setOutputMarkupId(true);
        content.add(orgsLabel);

        Label accShadowsLabel = new Label(ID_TEXT_ACC_SHADOWS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteAccountShadowsMessage();
            }
        });
        accShadowsLabel.setOutputMarkupId(true);
        content.add(accShadowsLabel);

        Label orgShadowsLabel = new Label(ID_TEXT_ORG_SHADOWS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteShadowsMessage(OrgType.COMPLEX_TYPE);
            }
        });
        orgShadowsLabel.setOutputMarkupId(true);
        content.add(orgShadowsLabel);

        Label roleShadowsLabel = new Label(ID_TEXT_ROLE_SHADOWS, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createDeleteShadowsMessage(RoleType.COMPLEX_TYPE);
            }
        });
        roleShadowsLabel.setOutputMarkupId(true);
        content.add(roleShadowsLabel);

        Label countLabel = new Label(ID_TOTAL, new LoadableModel<String>() {
            @Override
            protected String load() {
                return createTotalMessage();
            }
        });
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
        DeleteAllDto dto = model.getObject();
        dto.setObjectsToDelete(0);

        if(dto.getDeleteUsers()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getUserCount());
        }
        if(dto.getDeleteOrgs()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getOrgUnitCount());
        }
        if(dto.getDeleteAccountShadow()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getAccountShadowTypeCount());
        }
        if(dto.getDeleteOrgShadow()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getOrgShadowCount());
        }
        if(dto.getDeleteRoleShadow()){
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getRoleShadowCount());
        }

        return createStringResource("deleteAllDialog.label.totalToDelete", dto.getObjectsToDelete()).getString();
    }

    private String createDeleteUsersMessage(){
        if(!model.getObject().getDeleteUsers()){
            return createStringResource("deleteAllDialog.label.usersDelete", 0).getString();
        }
        DeleteAllDto dto = model.getObject();
        Task task = getPagebase().createSimpleTask(OPERATION_COUNT_TASK);
        OperationResult result = new OperationResult(OPERATION_COUNT_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            dto.setUserCount(getPagebase().getModelService().countObjects(UserType.class, null, options, task, result));

            //We need to substract 1, because we are not deleting user 'Administrator'
            dto.setUserCount(dto.getUserCount()-1);
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getUserCount());
        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }

        return createStringResource("deleteAllDialog.label.usersDelete", dto.getUserCount()).getString();
    }

    private String createDeleteOrgUnitsMessage(){
        if(!model.getObject().getDeleteOrgs()){
            return createStringResource("deleteAllDialog.label.orgUnitsDelete", 0).getString();
        }

        DeleteAllDto dto = model.getObject();
        Task task = getPagebase().createSimpleTask(OPERATION_COUNT_TASK);
        OperationResult result = new OperationResult(OPERATION_COUNT_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            dto.setOrgUnitCount(getPagebase().getModelService().countObjects(OrgType.class, null, options, task, result));

            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getOrgUnitCount());
        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }

        return createStringResource("deleteAllDialog.label.orgUnitsDelete", dto.getOrgUnitCount()).getString();
    }

    private String createDeleteAccountShadowsMessage(){
        if(!model.getObject().getDeleteAccountShadow()){
            return createStringResource("deleteAllDialog.label.accountShadowsDelete", 0).getString();
        }

        DeleteAllDto dto = model.getObject();
        Task task = getPagebase().createSimpleTask(OPERATION_SEARCH_ITERATIVE_TASK);
        OperationResult result = new OperationResult(OPERATION_SEARCH_ITERATIVE_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        try {
            ObjectFilter filter = EqualFilter.createEqual(ShadowType.F_KIND, ShadowType.class, getPagebase().getPrismContext(), null, ShadowKindType.ACCOUNT);
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            dto.setAccountShadowTypeCount(getPagebase().getModelService().countObjects(ShadowType.class, query, options, task, result));
            dto.setObjectsToDelete(dto.getObjectsToDelete() + dto.getAccountShadowTypeCount());
        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }

        return createStringResource("deleteAllDialog.label.accountShadowsDelete", dto.getAccountShadowTypeCount()).getString();
    }

    private String createDeleteShadowsMessage(final QName focus){
        if(OrgType.COMPLEX_TYPE.equals(focus) && !model.getObject().getDeleteOrgShadow()){
            return createStringResource("deleteAllDialog.label.orgShadowsDelete", 0).getString();
        } else if(RoleType.COMPLEX_TYPE.equals(focus) && !model.getObject().getDeleteRoleShadow()){
            return createStringResource("deleteAllDialog.label.roleShadowsDelete", 0).getString();
        }
        DeleteAllDto dto = model.getObject();
        int count = 0;

        Task task = getPagebase().createSimpleTask(OPERATION_SEARCH_ITERATIVE_TASK);
        final OperationResult result = new OperationResult(OPERATION_SEARCH_ITERATIVE_TASK);

        Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
        GetOperationOptions opt = GetOperationOptions.createRaw();
        options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

        ResultHandler<ShadowType> orgShadowHandler = new ResultHandler<ShadowType>() {
            @Override
            public boolean handle(PrismObject object, OperationResult parentResult) {
                ShadowType shadow = (ShadowType)object.asObjectable();
                String oid = shadow.getResourceRef().getOid();

                if(model.getObject().getResourceFocusMap().containsKey(oid)){
                    addShadowType(model.getObject().getResourceFocusMap().get(oid));
                    return true;
                }

                PrismObject<ResourceType> resource = WebModelUtils.loadObject(ResourceType.class, oid, result, getPagebase());

                if(resource != null && resource.asObjectable() != null){
                    SynchronizationType sync = resource.asObjectable().getSynchronization();

                    for(ObjectSynchronizationType s: sync.getObjectSynchronization()){
                        if(s.getFocusType() != null && focus.getLocalPart().equals(s.getFocusType().getLocalPart())){
                            model.getObject().getResourceFocusMap().put(oid, s.getFocusType().getLocalPart());
                            addShadowType(focus.getLocalPart());
                        }
                    }
                }
                return true;
            }
        };

        try {
            ObjectFilter filter = EqualFilter.createEqual(ShadowType.F_KIND, ShadowType.class, getPagebase().getPrismContext(), null, ShadowKindType.ACCOUNT);
            ObjectQuery query = ObjectQuery.createObjectQuery(NotFilter.createNot(filter));
            getPagebase().getModelService().searchObjectsIterative(ShadowType.class, query, orgShadowHandler, options, task, result);
            if(OrgType.COMPLEX_TYPE.equals(focus)){
                count = dto.getOrgShadowCount();
            } else if(RoleType.COMPLEX_TYPE.equals(focus)){
                count = dto.getRoleShadowCount();
            }

            dto.setObjectsToDelete(dto.getObjectsToDelete() + count);
        } catch (Exception ex) {
            result.computeStatus(getString("deleteAllDialog.message.countSearchProblem"));
            LoggingUtils.logException(LOGGER, getString("deleteAllDialog.message.countSearchProblem"), ex);
        }

        if(OrgType.COMPLEX_TYPE.equals(focus)){
            return createStringResource("deleteAllDialog.label.orgShadowsDelete", count).getString();
        } else if(RoleType.COMPLEX_TYPE.equals(focus)){
            return createStringResource("deleteAllDialog.label.roleShadowsDelete", count).getString();
        } else {
            return null;
        }
    }

    public int getObjectsToDelete(){
        return model.getObject().getObjectsToDelete();
    }

    private void addShadowType(String focus){
        DeleteAllDto dto = model.getObject();
        if(OrgType.COMPLEX_TYPE.getLocalPart().equals(focus)){
            dto.setOrgShadowCount(dto.getOrgShadowCount() + 1);
        } else if(RoleType.COMPLEX_TYPE.getLocalPart().equals(focus)){
            dto.setOrgShadowCount(dto.getRoleShadowCount() + 1);
        }
    }

    private PageBase getPagebase(){
        return (PageBase) getPage();
    }

    public void yesPerformed(AjaxRequestTarget target) {

    }

    public void noPerformed(AjaxRequestTarget target) {
        close(target);
    }
}
