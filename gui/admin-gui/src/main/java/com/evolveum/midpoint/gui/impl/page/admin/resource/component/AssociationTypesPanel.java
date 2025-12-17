/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.factory.duplicateresolver.AssociationDuplicateResolver;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartAlertGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingAlertDto;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPopupPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.RequestDetailsRecordDto;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.SuggestionsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.createNewItemContainerValueWrapper;

@PanelType(name = "associationTypes")
@PanelInstance(identifier = "associationTypes", applicableForType = ResourceType.class,
        childOf = SchemaHandlingPanel.class,
        display = @PanelDisplay(label = "PageResource.tab.associationTypes", icon = "fa fa-code-compare", order = 20))
public class AssociationTypesPanel extends SchemaHandlingObjectsPanel<ShadowAssociationTypeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationTypesPanel.class);

    private static final String CLASS_DOT = AssociationTypesPanel.class.getName() + ".";

    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";
    private static final String OP_DETERMINE_STATUSES = CLASS_DOT + ".determineStatuses";

    public AssociationTypesPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected ItemPath getTypesContainerPath() {
        return ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_ASSOCIATION_TYPE);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_ASSOCIATION_TYPES;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "AssociationTypesPanel.newObject";
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String>> createColumns() {
        return List.of();
    }

    protected ItemPath getPathForDisplayName() {
        return ShadowAssociationTypeDefinitionType.F_NAME;
    }

    @Override
    protected Class<ShadowAssociationTypeDefinitionType> getSchemaHandlingObjectsType() {
        return ShadowAssociationTypeDefinitionType.class;
    }

    @Override
    protected @NotNull Component createMultiValueListPanel(String id) {
        return new AssociationTablePanel(
                id,
                UserProfileStorage.TableId.PANEL_ASSOCIATION_SCHEMA_HANDLING,
                Model.of(ViewToggle.TILE),
                getSwitchSuggestionModel()) {

            @Override
            protected void onCreateNewObjectPerform(AjaxRequestTarget target) {
                newItemPerformed(null, target, null, false, null);
            }

            @Override
            public void refreshAndDetach(AjaxRequestTarget target) {
                super.refreshAndDetach(target);
                target.add(AssociationTypesPanel.this);
            }

            @Override
            protected List<Component> createNoValueButtonToolbar(String id) {
                List<Component> noValueButtonToolbar = super.createNoValueButtonToolbar(id);
                AjaxIconButton generateButton = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_MAGIC_WAND),
                        () -> isSuggestionExists()
                                ? createStringResource("Suggestion.button.showSuggest").getString()
                                : createStringResource("Suggestion.button.suggest").getString()) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (isSuggestionExists()) {
                            getSwitchSuggestionModel().setObject(Boolean.TRUE);
                            target.add(AssociationTypesPanel.this);
                            refreshAndDetach(target);
                            return;
                        }

                        getSwitchSuggestionModel().setObject(Boolean.TRUE);
                        onSuggestValue(createContainerModel(), target);
                    }
                };
                generateButton.add(new VisibleBehaviour(this::displayNoValuePanel));
                generateButton.add(AttributeModifier.append("class", "btn btn-default text-ai"));
                generateButton.setOutputMarkupId(true);
                generateButton.showTitleAsLabel(true);

                noValueButtonToolbar.add(generateButton);
                return noValueButtonToolbar;
            }

            @Override
            public void newItemPerformedAction(
                    PrismContainerValue<ShadowAssociationTypeDefinitionType> value,
                    AjaxRequestTarget target,
                    AssignmentObjectRelation relationSpec,
                    boolean isDuplicate,
                    StatusInfo<?> statusInfo) {
                onNewValue(value, createContainerModel(), target, isDuplicate, null);
            }

            @Override
            public void performAcceptOperationAction(
                    @NotNull AjaxRequestTarget target,
                    PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value) {
                StatusInfo<?> statusInfo = getStatusInfoObject(value);
                onAcceptValue(() -> value, target);
                performOnDeleteSuggestion(getPageBase(), target, value, statusInfo);
                refreshAndDetach(target);
            }

            @Override
            public void performEditOperationAction(
                    @NotNull AjaxRequestTarget target,
                    PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> value) {
                onEditValue(() -> value, target);
            }

            @Override
            protected void onReviewValue(
                    @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel,
                    AjaxRequestTarget target,
                    StatusInfo<?> statusInfo,
                    SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
                AssociationTypesPanel.this.onReviewValue(valueModel, target, statusInfo, postSaveHandler);
            }

            @Override
            protected StatusAwareDataFactory.SuggestionsModelDto<ShadowAssociationTypeDefinitionType> getSuggestionsModelDto() {
                return AssociationTypesPanel.this.getSuggestionsModelDto();
            }

            @Override
            protected ResourceType getResourceType() {
                return getObjectDetailsModels().getObjectType();
            }
        };
    }

    @Override
    protected void onNewValue(
            PrismContainerValue<ShadowAssociationTypeDefinitionType> value,
            IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> newWrapperModel,
            AjaxRequestTarget target,
            boolean isDuplicate,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
        try {
            CompleteResourceSchema resourceSchema = getObjectDetailsModels().getRefinedSchema();
            List<ShadowReferenceAttributeDefinition> assocDefs = ProvisioningObjectsUtil
                    .getShadowReferenceAttributeDefinitions(resourceSchema);

            if (assocDefs.isEmpty()) {
                ConfirmationPanel confirm = new ConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("AssociationTypesPanel.configureSimulatedAssociation.message")) {
                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        getPageBase().showMainPopup(new SingleContainerPopupPanel<>(
                                getPageBase().getMainPopupBodyId(),
                                PrismContainerWrapperModel.fromContainerWrapper(
                                        getObjectWrapperModel(),
                                        ItemPath.create(
                                                ResourceType.F_CAPABILITIES,
                                                CapabilitiesType.F_CONFIGURED,
                                                CapabilityCollectionType.F_REFERENCES))) {
                            @Override
                            public IModel<String> getTitle() {
                                return () -> WebPrismUtil.getLocalizedDisplayName(getModelObject().getItem());
                            }

                            @Override
                            protected void onSubmitPerformed(AjaxRequestTarget target) {
                                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, getModelObject().getValues().get(0));
                                getPageBase().hideMainPopup(target);
                            }
                        }, target);
                    }

                    @Override
                    protected IModel<String> createYesLabel() {
                        return createStringResource("AssociationTypesPanel.configureSimulatedAssociation.button");
                    }

                    @Override
                    protected IModel<String> createNoLabel() {
                        return createStringResource("Button.cancel");
                    }
                };
                getPageBase().showMainPopup(confirm, target);
            } else {
                if (isDuplicate) {
                    getObjectDetailsModels().getPageResource()
                            .showAssociationTypeWizardForDuplicate(value, target, newWrapperModel.getObject().getPath(), null);
                } else {
                    getObjectDetailsModels().getPageResource()
                            .showAssociationTypeWizard(value, target, newWrapperModel.getObject().getPath());
                }
            }

        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Couldn't load complete resource schema.", e);
        }
    }

    @Override
    protected void onSuggestValue(
            IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> newWrapperModel,
            AjaxRequestTarget target) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        ResourceType resourceType = objectDetailsModels.getObjectType();
        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        runAssociationSuggestionAction(getPageBase(), resourceType.getOid(), target, OP_DEFINE_TYPES, task);
    }

    @Override
    protected void onEditValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
        if (valueModel != null) {
            getObjectDetailsModels().getPageResource().showResourceAssociationTypePreviewWizard(
                    target,
                    valueModel.getObject().getPath());
        }
    }

    @Override
    protected StatusAwareDataFactory.SuggestionsModelDto<ShadowAssociationTypeDefinitionType> getSuggestionsModelDto() {
        PrismContainerWrapperModel<ResourceType, ShadowAssociationTypeDefinitionType> resourceDefWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

        return StatusAwareDataFactory.createAssociationModel(
                getPageBase(),
                getSwitchSuggestionModel(),
                resourceDefWrapper,
                getObjectWrapperObject().getOid());
    }

    @Override
    protected void onReviewValue(
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel,
            AjaxRequestTarget target,
            StatusInfo<?> statusInfo,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
        IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> containerModel = createContainerModel();
        PrismContainerValue<ShadowAssociationTypeDefinitionType> newValue = prepareNewPrismContainerValue(valueModel, containerModel);
        getObjectDetailsModels().getPageResource()
                .showAssociationTypeWizardForDuplicate(
                        newValue,
                        target,
                        containerModel.getObject().getPath(), postSaveHandler);
    }

    protected void onAcceptValue(
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel,
            AjaxRequestTarget target) {
        IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> containerModel = createContainerModel();
        PrismContainerValue<ShadowAssociationTypeDefinitionType> prismContainerValue =
                prepareNewPrismContainerValue(valueModel, containerModel);

        prismContainerValue.setParent(containerModel.getObject().getItem());
        createNewItemContainerValueWrapper(getPageBase(), prismContainerValue, containerModel.getObject(), target);
    }

    protected PrismContainerValue<ShadowAssociationTypeDefinitionType> prepareNewPrismContainerValue(
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel,
            @NotNull IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> containerModel) {
        PrismContainerWrapper<ShadowAssociationTypeDefinitionType> containerWrapper = containerModel.getObject();
        PrismContainer<ShadowAssociationTypeDefinitionType> container = containerWrapper.getItem();

        PrismContainerValue<ShadowAssociationTypeDefinitionType> oldValue = valueModel.getObject().getOldValue();
        WebPrismUtil.cleanupEmptyContainerValue(oldValue);
        oldValue.setParent(container);

        AssociationDuplicateResolver resolver = new AssociationDuplicateResolver();
        ShadowAssociationTypeDefinitionType duplicatedBean =
                resolver.duplicateObjectWithoutCopyOf(oldValue.getValue());

        //noinspection unchecked
        PrismContainerValue<ShadowAssociationTypeDefinitionType> prismContainerValue =
                (PrismContainerValue<ShadowAssociationTypeDefinitionType>)
                        PrismValueCollectionsUtil.cloneCollectionComplex(
                                        CloneStrategy.REUSE,
                                        Collections.singletonList(duplicatedBean.asPrismContainerValue()))
                                .iterator().next();
        WebPrismUtil.cleanupEmptyContainerValue(prismContainerValue);
        return prismContainerValue;
    }

    @Override
    protected @NotNull SmartAlertGeneratingPanel createSmartAlertGeneratingPanel(
            @NotNull String id,
            @NotNull IModel<Boolean> switchToggleModel) {
        SmartAlertGeneratingPanel aiPanel = new SmartAlertGeneratingPanel(id,
                () -> new SmartGeneratingAlertDto(loadSuggestion(getResourceOid()), switchToggleModel, getPageBase())) {
            @Override
            protected void performSuggestOperation(AjaxRequestTarget target) {
                onSuggestValue(createContainerModel(), target);
            }

            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                getTableComponent().refreshAndDetach(target);
            }

            @Override
            protected @NotNull IModel<RequestDetailsRecordDto> getPermissionRecordDtoIModel() {
                return () -> new RequestDetailsRecordDto(null,
                        RequestDetailsRecordDto.initDummyObjectTypePermissionData());
            }

            @Override
            protected void refreshAssociatedComponents(@NotNull AjaxRequestTarget target) {
                AssociationTablePanel smartMappingTable = getTableComponent();
                smartMappingTable.refreshAndDetach(target);
            }
        };

        aiPanel.setOutputMarkupId(true);
        aiPanel.setOutputMarkupPlaceholderTag(true);
        aiPanel.add(new VisibleBehaviour(() -> switchToggleModel.getObject().equals(Boolean.TRUE)));
        return aiPanel;
    }

    public AssociationTablePanel getTableComponent() {
        return (AssociationTablePanel) getTablePanelComponent();
    }

    protected LoadableModel<StatusInfo<?>> loadSuggestion(String resourceOid) {
        Task task = getPageBase().createSimpleTask("loadAssociationTypeSuggestion");
        OperationResult result = task.getResult();
        return new LoadableModel<>() {
            @Override
            protected StatusInfo<AssociationsSuggestionType> load() {
                return loadAssociationTypeSuggestion(
                        getPageBase(), resourceOid, task, result);
            }
        };
    }

    protected String getResourceOid() {
        return getObjectDetailsModels().getObjectType().getOid();
    }

    @Override
    protected SuggestionsStorage.SuggestionType getSuggestionType() {
        return SuggestionsStorage.SuggestionType.ASSOCIATION;
    }
}
