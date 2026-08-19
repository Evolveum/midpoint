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
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.ResourceAssociationTypeSubjectObjectWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.AssociationMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceAssociationTypeWizardPanel extends AbstractWizardChoicePanelWithSeparatedCreatePanel<ShadowAssociationTypeDefinitionType> {

    boolean isPanelForDuplicate = false;
    private boolean infoPanelClosed = false;

    private static final String ID_INFO_PANEL = "infoPanel";
    private static final String ID_INFO_TEXT = "infoText";
    private static final String ID_CLOSE = "close";

    public ResourceAssociationTypeWizardPanel(
            String id,
            WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected void initLayout() {
        super.initLayout();
        add(initInfoPanel());
    }

    private @NotNull WebMarkupContainer initInfoPanel() {
        WebMarkupContainer infoPanel = new WebMarkupContainer(ID_INFO_PANEL);
        infoPanel.setOutputMarkupId(true);
        infoPanel.setOutputMarkupPlaceholderTag(true);

        Label infoText = new Label(ID_INFO_TEXT, LoadableDetachableModel.of(this::createMultiRuleInfoMessage));
        infoPanel.add(infoText);

        infoPanel.add(new VisibleBehaviour(() -> hasMultipleProvisioningRules() && !infoPanelClosed));

        infoPanel.add(new AjaxLink<Void>(ID_CLOSE) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                infoPanelClosed = true;
                target.add(infoPanel);
            }
        });
        return infoPanel;
    }

    private String createMultiRuleInfoMessage() {
        String ruleName = getSupportedRuleName();
        return createStringResource(
                "ResourceAssociationTypeWizardPanel.multipleRulesInfo",
                ruleName).getString();
    }

    private @NotNull String getSupportedRuleName() {
        var value = getValueModel().getObject();
        if (value == null) {
            return "";
        }

        var association = Optional.ofNullable(value.getRealValue())
                .map(ShadowAssociationTypeDefinitionType::getSubject)
                .map(ShadowAssociationTypeSubjectDefinitionType::getAssociation)
                .orElse(null);

        if (association == null) {
            return "";
        }

        List<String> supportedRules = new ArrayList<>();

        MappingType inbound = getFirst(association.getInbound());
        if (inbound != null) {
            supportedRules.add(formatRuleLabel("inbound", inbound));
        }

        MappingType outbound = getFirst(association.getOutbound());
        if (outbound != null) {
            supportedRules.add(formatRuleLabel("outbound", outbound));
        }

        return String.join(", ", supportedRules);
    }

    private @NotNull String formatRuleLabel(String direction, @NotNull MappingType mapping) {
        String name = mapping.getName();
        if (name == null || name.isBlank()) {
            name = "unnamed";
        }
        return direction + ": " + name;
    }

    private MappingType getFirst(List<?> list) {
        return (list == null || list.isEmpty()) ? null : (MappingType) list.get(0);
    }

    private boolean hasMultipleProvisioningRules() {
        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> valueWrapper = getValueModel().getObject();
        if (valueWrapper == null || valueWrapper.getRealValue() == null) {
            return false;
        }

        ShadowAssociationTypeDefinitionType associationType = valueWrapper.getRealValue();
        ShadowAssociationTypeSubjectDefinitionType subject = associationType.getSubject();
        if (subject == null || subject.getAssociation() == null) {
            return false;
        }

        ShadowAssociationDefinitionType association = subject.getAssociation();

        int inboundCount = association.getInbound() != null ? association.getInbound().size() : 0;
        int outboundCount = association.getOutbound() != null ? association.getOutbound().size() : 0;

        return inboundCount > 1 || outboundCount > 1;
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
    protected ResourceAssociationTypeWizardChoicePanel createTypePreview() {
        return new ResourceAssociationTypeWizardChoicePanel(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(ResourceAssociationTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC_ATTRIBUTES -> showResourceObjectTypeBasic(target);
                    case OBJECT_AND_SUBJECT -> showObjectSubjectWizard(target);
                    case MAPPINGS -> showTableForAttributesMappings(target);
                    case CORRELATION -> showCorrelationItemsTable(target);
                    case SYNCHRONIZATION -> showSynchronizationConfigWizard(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeWizardPanel.this.onExitPerformed(target);
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

    private void showObjectSubjectWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ResourceAssociationTypeSubjectObjectWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(false))
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
                ) {
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
