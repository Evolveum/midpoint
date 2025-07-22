/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/smartIntegrationDefiningType",
                        matchUrlForSecurity = "/admin/config/smartIntegrationDefiningType")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION) })
public class PageSmartIntegrationDefiningType extends PageAdminConfiguration {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSmartIntegrationDefiningType.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_PREVIOUS = "previous";
    private static final String ID_SUGGEST_CORRELATION_RULES = "suggestCorrelationRules";
    private static final String ID_SUGGEST_MAPPINGS = "suggestMappings";
    private static final String ID_DEFINITION_XML = "definitionXml";
    private static final String ID_ACCEPT_SUGGESTION = "acceptSuggestion";
    private static final String ID_SUGGESTION_XML = "suggestionXml";

    private static final String CLASS_DOT = PageSmartIntegrationDefiningType.class.getName() + ".";
    private static final String OP_SUGGEST_CORRELATION_RULES = CLASS_DOT + "suggestCorrelationRules";
    private static final String OP_SUGGEST_MAPPINGS = CLASS_DOT + "suggestMappings";
    private static final String OP_ACCEPT_SUGGESTION = CLASS_DOT + "acceptSuggestion";
    private static final String OP_SAVE_RESOURCE = CLASS_DOT + "saveResource";

    private final DefinedResource definedResource;
    private final ResourceObjectTypeIdentification typeIdentification;

    private final IModel<String> definitionXmlModel = Model.of();
    private final IModel<String> suggestionXmlModel = Model.of();

    public PageSmartIntegrationDefiningType(ResourceType resource, ResourceObjectTypeIdentification typeIdentification) {
        this.definedResource = new DefinedResource(resource);
        this.typeIdentification = typeIdentification;
        updateDefinitionModel();
    }

    static void navigateTo(
            PageBase sourcePage, ResourceType resource, ResourceObjectTypeIdentification typeIdentification) {
        sourcePage.navigateToNext(
                new PageSmartIntegrationDefiningType(resource, typeIdentification));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        AceEditor definitionXmlEditor = new AceEditor(ID_DEFINITION_XML, definitionXmlModel);
        definitionXmlEditor.setHeight(400);
        definitionXmlEditor.setResizeToMaxHeight(false);
        mainForm.add(definitionXmlEditor);

        AceEditor suggestionXmlEditor = new AceEditor(ID_SUGGESTION_XML, suggestionXmlModel);
        suggestionXmlEditor.setReadonly(true);
        suggestionXmlEditor.setHeight(400);
        suggestionXmlEditor.setResizeToMaxHeight(false);
        mainForm.add(suggestionXmlEditor);

        mainForm.add(new AjaxSubmitButton(ID_PREVIOUS) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_SAVE_RESOURCE) // TODO what if not needed?
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            PageSmartIntegrationTypesSuggestion.navigateTo(
                                    PageSmartIntegrationDefiningType.this,
                                    definedResource.getResourceBean(),
                                    null);
                        });
            }
        });

        mainForm.add(new AjaxSubmitButton(ID_SUGGEST_CORRELATION_RULES) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_SUGGEST_CORRELATION_RULES)
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            var suggestion = getSmartIntegrationService().suggestCorrelation(
                                    getResourceOid(), typeIdentification, getFocusTypeName(), null,
                                    task, result);
                            updateSuggestionXmlModel(suggestion);
                            target.add(PageSmartIntegrationDefiningType.this);
                        });
            }
        });

        mainForm.add(new AjaxSubmitButton(ID_SUGGEST_MAPPINGS) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_SUGGEST_MAPPINGS)
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            var suggestion = getSmartIntegrationService().suggestMappings(
                                    getResourceOid(), typeIdentification, getFocusTypeName(), null, null,
                                    task, result);
                            updateSuggestionXmlModel(suggestion);
                            target.add(PageSmartIntegrationDefiningType.this);
                        });
            }
        });

        mainForm.add(new AjaxSubmitButton(ID_ACCEPT_SUGGESTION) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
            }
        });
    }

    private void updateSuggestionXmlModel(CorrelationSuggestionType suggestion) {
        suggestionXmlModel.setObject(
                Util.serializeRealValue(suggestion, SchemaConstantsGenerated.C_CORRELATION_SUGGESTION));
    }

    private void updateSuggestionXmlModel(MappingsSuggestionType suggestion) {
        suggestionXmlModel.setObject(
                Util.serializeRealValue(suggestion, SchemaConstantsGenerated.C_MAPPINGS_SUGGESTION));
    }

    private String getResourceOid() {
        return definedResource.getOid();
    }

    private void saveResourceToRepositoryAndReload(Task task, OperationResult result) {
        try {
            parseDefinitionAndUpdateInMemoryResource();
            // FIXME replace only the selected type, not the whole schema handling
            // FIXME avoid saving if no changes
            getModelService().executeChanges(
                    List.of(
                            PrismContext.get().deltaFor(ResourceType.class)
                                    .item(ResourceType.F_SCHEMA_HANDLING)
                                    .replace(CloneUtil.clone(definedResource.getSchemaHandling()))
                                    .asObjectDelta(getResourceOid())),
                    null, task, result);
            reloadResourceFromRepository(task, result);
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }

    /** Converts the XML definition from the editor into a {@link SchemaHandlingType} and updates the in-memory resource. */
    private void parseDefinitionAndUpdateInMemoryResource() throws SchemaException {
        ResourceObjectTypeDefinitionType newDefinition;
        var xml = definitionXmlModel.getObject();
        if (StringUtils.isNotBlank(xml)) {
            newDefinition = PrismContext.get()
                    .parserFor(xml)
                    .parseRealValue(ResourceObjectTypeDefinitionType.class);
        } else {
            newDefinition = null;
        }
        var oldDefinition = definedResource.findObjectTypeDefinitionBean(typeIdentification);
        if (!Objects.equals(oldDefinition, newDefinition)) {
            definedResource.replaceObjectTypeDefinition(typeIdentification, newDefinition);
            afterResourceChanged();
        }
    }

    /** To be called after {@link #definedResource} has been changed. */
    private void afterResourceChanged() {
        updateDefinitionModel();
    }

    private void updateDefinitionModel() {
        var definition = definedResource.findObjectTypeDefinitionBean(typeIdentification);
        if (definition != null) {
            definitionXmlModel.setObject(Util.serializeRealValue(definition, SchemaHandlingType.F_OBJECT_TYPE));
        } else {
            definitionXmlModel.setObject(null);
        }
    }

    private void reloadResourceFromRepository(Task task, OperationResult result) {
        try {
            definedResource.replace(
                    getModelService()
                            .getObject(ResourceType.class, getResourceOid(), null, task, result));
            afterResourceChanged();
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }

    private QName getFocusTypeName() {
        var definition = definedResource.findObjectTypeDefinitionBean(typeIdentification);
        var focusDef = definition != null ? definition.getFocus() : null;
        var focusTypeName = focusDef != null ? focusDef.getType() : null;
        return Objects.requireNonNullElse(focusTypeName, FocusType.COMPLEX_TYPE);
    }
}
