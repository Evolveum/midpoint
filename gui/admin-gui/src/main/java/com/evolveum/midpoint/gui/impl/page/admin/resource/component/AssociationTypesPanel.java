/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.suggestion.StatusAwareDataFactory;
import com.evolveum.midpoint.gui.impl.factory.duplicateresolver.AssociationDuplicateResolver;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPopupPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
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
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
                this,
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
        getObjectDetailsModels().getPageResource()
                .showAssociationTypeWizardForDuplicate(
                        prismContainerValue,
                        target,
                        containerModel.getObject().getPath(), postSaveHandler);
    }
}
