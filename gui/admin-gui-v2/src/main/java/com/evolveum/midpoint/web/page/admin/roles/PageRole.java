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

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.page.admin.dto.DtoUtils;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
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

    public static final String PARAM_ROLE_ID = "roleId";
    private static final String OPERATION_LOAD_ROLE = "pageRole.loadRole";
    private static final String OPERATION_SAVE_ROLE = "pageRole.saveRole";
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
        StringValue roleOid = getPageParameters().get(PARAM_ROLE_ID);
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

            dto = new ObjectViewDto(role.getOid(), DtoUtils.getName(role), role, xml);
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
        Form mainForm = new Form("mainForm");
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
        AceEditor<String> editor = new AceEditor<String>("aceEditor", new PropertyModel<String>(model, "xml"));
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
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
                createStringResource("pageRole.button.save")) {

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

        AjaxLinkButton backButton = new AjaxLinkButton("backButton",
                createStringResource("pageRole.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(PageRoles.class);
            }
        };
        mainForm.add(backButton);
    }

    private boolean isEditing() {
        StringValue roleOid = getPageParameters().get(PARAM_ROLE_ID);
        if (roleOid == null || StringUtils.isEmpty(roleOid.toString())) {
            return false;
        }

        return true;
    }

    private void editPerformed(AjaxRequestTarget target, boolean editable) {
        AceEditor editor = (AceEditor) get("mainForm:aceEditor");

        editor.setReadonly(!editable);
        target.appendJavaScript(editor.createJavascriptEditableRefresh());
    }

    private void savePerformed(AjaxRequestTarget target) {
        ObjectViewDto dto = model.getObject();
        if (StringUtils.isEmpty(dto.getXml())) {
            error("Can't save empty xml as role.");     //todo i18n
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_ROLE);
        try {
            Task task = getTaskManager().createTaskInstance(OPERATION_SAVE_ROLE);
            if (!isEditing()) {
                //we're adding new role
                PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
                PrismObject<RoleType> newRole = domProcessor.parseObject(dto.getXml(), RoleType.class);

                getModelService().addObject(newRole, task, result);
            } else {
                //we're editing existing role
                PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
                PrismObject<RoleType> oldRole = dto.getObject();
                PrismObject<RoleType> newRole = domProcessor.parseObject(dto.getXml(), RoleType.class);

                ObjectDelta<RoleType> delta = oldRole.diff(newRole);
                getModelService().modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), task, result);
            }

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save role.", ex);
        }

        showResult(result);
        if (result.isSuccess()) {
            //todo success message is not shown
            setResponsePage(PageRoles.class);
        }
    }
}
