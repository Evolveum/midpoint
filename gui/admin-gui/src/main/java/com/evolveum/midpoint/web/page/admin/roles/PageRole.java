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
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.roles.dto.RoleDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *  @author shood
 * */
@PageDescriptor(url = "/admin/role", encoder = OnePageParameterEncoder.class, action = {
        PageAdminRoles.AUTHORIZATION_ROLE_ALL,
        AuthorizationConstants.NS_AUTHORIZATION + "#role"})
public class PageRole extends PageAdminRoles{

    private static final String DOT_CLASS = PageRoleOld.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE = DOT_CLASS + "loadRole";
    private static final String OPERATION_SAVE_ROLE = DOT_CLASS + "saveRole";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BUTTON_BACK = "backButton";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ROLE_TYPE = "roleType";
    private static final String ID_REQUESTABLE = "requestable";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_ADMIN_STATUS = "adminStatus";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private IModel<RoleDto> model;

    public PageRole(){

        model = new LoadableModel<RoleDto>() {

            @Override
            protected RoleDto load() {
                return loadRole();
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel(){
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isEditing()){
                    return createStringResource("PageRoleEditor.title.newRole").getObject();
                }

                String roleName = model.getObject().getName();
                return new StringResourceModel("PageRoleEditor.title.editingRole", PageRole.this, null, null, roleName).getString();
            }
        };
    }

    private RoleDto loadRole(){
        StringValue roleOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (roleOid == null || StringUtils.isEmpty(roleOid.toString())) {
            return new RoleDto();
        }

        RoleDto dto = null;
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE);

        try{
            Task task = getTaskManager().createTaskInstance(OPERATION_LOAD_ROLE);
            PrismObject<RoleType> rolePrism = getModelService().getObject(RoleType.class, roleOid.toString(),
                    null, task, result);

            RoleType role = rolePrism.asObjectable();

            dto = new RoleDto(WebMiscUtil.getOrigStringFromPoly(role.getName()), role.getDescription(), role.getRoleType(), role.isRequestable(),
                    role.getActivation().getValidFrom(), role.getActivation().getValidTo(), role.getActivation().getAdministrativeStatus());

            result.recordSuccess();
        } catch (Exception e){
            result.recordFatalError(e.getMessage(), e);
        }

        if(!result.isSuccess()){
            showResult(result);
        }

        if(dto != null){
            return dto;
        } else{
            return new RoleDto();
        }
    }

    private void initLayout(){
        Form form = new Form(ID_MAIN_FORM);
        add(form);

        TextFormGroup name = new TextFormGroup(ID_NAME, new PropertyModel<String>(model, RoleDto.F_NAME),
                createStringResource("PageRoleEditor.label.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        form.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PropertyModel<String>(model, RoleDto.F_DESCRIPTION),
                createStringResource("PageRoleEditor.label.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(description);

        TextFormGroup roleType = new TextFormGroup(ID_ROLE_TYPE, new PropertyModel<String>(model, RoleDto.F_TYPE),
                createStringResource("PageRoleEditor.label.type"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(roleType);

        CheckFormGroup requestable = new CheckFormGroup(ID_REQUESTABLE, new PropertyModel<Boolean>(model, RoleDto.F_REQUESTABLE),
                createStringResource("PageRoleEditor.label.requestable"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(requestable);

        IModel choices = WebMiscUtil.createReadonlyModelFromEnum(ActivationStatusType.class);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
        DropDownFormGroup adminStatus = new DropDownFormGroup(ID_ADMIN_STATUS, new PropertyModel(model, RoleDto.F_ADMIN_STATUS),
                choices, renderer, createStringResource("ActivationType.administrativeStatus"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(adminStatus);

        DateFormGroup validFrom = new DateFormGroup(ID_DATE_FROM, new PropertyModel<XMLGregorianCalendar>(model, RoleDto.F_FROM),
                createStringResource("ActivationType.validFrom"),ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(validFrom);

        DateFormGroup validTo = new DateFormGroup(ID_DATE_TO, new PropertyModel<XMLGregorianCalendar>(model, RoleDto.F_TO),
                createStringResource("ActivationType.validTo"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(validTo);

        initButtons(form);
    }

    private void initButtons(Form form){
        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(form);
                target.add(getFeedbackPanel());
            }
        };
        form.add(save);

        AjaxSubmitButton back = new AjaxSubmitButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                backPerformed(target);
            }
        };
        form.add(back);
    }









    private boolean isEditing() {
        StringValue roleOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (roleOid == null || StringUtils.isEmpty(roleOid.toString())) {
            return false;
        }
        return true;
    }

    private void savePerformed(AjaxRequestTarget target){

    }

    private void backPerformed(AjaxRequestTarget target){
        setResponsePage(PageRoles.class);
    }

}
