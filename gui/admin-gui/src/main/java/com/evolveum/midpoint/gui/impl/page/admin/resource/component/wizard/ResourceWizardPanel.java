/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.AssociationTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.ResourceAssociationTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page.SmartObjectTypeSuggestionWizardPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationWrapperUtils.processSuggestedContainerValue;

/**
 * @author lskublik
 */
public class ResourceWizardPanel extends AbstractWizardPanel<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceWizardPanel.class);

    public ResourceWizardPanel(String id, WizardPanelHelper<ResourceType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createBasicWizard()));
    }

    private @NotNull BasicResourceWizardPanel createBasicWizard() {
        BasicResourceWizardPanel basicWizard = new BasicResourceWizardPanel(
                getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
                ResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        };
        basicWizard.setOutputMarkupId(true);
        return basicWizard;
    }

    private @NotNull ResourceObjectTypeWizardPanel createObjectTypeWizard(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            @Nullable SerializableConsumer<AjaxRequestTarget> afterSaveAction) {
        var helper = createObjectTypeHelper(valueModel, afterSaveAction);
        return buildResourceObjectTypePanel(helper);
    }

    private @NotNull WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> createObjectTypeHelper(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            @Nullable SerializableConsumer<AjaxRequestTarget> afterSaveAction) {
        return new WizardPanelHelper<>(getAssignmentHolderModel()) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target, t ->
                        showChoiceFragment(t, createObjectTypesTablePanel()));
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                if (afterSaveAction != null) {
                    afterSaveAction.accept(target);
                }

                return getHelper().onSaveObjectPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getDefaultValueModel() {
                return valueModel;
            }
        };
    }

    private @NotNull ResourceObjectTypeWizardPanel buildResourceObjectTypePanel(
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        ResourceObjectTypeWizardPanel wizard = new ResourceObjectTypeWizardPanel(getIdOfChoicePanel(), helper) {

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                super.onExitPerformed(target);
            }
        };
        wizard.setOutputMarkupId(true);
        wizard.setFromNewResourceWizard();
        return wizard;
    }

    private @NotNull SmartObjectTypeSuggestionWizardPanel createSuggestionObjectTypeWizard() {

        WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper = createObjectTypeHelper(null, null);

        SmartObjectTypeSuggestionWizardPanel wizard = new SmartObjectTypeSuggestionWizardPanel(getIdOfChoicePanel(), helper) {
            @Override
            protected void onReviewSelected(
                    @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> newValue,
                    @NotNull ItemPath pathToContainer,
                    @NotNull AjaxRequestTarget target,
                    @Nullable SerializableConsumer<AjaxRequestTarget> afterSaveAction) {
                try {
                    ResourceDetailsModel detailsModel = getHelper().getDetailsModel();
                    PrismContainerWrapper<ResourceObjectTypeDefinitionType> container =
                            detailsModel.getObjectWrapper().findContainer(pathToContainer);
                    WebPrismUtil.cleanupEmptyContainerValue(newValue);

                    PrismContainerValue<ResourceObjectTypeDefinitionType> stored =
                            processSuggestedContainerValue(newValue);

                    PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> newWrapper =
                            WebPrismUtil.addNewValueToContainer(
                                    container,
                                    stored,
                                    getPageBase(),
                                    detailsModel.createWrapperContext());

                    showChoiceFragment(target, createObjectTypeWizard(() -> newWrapper, afterSaveAction));
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't resolve value for path: {}", pathToContainer, e);
                }
            }
        };
        wizard.setOutputMarkupId(true);
        return wizard;
    }

    protected ResourceObjectTypeTableWizardPanel createObjectTypesTablePanel() {
        return new ResourceObjectTypeTableWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onEditValue(
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel wizard = createObjectTypeWizard(valueModel, null);
                wizard.setShowChoicePanel(true);
                showChoiceFragment(target, wizard);
            }

            @Override
            protected void onCreateValue(
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> value,
                    AjaxRequestTarget target, boolean isDuplicate, @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
                showChoiceFragment(target, createObjectTypeWizard(value, postSaveHandler));
            }

            @Override
            protected void onSuggestValue(IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel,
                    AjaxRequestTarget target) {
                showChoiceFragment(target, createSuggestionObjectTypeWizard());
            }

            @Override
            protected OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return getHelper().onSaveObjectPerformed(target);
            }

            @Override
            protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                super.onExitPerformedAfterValidate(target);
                exitToPreview(target);
            }

            @Override
            protected void refreshValueModel() {
                getHelper().refreshValueModel();
            }
        };
    }

    protected AssociationTypeTableWizardPanel createAssociationTypesTablePanel() {
        return new AssociationTypeTableWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel,
                    AjaxRequestTarget target) {
                ResourceAssociationTypeWizardPanel wizard = createAssociationTypeWizard(valueModel, false, null);
                wizard.setShowChoicePanel(true);
                showChoiceFragment(target, wizard);
            }

            @Override
            protected void onCreateValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> value,
                    AjaxRequestTarget target, boolean isDuplicate, @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {
                showChoiceFragment(target, createAssociationTypeWizard(value, isDuplicate, postSaveHandler));
            }

            @Override
            protected OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return getHelper().onSaveObjectPerformed(target);
            }

            @Override
            protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
                super.onExitPerformedAfterValidate(target);
                exitToPreview(target);
            }

            @Override
            protected void refreshValueModel() {
                getHelper().refreshValueModel();
            }
        };
    }

    protected ResourceAssociationTypeWizardPanel createAssociationTypeWizard(
            IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel, boolean isDuplicate,
            @Nullable SerializableConsumer<AjaxRequestTarget> postSaveHandler) {

        WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {

                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        checkDeltasExitPerformed(target,
                                t -> showChoiceFragment(t, createAssociationTypesTablePanel()));
                    }

                    @Override
                    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                        if (postSaveHandler != null) {
                            postSaveHandler.accept(target);
                        }

                        return getHelper().onSaveObjectPerformed(target);
                    }

                    @Override
                    public IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> getDefaultValueModel() {
                        return valueModel;
                    }
                };
        ResourceAssociationTypeWizardPanel wizard = new ResourceAssociationTypeWizardPanel(getIdOfChoicePanel(), helper);
        wizard.setPanelForDuplicate(isDuplicate);
        wizard.setOutputMarkupId(true);
        return wizard;
    }

    private PreviewResourceDataWizardPanel createPreviewResourceDataWizardPanel() {
        return new PreviewResourceDataWizardPanel(getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        };
    }

    protected void exitToPreview(AjaxRequestTarget target) {
        getBreadcrumb().clear();
        SchemaHandlingWizardChoicePanel preview = new SchemaHandlingWizardChoicePanel(
                getIdOfChoicePanel(), getAssignmentHolderModel()) {
            @Override
            protected void onTileClickPerformed(SchemaHandlingWizardChoicePanel.@NotNull PreviewTileType value,
                    AjaxRequestTarget target) {
                switch (value) {
                    case PREVIEW_DATA:
                        showChoiceFragment(target, createPreviewResourceDataWizardPanel());
                        break;
                    case CONFIGURE_OBJECT_TYPES:
                        showChoiceFragment(target, createObjectTypesTablePanel());
                        break;
                    case CONFIGURE_ASSOCIATION_TYPES:
                        showChoiceFragment(target, createAssociationTypesTablePanel());
                        break;

                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getHelper().onExitPerformed(target);
            }
        };
        showChoiceFragment(target, preview);
    }
}
