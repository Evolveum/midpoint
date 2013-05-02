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

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Collection;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author lazyman
 */
public class PageResourceEdit extends PageAdminResources {

    private static final String DOT_CLASS = PageResourceEdit.class.getName() + ".";
    private static final String OPERATION_SAVE_RESOURCE = DOT_CLASS + "saveResource";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_EDIT = "edit";
    private static final String ID_ACE_EDITOR = "aceEditor";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_BACK_BUTTON = "backButton";

    private IModel<ObjectViewDto> model;

    public PageResourceEdit() {
        model = new LoadableModel<ObjectViewDto>(false) {

            @Override
            protected ObjectViewDto load() {
                return loadResource();
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
                    return PageResourceEdit.super.createPageTitleModel().getObject();
                }

                String name = model.getObject().getName();
                return new StringResourceModel("page.title.editResource", PageResourceEdit.this, null, null, name).getString();
            }
        };
    }

    private ObjectViewDto loadResource() {
        if (!isEditing()) {
            return new ObjectViewDto();
        }

        ObjectViewDto dto;
        try {
            PrismObject<ResourceType> resource = loadResource(null);
            PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
            String xml = domProcessor.serializeObjectToString(resource);

            dto = new ObjectViewDto(resource.getOid(), WebMiscUtil.getName(resource), resource, xml);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load resource", ex);
            throw new RestartResponseException(PageResources.class);
        }

        return dto;
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
        mainForm.add(new AjaxCheckBox(ID_EDIT, editable) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                editPerformed(target, editable.getObject());
            }
        });
        AceEditor<String> editor = new AceEditor<String>(ID_ACE_EDITOR, new PropertyModel<String>(model, ObjectViewDto.F_XML));
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
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton(ID_SAVE_BUTTON, ButtonType.POSITIVE,
                createStringResource("PageBase.button.save")) {

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

        AjaxLinkButton backButton = new AjaxLinkButton(ID_BACK_BUTTON, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(PageResources.class);
            }
        };
        mainForm.add(backButton);
    }

    private boolean isEditing() {
        StringValue resourceOid = getPageParameters().get(PARAM_RESOURCE_ID);
        if (resourceOid == null || StringUtils.isEmpty(resourceOid.toString())) {
            return false;
        }

        return true;
    }

    private void editPerformed(AjaxRequestTarget target, boolean editable) {
        AceEditor editor = (AceEditor) get(createComponentPath(ID_MAIN_FORM, ID_ACE_EDITOR));

        editor.setReadonly(!editable);
        target.appendJavaScript(editor.createJavascriptEditableRefresh());
    }

    private void savePerformed(AjaxRequestTarget target) {
        ObjectViewDto dto = model.getObject();
        if (StringUtils.isEmpty(dto.getXml())) {
            error(getString("pageResourceEdit.message.emptyXml"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SAVE_RESOURCE);
        try {
            Task task = createSimpleTask(OPERATION_SAVE_RESOURCE);
            if (!isEditing()) {
                //we're adding new resource
                PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
                PrismObject<ResourceType> newResource = domProcessor.parseObject(dto.getXml(), ResourceType.class);

                ObjectDelta delta = ObjectDelta.createAddDelta(newResource);
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            } else {
                //we're editing existing resource
                PrismDomProcessor domProcessor = getPrismContext().getPrismDomProcessor();
                PrismObject<ResourceType> oldResource = dto.getObject();
                PrismObject<ResourceType> newResource = domProcessor.parseObject(dto.getXml(), ResourceType.class);

                ObjectDelta<ResourceType> delta = oldResource.diff(newResource);

                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), ModelExecuteOptions.createRaw(), task, result);
            }

            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save resource", ex);
            result.recordFatalError("Couldn't save resource.", ex);
        }

        if (WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            setResponsePage(PageResources.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }
}
