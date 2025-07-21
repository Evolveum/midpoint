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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/smartIntegrationDefiningType",
                        matchUrlForSecurity = "/admin/config/smartIntegrationDefiningType")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION) })
public class PageSmartIntegrationTypesSuggestion extends PageAdminConfiguration {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSmartIntegrationTypesSuggestion.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ACCEPT_SUGGESTION = "acceptSuggestion";
    private static final String ID_SELECTED_SUGGESTION = "selectedSuggestion";
    private static final String ID_SUGGEST_FOCUS_TYPE = "suggestFocusType";
    private static final String ID_SELECTED_OBJECT_TYPE = "selectedObjectType";
    private static final String ID_PREVIOUS = "previous";
    private static final String ID_SAVE = "save";
    private static final String ID_RESET = "reset";
    private static final String ID_NEXT = "next";
    private static final String ID_SUGGESTION_XML = "suggestionXml";
    private static final String ID_DEFINITION_XML = "definitionXml";

    private static final String CLASS_DOT = PageSmartIntegrationTypesSuggestion.class.getName() + ".";
    private static final String OP_RESET = CLASS_DOT + "reset";
    private static final String OP_SUGGEST_FOCUS_TYPE = CLASS_DOT + "suggestFocusType";
    private static final String OP_SAVE_RESOURCE = CLASS_DOT + "saveResource";

    private ResourceType resource;
    private final ObjectTypesSuggestionType suggestion;

    private final List<? extends ResourceObjectTypeIdentification> suggestedObjectTypes;

    private final IModel<ResourceObjectTypeIdentification> selectedSuggestionModel = Model.of();
    private final IModel<ResourceObjectTypeIdentification> selectedObjectTypeModel = Model.of();
    private final LoadableModel<List<ResourceObjectTypeIdentification>> definedObjectTypesModel;
    private final IModel<String> suggestionModel;
    private final IModel<String> definitionModel = Model.of();

    private PageSmartIntegrationTypesSuggestion(ResourceType resource, @Nullable ObjectTypesSuggestionType suggestion) {
        this.resource = resource;
        this.suggestion = Objects.requireNonNullElseGet(suggestion, ObjectTypesSuggestionType::new);
        this.suggestedObjectTypes = this.suggestion.getObjectType().stream()
                .map(t -> ResourceObjectTypeIdentification.of(t.getIdentification()))
                .toList();
        this.suggestionModel = Model.of(serializeRealValue(this.suggestion, SchemaConstantsGenerated.C_OBJECT_TYPES_SUGGESTION));
        updateDefinitionModel();

        definedObjectTypesModel = LoadableModel.create(
                () -> {
                    try {
                        return Resource.of(this.resource.asPrismObject())
                                .getCompleteSchemaRequired()
                                .getObjectTypeDefinitions()
                                .stream()
                                .map(def -> def.getTypeIdentification())
                                .toList();
                    } catch (Exception e) {
                        throw SystemException.unexpected(e);
                    }
                }, true);
    }

    private static String serializeRealValue(Containerable suggestion, ItemName root) {
        try {
            return PrismContext.get().xmlSerializer().serializeRealValue(suggestion, root);
        } catch (SchemaException e) {
            return "Error serializing value: " + e.getMessage();
        }
    }

    private void updateDefinitionModel() {
        var schemaHandling = resource.getSchemaHandling();
        if (schemaHandling != null) {
            definitionModel.setObject(serializeRealValue(schemaHandling, ResourceType.F_SCHEMA_HANDLING));
        } else {
            definitionModel.setObject(null);
        }
    }

    static void navigateTo(PageBase sourcePage, ResourceType resource, ObjectTypesSuggestionType suggestion) {
        sourcePage.navigateToNext(
                new PageSmartIntegrationTypesSuggestion(resource, suggestion));
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

        mainForm.add(new AjaxSubmitButton(ID_ACCEPT_SUGGESTION) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                var selectedTypeId = selectedSuggestionModel.getObject();
                if (selectedTypeId == null) {
                    return;
                }
                taskAwareExecutor(target, OP_SUGGEST_FOCUS_TYPE)
                        .runVoid((task, result) -> {
                            parseDefinitionAndUpdateInMemoryResource();
                            replaceObjectTypeDefinitionBySuggestedOne(selectedTypeId);

                            target.add(PageSmartIntegrationTypesSuggestion.this);
                        });
            }
        });

        mainForm.add(new DropDownChoicePanel<>(
                ID_SELECTED_SUGGESTION,
                selectedSuggestionModel,
                Model.ofList(suggestedObjectTypes)));

        mainForm.add(new AjaxSubmitButton(ID_SUGGEST_FOCUS_TYPE) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                var selectedTypeId = selectedObjectTypeModel.getObject();
                if (selectedTypeId == null) {
                    return;
                }
                taskAwareExecutor(target, OP_SUGGEST_FOCUS_TYPE)
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            var suggestedFocusType = getSmartIntegrationService().suggestFocusType(
                                    getResourceOid(), selectedTypeId, task, result);
                            var bean = stateNonNull(
                                    findObjectTypeDefinitionBean(selectedTypeId),
                                    "No bean found for type: %s", selectedTypeId);
                            var focus = bean.getFocus();
                            if (focus == null) {
                                bean.setFocus(new ResourceObjectFocusSpecificationType());
                            }
                            bean.getFocus().setType(suggestedFocusType);
                            afterResourceChanged();
                            target.add(PageSmartIntegrationTypesSuggestion.this);
                        });
            }
        });

        mainForm.add(new DropDownChoicePanel<>(
                ID_SELECTED_OBJECT_TYPE,
                selectedObjectTypeModel,
                definedObjectTypesModel));

        mainForm.add(new AjaxSubmitButton(ID_PREVIOUS) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_SAVE_RESOURCE)
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            PageSmartIntegrationDefiningTypes.navigateTo(
                                    PageSmartIntegrationTypesSuggestion.this, getResourceOid());
                        });
            }
        });
        mainForm.add(new AjaxSubmitButton(ID_SAVE) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_SAVE_RESOURCE)
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            target.add(PageSmartIntegrationTypesSuggestion.this);
                        });
            }
        });
        mainForm.add(new AjaxSubmitButton(ID_RESET) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_RESET)
                        .runVoid((task, result) -> {
                            reloadResourceFromRepository(task, result);
                            target.add(PageSmartIntegrationTypesSuggestion.this);
                        });
            }
        });
        mainForm.add(new AjaxSubmitButton(ID_NEXT) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                taskAwareExecutor(target, OP_SAVE_RESOURCE)
                        .runVoid((task, result) -> {
                            saveResourceToRepositoryAndReload(task, result);
                            target.add(PageSmartIntegrationTypesSuggestion.this);
                        });
            }
        });

        AceEditor suggestionEditor = new AceEditor(ID_SUGGESTION_XML, suggestionModel);
        suggestionEditor.setModeForDataLanguage(PrismContext.LANG_XML);
        suggestionEditor.setReadonly(true);
        suggestionEditor.setHeight(400);
        suggestionEditor.setResizeToMaxHeight(false);
        mainForm.add(suggestionEditor);

        AceEditor definitionEditor = new AceEditor(ID_DEFINITION_XML, definitionModel);
        definitionEditor.setModeForDataLanguage(PrismContext.LANG_XML);
        definitionEditor.setHeight(400);
        definitionEditor.setResizeToMaxHeight(false);
        mainForm.add(definitionEditor);
    }

    private void replaceObjectTypeDefinitionBySuggestedOne(ResourceObjectTypeIdentification selectedTypeId) {
        var bean = findObjectTypeDefinitionBean(selectedTypeId);
        if (bean != null) {
            resource.getSchemaHandling().getObjectType().remove(bean);
        }
        if (resource.getSchemaHandling() == null) {
            resource.setSchemaHandling(new SchemaHandlingType());
        }
        var selectedTypeSuggestion = findObjectTypeSuggestion(selectedTypeId);
        resource.getSchemaHandling().getObjectType().add(new ResourceObjectTypeDefinitionType()
                .kind(selectedTypeSuggestion.getIdentification().getKind())
                .intent(selectedTypeSuggestion.getIdentification().getIntent())
                .delineation(selectedTypeSuggestion.getDelineation().clone()));
        afterResourceChanged();
    }

    /** To be called after {@link #resource} has been changed. */
    private void afterResourceChanged() {
        ResourceSchemaFactory.deleteCachedSchemas(resource.asPrismObject());
        updateDefinitionModel();
        definedObjectTypesModel.reset();
    }

    private void reloadResourceFromRepository(Task task, OperationResult result) {
        try {
            resource = getModelService()
                    .getObject(ResourceType.class, getResourceOid(), null, task, result)
                    .asObjectable();
            afterResourceChanged();
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }

    private void saveResourceToRepositoryAndReload(Task task, OperationResult result) {
        try {
            parseDefinitionAndUpdateInMemoryResource();
            getModelService().executeChanges(
                    List.of(
                            PrismContext.get().deltaFor(ResourceType.class)
                                    .item(ResourceType.F_SCHEMA_HANDLING)
                                    .replace(CloneUtil.clone(resource.getSchemaHandling()))
                                    .asObjectDelta(getResourceOid())),
                    null, task, result);
            reloadResourceFromRepository(task, result);
        } catch (Exception e) {
            throw SystemException.unexpected(e);
        }
    }

    /** Converts the XML definition from the editor into a {@link SchemaHandlingType} and updates the in-memory resource. */
    private void parseDefinitionAndUpdateInMemoryResource() throws SchemaException {
        SchemaHandlingType schemaHandling;
        var xml = definitionModel.getObject();
        if (StringUtils.isNotBlank(xml)) {
            schemaHandling = PrismContext.get()
                    .parserFor(xml)
                    .parseRealValue(SchemaHandlingType.class);
        } else {
            schemaHandling = null;
        }
        if (!Objects.equals(resource.getSchemaHandling(), schemaHandling)) {
            resource.setSchemaHandling(schemaHandling);
            afterResourceChanged();
        }
    }

    /** Returns live (i.e. directly updatable) bean. */
    private @Nullable ResourceObjectTypeDefinitionType findObjectTypeDefinitionBean(ResourceObjectTypeIdentification typeId) {
        var schemaHandling = resource.getSchemaHandling();
        if (schemaHandling == null) {
            return null;
        }
        for (ResourceObjectTypeDefinitionType definitionBean : schemaHandling.getObjectType()) {
            if (ResourceObjectTypeDefinitionTypeUtil.matches(definitionBean, typeId.getKind(), typeId.getIntent())) {
                return definitionBean;
            }
        }
        return null;
    }

    private String getResourceOid() {
        return resource.getOid();
    }

    /**
     * Finds the {@link ObjectTypeSuggestionType} in the suggestion list ({@link #suggestion}) that matches the provided type ID.
     * The type ID was derived from the suggestion list, so it should match one of the suggestions.
     */
    private ObjectTypeSuggestionType findObjectTypeSuggestion(ResourceObjectTypeIdentification typeId) {
        return suggestion.getObjectType().stream()
                .filter(t -> ResourceObjectTypeIdentification.of(t.getIdentification()).equals(typeId))
                .findFirst()
                .orElseThrow(() ->
                        new SystemException("Selected suggestion not found in the list of suggestions: " + typeId));
    }
}
