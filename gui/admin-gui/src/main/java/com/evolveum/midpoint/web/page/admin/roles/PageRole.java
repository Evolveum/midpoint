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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

/**
 * @author lazyman
 */
public class PageRole extends PageAdminRoles {

    private static final String DOT_CLASS = PageRole.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE = DOT_CLASS + "loadRole";
    private static final String OPERATION_SAVE_ROLE = DOT_CLASS + "saveRole";

    private static final String ID_MAIN_FORM = "mainForm";

    private IModel<ObjectViewDto> model;

    public PageRole() {
        model = new LoadableModel<ObjectViewDto>(false) {

            @Override
            protected ObjectViewDto load() {
                return loadRole();
            }
        };
        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isEditing()) {
                    return PageRole.super.createPageTitleModel().getObject();
                }

                String name = model.getObject().getName();
                return new StringResourceModel("pageRole.title.editing", PageRole.this, null, null, name).getString();
            }
        };
    }

    private ObjectViewDto loadRole() {
        StringValue roleOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (roleOid == null || StringUtils.isEmpty(roleOid.toString())) {
            return new ObjectViewDto();
        }

        ObjectViewDto dto = null;
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE);
        try {
            Task task = getTaskManager().createTaskInstance(OPERATION_LOAD_ROLE);
            PrismObject<RoleType> role = getModelService().getObject(RoleType.class, roleOid.toString(),
                    null, task, result);

            PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
            String xml = domProcessor.serializeObjectToString(role);

            dto = new ObjectViewDto(role.getOid(), WebMiscUtil.getName(role), role, xml);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError(ex.getMessage(), ex);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        return dto != null ? dto : new ObjectViewDto();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        final IModel<Boolean> editable = new LoadableModel<Boolean>(false) {

            @Override
            protected Boolean load() {
                return !isEditing();
            }
        };
        mainForm.add(new AjaxCheckBox("edit", editable) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                editPerformed(target, editable.getObject());
            }
        });
        AceEditor editor = new AceEditor("aceEditor", new PropertyModel<String>(model, ObjectViewDto.F_XML));
        editor.setReadonly(new LoadableModel<Boolean>(false) {

            @Override
            protected Boolean load() {
                return isEditing();
            }
        });
        mainForm.add(editor);

        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitButton saveButton = new AjaxSubmitButton("saveButton", createStringResource("pageRole.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);

        AjaxButton backButton = new AjaxButton("backButton", createStringResource("pageRole.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(PageRoles.class);
            }
        };
        mainForm.add(backButton);
    }

    private boolean isEditing() {
        StringValue roleOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (roleOid == null || StringUtils.isEmpty(roleOid.toString())) {
            return false;
        }

        return true;
    }

    private void editPerformed(AjaxRequestTarget target, boolean editable) {
        AceEditor editor = (AceEditor) get("mainForm:aceEditor");

        editor.setReadonly(!editable);
        editor.refreshReadonly(target);
    }

    private void savePerformed(AjaxRequestTarget target) {
        ObjectViewDto dto = model.getObject();
        if (StringUtils.isEmpty(dto.getXml())) {
            error(getString("pageRole.message.emptyXml"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_ROLE);
        Holder<PrismObject<RoleType>> objectHolder = new Holder<PrismObject<RoleType>>(null);
        validateObject(dto.getXml(), objectHolder, true, result);
        try {
        	Task task = createSimpleTask(OPERATION_SAVE_ROLE);
            PrismObject<RoleType> newRole = objectHolder.getValue();

            if (!isEditing()) {
                //we're adding new role
                ObjectDelta delta = ObjectDelta.createAddDelta(newRole);
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } else {
                //we're editing existing role
                PrismObject<RoleType> oldRole = dto.getObject();
                ObjectDelta<RoleType> delta = oldRole.diff(newRole);
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save role.", ex);
        }
        result.recomputeStatus();

        showResult(result);
        target.add(getFeedbackPanel());

        if (result.isSuccess()) {
            showResultInSession(result);
            setResponsePage(PageRoles.class);
        }
    }
}
