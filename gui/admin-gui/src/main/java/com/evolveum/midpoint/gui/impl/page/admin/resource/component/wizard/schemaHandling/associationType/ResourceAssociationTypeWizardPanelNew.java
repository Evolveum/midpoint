/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardChoicePanelWithSeparatedCreatePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.ResourceAssociationTypeBasicWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.ResourceAssociationTypeSubjectWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.help.AssociationMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

public class ResourceAssociationTypeWizardPanelNew extends AbstractWizardChoicePanelWithSeparatedCreatePanel<ShadowAssociationTypeDefinitionType> {

    boolean isPanelForDuplicate = false;

    public ResourceAssociationTypeWizardPanelNew(
            String id,
            WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    public void setPanelForDuplicate(boolean panelForDuplicate) {
        isPanelForDuplicate = panelForDuplicate;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (isPanelForDuplicate) {
            addOrReplace(createChoiceFragment(createNewTypeWizard()));
        }
    }

    @Override
    protected ResourceAssociationTypeBasicWizardPanel createNewTypeWizard(
            String id, WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        ResourceAssociationTypeBasicWizardPanel panel = new ResourceAssociationTypeBasicWizardPanel(id, helper);
        panel.setPanelForDuplicate(isPanelForDuplicate);
        return panel;
    }

    @Override
    protected ResourceAssociationTypeWizardChoicePanelNew createTypePreview() {
        return new ResourceAssociationTypeWizardChoicePanelNew(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(ResourceAssociationTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC_ATTRIBUTES -> showResourceObjectTypeBasic(target);
                    case OBJECT_AND_SUBJECT -> showSubjectWizard(target);
                    case MAPPINGS -> showTableForAttributesMappings(target);
                    case CORRELATION -> showCorrelationItemsTable(target);
                    case SYNCHRONIZATION -> showSynchronizationConfigWizard(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeWizardPanelNew.this.onExitPerformed(target);
            }

            @Override
            protected IModel<String> getExitLabel() {
                if (getHelper().getExitLabel() != null) {
                    return getHelper().getExitLabel();
                }
                return super.getExitLabel();
            }
        };
    }

    private void showSubjectWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ResourceAssociationTypeSubjectWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                false))
        );
    }

    private void showTableForAttributesMappings(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new AssociationMappingWizardPanel<>(
                        getIdOfWizardPanel(),
                        createHelper(ItemPath.create(
                                ShadowAssociationTypeDefinitionType.F_SUBJECT
                        ), false)));
    }

    private void showCorrelationItemsTable(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CorrelationWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(synchronizationEvalModel(AssociationSynchronizationExpressionEvaluatorType.F_CORRELATION),
                                false)
                ){
                    @Override
                    protected boolean isAssociationTypeWizardPanel() {
                        return true;
                    }
                });
    }

    private void showSynchronizationConfigWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new SynchronizationWizardPanel<>(
                        getIdOfWizardPanel(),
                        createHelper(synchronizationEvalModel(AssociationSynchronizationExpressionEvaluatorType.F_SYNCHRONIZATION),
                                false)
                )
        );
    }

    private @Nullable <C extends Containerable> IModel<PrismContainerValueWrapper<C>> synchronizationEvalModel(ItemPath suffix) {
        // subject -> association container value
        IModel<PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType>> assocSubjectModel =
                PrismContainerValueWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ItemPath.create(
                                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION));

        PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subject = assocSubjectModel.getObject();

        if (subject == null) {
            return null;
        }

        try {
            // model for the first "inbound mapping" row (GUI limitation - only one mapping is supported)
            // point to associationSynchronization evaluator under that mapping
            PrismContainerWrapper<MappingType> inbound = subject.findContainer(ShadowAssociationDefinitionType.F_INBOUND);
            if (inbound == null || inbound.getValues() == null || inbound.getValues().isEmpty()) {
                return null;
            }

            return PrismContainerValueWrapperModel.fromContainerValueWrapper(
                    () -> inbound.getValues().get(0),
                    ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION, suffix));

        } catch (SchemaException e) {
            throw new RuntimeException("Cannot load inbound association synchronization evaluator", e);
        }
    }

    private void showResourceObjectTypeBasic(AjaxRequestTarget target) {
        ResourceAssociationTypeBasicWizardPanel wizard =
                new ResourceAssociationTypeBasicWizardPanel(getIdOfChoicePanel(), createHelper(true));
        wizard.setShowChoicePanel(false);
        showChoiceFragment(target, wizard);
    }
}
