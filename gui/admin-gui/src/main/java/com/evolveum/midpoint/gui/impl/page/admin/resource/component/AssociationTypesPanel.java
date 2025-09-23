/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.StatusAwareDataProvider;
import com.evolveum.midpoint.gui.impl.factory.duplicateresolver.AssociationDuplicateResolver;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.AssociationDefinitionWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPopupPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceParticipantRole;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadAssociationTypeSuggestionWrappers;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

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
        List<IColumn<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, String>> columns = new ArrayList<>();
        LoadableDetachableModel<PrismContainerDefinition<ShadowAssociationTypeDefinitionType>> defModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerDefinition<ShadowAssociationTypeDefinitionType> load() {
                ComplexTypeDefinition resourceDef =
                        PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                return resourceDef.findContainerDefinition(
                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_ASSOCIATION_TYPE));
            }
        };
        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ShadowAssociationTypeDefinitionType.F_DISPLAY_NAME,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new IconColumn<>(createStringResource("AssociationTypesPanel.column.description")) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> rowModel) {
                return new DisplayType().beginIcon().cssClass(GuiStyleConstants.CLASS_INFO_CIRCLE + " text-info").end();
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> rowModel) {
                ShadowAssociationTypeDefinitionType realValue = rowModel.getObject().getRealValue();
                String description = realValue.getDescription();

                ImagePanel panel = new ImagePanel(componentId, () -> getIconDisplayType(rowModel));
                panel.setIconRole(ImagePanel.IconRole.IMAGE);
                panel.add(AttributeModifier.replace("title", description != null ? description : ""));
                panel.add(new TooltipBehavior());
                cellItem.add(panel);
            }
        });

        columns.add(new LifecycleStateColumn<>(defModel, getPageBase()) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> cellItem,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> rowModel) {
                OperationResultStatusType status = statusFor(rowModel.getObject());
                if (status == null) {
                    super.populateItem(cellItem, componentId, rowModel);
                    return;
                }
                var style = SmartIntegrationUtils.SuggestionUiStyle.from(status);
                Label statusLabel = new Label(componentId, createStringResource(
                        "ResourceObjectTypesPanel.suggestion." + status.value()));
                statusLabel.setOutputMarkupId(true);
                statusLabel.add(AttributeModifier.append("class", style.badgeClass));
                cellItem.add(statusLabel);
            }
        });

        return columns;
    }

    protected ItemPath getPathForDisplayName() {
        return ShadowAssociationTypeDefinitionType.F_NAME;
    }

    @Override
    protected Class<ShadowAssociationTypeDefinitionType> getSchemaHandlingObjectsType() {
        return ShadowAssociationTypeDefinitionType.class;
    }

    @Override
    protected void onNewValue(
            PrismContainerValue<ShadowAssociationTypeDefinitionType> value,
            IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> newWrapperModel,
            AjaxRequestTarget target,
            boolean isDuplicate) {
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
                            .showAssociationTypeWizardForDuplicate(value, target, newWrapperModel.getObject().getPath());
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
            PrismContainerValue<ShadowAssociationTypeDefinitionType> value,
            IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> newWrapperModel,
            AjaxRequestTarget target) {
        ResourceDetailsModel objectDetailsModels = getObjectDetailsModels();
        ResourceType resourceType = objectDetailsModels.getObjectType();
        CompleteResourceSchema resourceSchema;
        try {
            resourceSchema = getObjectDetailsModels().getRefinedSchema();
        } catch (SchemaException | ConfigurationException e) {
            throw new RuntimeException("Couldn't get refined schema for resource " + resourceType.getName(), e);
        }

        List<AssociationDefinitionWrapper> list = new ArrayList<>();

        resourceSchema.getObjectTypeDefinitions().forEach(objectTypeDef ->
                objectTypeDef.getReferenceAttributeDefinitions().forEach(
                        refAttrDef -> {
                            if (!refAttrDef.canRead()
                                    || ShadowReferenceParticipantRole.SUBJECT != refAttrDef.getParticipantRole()
                                    || refAttrDef.getNativeDefinition().isComplexAttribute()) {
                                return;
                            }

                            AssociationDefinitionWrapper wrapper = new AssociationDefinitionWrapper(
                                    objectTypeDef, refAttrDef, resourceSchema);
                            list.add(wrapper);
                        }));

        Collection<ResourceObjectTypeIdentification> subjectTypes = new ArrayList<>();
        Collection<ResourceObjectTypeIdentification> objectTypes = new ArrayList<>();
        list.forEach(def -> {
            List<AssociationDefinitionWrapper.ParticipantWrapper> objects = def.getObjects();
            List<AssociationDefinitionWrapper.ParticipantWrapper> subjects = def.getSubjects();
            objects.stream()
                    .filter(object -> ShadowUtil.isClassified(object.getKind(), object.getIntent()))
                    .map(object -> ResourceObjectTypeIdentification.of(object.getKind(), object.getIntent()))
                    .forEach(objectTypes::add);
            subjects.stream()
                    .filter(subject -> ShadowUtil.isClassified(subject.getKind(), subject.getIntent()))
                    .map(subject -> ResourceObjectTypeIdentification.of(subject.getKind(), subject.getIntent()))
                    .forEach(subjectTypes::add);
        });

        if (subjectTypes.isEmpty() || objectTypes.isEmpty()) {
            getPageBase().warn(getString("AssociationTypesPanel.noAssociationsDefined"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        runAssociationSuggestionAction(getPageBase(), resourceType.getOid(), subjectTypes, objectTypes, target, OP_DEFINE_TYPES, task);
        refreshForm(target);
    }

    @Override
    protected void onEditValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
        if (valueModel != null) {
            getObjectDetailsModels().getPageResource().showResourceAssociationTypePreviewWizard(
                    target,
                    valueModel.getObject().getPath());
        }
    }

    //TODO: set to true when smart association has been implemented
    @Override
    protected boolean allowNoValuePanel() {
        return true;
    }

    @SuppressWarnings("unchecked")
    protected @Nullable StatusInfo<AssociationsSuggestionType> getStatusInfo(PrismContainerValueWrapper<?> value) {
        StatusInfo<?> statusInfo = super.getStatusInfo(value);
        if (statusInfo != null) {
            return (StatusInfo<AssociationsSuggestionType>) statusInfo;
        }
        return null;
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> createProvider() {
        PrismContainerWrapperModel<ResourceType, ShadowAssociationTypeDefinitionType> resourceDefWrapper =
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());

        final Map<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>, StatusInfo<AssociationsSuggestionType>>
                suggestionsIndex = new HashMap<>();

        LoadableModel<List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>>> containerModel =
                new LoadableModel<>() {
                    @Override
                    protected @NotNull List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> load() {
                        List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> out = new ArrayList<>();

                        suggestionsIndex.clear();
                        if (Boolean.TRUE.equals(getSwitchSuggestionModel().getObject())) {
                            final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
                            final OperationResult result = task.getResult();

                            final String resourceOid = getObjectDetailsModels().getObjectType().getOid();

                            SmartIntegrationStatusInfoUtils.@NotNull AssociationTypeSuggestionProviderResult suggestions =
                                    loadAssociationTypeSuggestionWrappers(getPageBase(), resourceOid, task, result);
                            out.addAll(suggestions.wrappers());
                            suggestionsIndex.putAll(suggestions.suggestionByWrapper());
                        }

                        List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> resource = resourceDefWrapper
                                .getObject().getValues();
                        if (resource != null) {
                            out.addAll(resource);
                        }
                        return out;
                    }
                };

        String resourceOid = getObjectWrapperObject().getOid();
        return new StatusAwareDataProvider<>(this, resourceOid, Model.of(), containerModel, suggestionsIndex::get);
    }

    @Override
    protected boolean hasNoValues() {
        return Objects.requireNonNull(getStatusAwareProvider()).size() == 0;
    }

    @Override
    protected boolean isToggleSuggestionVisible() {
        final Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        final OperationResult result = task.getResult();

        final String resourceOid = getObjectDetailsModels().getObjectType().getOid();

        SmartIntegrationStatusInfoUtils.@NotNull AssociationTypeSuggestionProviderResult suggestions = loadAssociationTypeSuggestionWrappers(
                getPageBase(), resourceOid, task, result);

        return !suggestions.wrappers().isEmpty();
    }

    @Override
    protected boolean performOnDeleteSuggestion(AjaxRequestTarget target, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> valueWrapper) {
        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUSES);
        OperationResult result = task.getResult();

        @Nullable StatusInfo<AssociationsSuggestionType> statusInfo = getStatusInfo(valueWrapper);
        if (statusInfo == null) {
            return false;
        }
        PrismContainerValueWrapper<AssociationSuggestionType> parentContainerValue = valueWrapper
                .getParentContainerValue(AssociationSuggestionType.class);
        //TODO move it into schemaHandling
        SmartIntegrationUtils.removeAssociationTypeSuggestionNew(
                getPageBase(),
                statusInfo,
                parentContainerValue.getRealValue(),
                task,
                result);
        target.add(getPageBase().getFeedbackPanel());
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void performOnReview(@NotNull AjaxRequestTarget target, @NotNull PrismContainerValueWrapper<?> valueWrapper) {
        IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> containerModel = createContainerModel();
        PrismContainerWrapper<ShadowAssociationTypeDefinitionType> containerWrapper = containerModel.getObject();
        PrismContainer<ShadowAssociationTypeDefinitionType> container = containerWrapper.getItem();

        PrismContainerValue<ShadowAssociationTypeDefinitionType> oldValue = valueWrapper.getOldValue();
        WebPrismUtil.cleanupEmptyContainerValue(oldValue);
        oldValue.setParent(container);

        AssociationDuplicateResolver resolver = new AssociationDuplicateResolver();
        ShadowAssociationTypeDefinitionType duplicatedBean =
                resolver.duplicateObjectWithoutCopyOf(oldValue.getValue());

        PrismContainerValue<ShadowAssociationTypeDefinitionType> prismContainerValue =
                (PrismContainerValue<ShadowAssociationTypeDefinitionType>)
                        PrismValueCollectionsUtil.cloneCollectionComplex(
                                        CloneStrategy.REUSE,
                                        Collections.singletonList(duplicatedBean.asPrismContainerValue()))
                                .iterator().next();

        WebPrismUtil.cleanupEmptyContainerValue(prismContainerValue);
        IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> containerModel2 = createContainerModel();
        prismContainerValue.setParent(containerModel2.getObject().getItem());

        refreshForm(target);
        //TODO: temporary solution
        performOnDeleteSuggestion(target, (PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>) valueWrapper);

        //TODO problem with ref attribute modification (try using review, change ref, review again)
        getObjectDetailsModels().getPageResource()
                .showAssociationTypeWizardForDuplicate(
                        prismContainerValue,
                        target,
                        containerModel2.getObject().getPath());

    }
}
