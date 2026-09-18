/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.AssociationMappingTypeChoicePanelPopup;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.discardDraftMapping;

public final class CorrelationMappingCreationUtil {

    private CorrelationMappingCreationUtil() {
    }

    public static void createMapping(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> correlatorModel,
            @Nullable SerializableConsumer<AjaxRequestTarget> refreshAction,
            @NotNull IModel<PrismContainerValueWrapper<CorrelationItemType>> correlationItemModel) {

        PrismContainerValueWrapper<ItemsSubCorrelatorType> context = correlatorModel.getObject();
        PrismContainerValueWrapper<? extends Containerable> parent =
                context.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        if (parent != null) {
            createRegularMapping(
                    target,
                    pageBase,
                    parent,
                    refreshAction,
                    correlationItemModel);
            return;
        }

        PrismContainerValueWrapper<? extends Containerable> associationParent =
                context.getParentContainerValue(AssociationSynchronizationExpressionEvaluatorType.class);

        if (associationParent != null) {
            AssociationMappingTypeChoicePanelPopup popup =
                    new AssociationMappingTypeChoicePanelPopup(
                            pageBase.getMainPopupBodyId(), pageBase) {

                        @Override
                        protected void onAssociationMappingKindChosen(
                                AjaxRequestTarget target,
                                AssociationMappingKind kind) {
                            createAssociationMapping(
                                    target,
                                    pageBase,
                                    kind,
                                    associationParent,
                                    correlatorModel,
                                    refreshAction,
                                    correlationItemModel);
                        }
                    };

            pageBase.showMainPopup(popup, target);
        }
    }

    private static void createRegularMapping(
            AjaxRequestTarget target,
            PageBase pageBase,
            PrismContainerValueWrapper<? extends Containerable> parent,
            @Nullable SerializableConsumer<AjaxRequestTarget> refreshAction,
            IModel<PrismContainerValueWrapper<CorrelationItemType>> correlationItemModel) {

        PrismContainerValueWrapper<MappingType> newMappingValue =
                MappingUtils.createNewVirtualMappingValue(
                        null,
                        () -> parent,
                        MappingDirection.INBOUND,
                        ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                        AbstractAttributeMappingsDefinitionType.F_REF,
                        pageBase,
                        target);

        if (newMappingValue != null) {
            ensureCorrelationUseMapping(newMappingValue);

            openMappingPopup(
                    target,
                    pageBase,
                    newMappingValue,
                    refreshAction,
                    correlationItemModel);
        }
    }

    /**
     * Sets correlation as the default use for mappings created for correlation purposes.
     */
    private static void ensureCorrelationUseMapping(PrismContainerValueWrapper<MappingType> newMappingValue) {
        try {
            PrismPropertyWrapper<InboundMappingUseType> useProperty =
                    newMappingValue.findProperty(InboundMappingType.F_USE);
            if (useProperty.getValue() != null && useProperty.getValue().getRealValue() == null) {
                useProperty.getValue().setRealValue(InboundMappingUseType.CORRELATION);
            }
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't initialize the inbound mapping use.", e);
        }
    }

    private static void createAssociationMapping(
            AjaxRequestTarget target,
            PageBase pageBase,
            AssociationMappingTypeChoicePanelPopup.AssociationMappingKind kind,
            PrismContainerValueWrapper<? extends Containerable> parent,
            IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> correlatorModel,
            @Nullable SerializableConsumer<AjaxRequestTarget> refreshAction,
            IModel<PrismContainerValueWrapper<CorrelationItemType>> correlationItemModel) {

        ItemName containerName =
                kind == AssociationMappingTypeChoicePanelPopup.AssociationMappingKind.OBJECT_REF
                        ? AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF
                        : AssociationSynchronizationExpressionEvaluatorType.F_ATTRIBUTE;

        MappingDirection direction =
                kind == AssociationMappingTypeChoicePanelPopup.AssociationMappingKind.OBJECT_REF
                        ? MappingDirection.OBJECTS
                        : MappingDirection.ATTRIBUTE;

        PrismContainerValueWrapper<MappingType> newMappingValue =
                MappingUtils.createNewVirtualMappingValue(
                        null,
                        () -> parent,
                        direction,
                        containerName,
                        AbstractAttributeMappingsDefinitionType.F_REF,
                        pageBase,
                        target);

        if (newMappingValue != null) {

            ensureCorrelationUseMapping(newMappingValue);

            openMappingPopup(
                    target,
                    pageBase,
                    newMappingValue,
                    refreshAction,
                    correlationItemModel);
        }
    }

    private static void openMappingPopup(
            AjaxRequestTarget target,
            PageBase pageBase,
            PrismContainerValueWrapper<MappingType> newMappingValue,
            @Nullable SerializableConsumer<AjaxRequestTarget> refreshAction,
            IModel<PrismContainerValueWrapper<CorrelationItemType>> correlationItemModel) {

        CorrelationMappingFormPanel<MappingType> panel =
                new CorrelationMappingFormPanel<>(
                        pageBase.getMainPopupBodyId(),
                        () -> newMappingValue) {

                    @Override
                    protected void onCancel(AjaxRequestTarget target) {
                        discardDraftMapping(pageBase, newMappingValue);

                        if (refreshAction != null) {
                            refreshAction.accept(target);
                        }

                        super.onCancel(target);
                    }

                    @Override
                    protected void performCreateMapping(AjaxRequestTarget target) {
                        updateCorrelationItem(
                                correlationItemModel.getObject(),
                                newMappingValue);

                        if (refreshAction != null) {
                            refreshAction.accept(target);
                        }
                    }
                };

        pageBase.showMainPopup(panel, target);
    }

    private static void updateCorrelationItem(
            @NotNull PrismContainerValueWrapper<CorrelationItemType> correlationItem,
            @NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {

        MappingType mapping = mappingWrapper.getRealValue();

        try {
            PrismPropertyWrapper<ItemPathType> refProperty =
                    correlationItem.findProperty(CorrelationItemType.F_REF);

            if (mapping.getTarget() != null) {
                refProperty.getValue().setRealValue(
                        mapping.getTarget().getPath());
            }

            PrismPropertyWrapper<String> nameProperty =
                    correlationItem.findProperty(CorrelationItemType.F_NAME);

            nameProperty.getValue().setRealValue(mapping.getName());

        } catch (SchemaException e) {
            throw new IllegalStateException(
                    "Couldn't update correlation item from created mapping.", e);
        }
    }
}
