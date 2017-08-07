/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import java.util.List;

/**
 * @author lazyman
 *
 * TODO - delete this page when ResourceWizard is fully functional and tested
 */
@PageDescriptor(url = "/admin/resource/edit", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
                label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
                description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL,
                label = "PageResourceEdit.auth.resourceEdit.label",
                description = "PageResourceEdit.auth.resourceEdit.description")})
@Deprecated
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
                return PageBase.createStringResourceStatic(PageResourceEdit.this, "page.title.editResource", name).getString();
//                return new StringResourceModel("page.title.editResource", PageResourceEdit.this, null, null, name).getString();
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
            String xml = getPrismContext().serializeObjectToString(resource, PrismContext.LANG_XML);

            dto = new ObjectViewDto(resource.getOid(), WebComponentUtil.getName(resource), resource, xml);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load resource", ex);
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
        AceEditor editor = new AceEditor(ID_ACE_EDITOR, new PropertyModel<String>(model, ObjectViewDto.F_XML));
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
        AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE_BUTTON,
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

        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new PageResources(false));
            }
        };
        mainForm.add(backButton);
    }

    private boolean isEditing() {
        StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (resourceOid == null || StringUtils.isEmpty(resourceOid.toString())) {
            return false;
        }

        return true;
    }

    private void editPerformed(AjaxRequestTarget target, boolean editable) {
        AceEditor editor = (AceEditor) get(createComponentPath(ID_MAIN_FORM, ID_ACE_EDITOR));

        editor.setReadonly(!editable);
        editor.refreshReadonly(target);
    }

    private void savePerformed(AjaxRequestTarget target) {
        ObjectViewDto dto = model.getObject();
        if (StringUtils.isEmpty(dto.getXml())) {
            error(getString("pageResourceEdit.message.emptyXml"));
            target.add(getFeedbackPanel());
            return;
        }

        Task task = createSimpleTask(OPERATION_SAVE_RESOURCE);
        OperationResult result = task.getResult();
        try {
            Holder<ResourceType> objectHolder = new Holder<>(null);
            validateObject(dto.getXml(), objectHolder, PrismContext.LANG_XML, false, ResourceType.class, result);

            if (result.isAcceptable()) {
                PrismObject<ResourceType> newResource = objectHolder.getValue().asPrismObject();
                updateConnectorRef(newResource, task, result);

                if (!isEditing()) {
                    //we're adding new resource
                    ObjectDelta delta = ObjectDelta.createAddDelta(newResource);
                    getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
                } else {
                    //we're editing existing resource
                    PrismObject<ResourceType> oldResource = dto.getObject();
                    ObjectDelta<ResourceType> delta = oldResource.diff(newResource);

                    getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta),
                            ModelExecuteOptions.createRaw(), task, result);
                }

                result.computeStatus();
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save resource", ex);
            result.recordFatalError("Couldn't save resource.", ex);
        }

        if (WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
            setResponsePage(new PageResources(false));
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    /**
     * Method which attempts to resolve connector reference filter to actual connector (if necessary).
     *
     * @param resource {@link PrismObject} resource
     */
    private void updateConnectorRef(PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        if (resource == null) {
            return;
        }

        PrismReference resourceRef = resource.findReference(ResourceType.F_CONNECTOR_REF);
        if (resourceRef == null || resourceRef.getValue() == null) {
            return;
        }

        PrismReferenceValue refValue = resourceRef.getValue();
        if (StringUtils.isNotEmpty(refValue.getOid())) {
            return;
        }

        if (refValue.getFilter() == null) {
            return;
        }

        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(ConnectorType.class);
        ObjectFilter filter = QueryConvertor.parseFilter(refValue.getFilter(), objDef);

        List<PrismObject<ConnectorType>> connectors = getModelService().searchObjects(ConnectorType.class,
                ObjectQuery.createObjectQuery(filter), null, task, result);
        if (connectors.size() != 1) {
            return;
        }

        PrismObject<ConnectorType> connector = connectors.get(0);
        refValue.setOid(connector.getOid());
        refValue.setTargetType(ConnectorType.COMPLEX_TYPE);

        refValue.setFilter(null);
    }
}
