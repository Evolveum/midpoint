/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.help;

import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.TabSeparatedTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@PanelType(name = "rw-association-mappings")
@PanelInstance(identifier = "rw-association-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AssociationMappingsTableWizardPanel.inboundTable", icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(identifier = "rw-association-outbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AssociationMappingsTableWizardPanel.outboundTable", icon = "fa fa-arrow-right-from-bracket"))
public abstract class AssociationMappingsTableWizardPanel<C extends Containerable> extends AbstractResourceWizardBasicPanel<C> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationMappingsTableWizardPanel.class);

    private static final String ID_TAB_TABLE = "tabTable";

    private final MappingDirection initialTab;
    boolean isInboundTabSelected = true;

    public AssociationMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper,
            MappingDirection initialTab) {
        super(id, superHelper);
        this.initialTab = initialTab;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createInboundObjectTableTab());
        tabs.add(createInboundAttributeTableTab());
        tabs.add(createOutboundObjectTableTab());
        tabs.add(createOutboundAttributeTableTab());

        TabSeparatedTabbedPanel<ITab> tabPanel = new TabSeparatedTabbedPanel<>(ID_TAB_TABLE, tabs) {
            @Override
            protected void onAjaxUpdate(@NotNull Optional<AjaxRequestTarget> optional) {
                optional.ifPresent(target -> target.add(getButtonsContainer()));
            }

            @Override
            protected void onClickTabPerformed(int index, @NotNull Optional<AjaxRequestTarget> target) {
                isInboundTabSelected = index == 0;
                if (getTable().isValidFormComponents()) {
                    super.onClickTabPerformed(index, target);
                }
            }
        };

        switchTabs(tabPanel);

        tabPanel.setOutputMarkupId(true);
        add(tabPanel);
    }

    private void switchTabs(TabSeparatedTabbedPanel<ITab> tabPanel) {
        switch (initialTab) {
            case INBOUND:
                tabPanel.setSelectedTab(0);
                break;
            case OUTBOUND:
                tabPanel.setSelectedTab(3);
                break;
        }
    }

    private @NotNull IModel<PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType>> inboundEvalModel() {
        // subject -> association container value
        IModel<PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType>> assocSubjectModel =
                PrismContainerValueWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ItemPath.create(ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION));

        PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subject = assocSubjectModel.getObject();

        try {
            // provisioning rule inbound container
            PrismContainerWrapper<MappingType> inbound = subject.findContainer(ShadowAssociationDefinitionType.F_INBOUND);

            if (inbound.getValues().isEmpty()) {
                PrismContainerValue<MappingType> newValue = inbound.getItem().createNewValue();

                // initialize evaluator in expression
                ExpressionType expression = newValue.asContainerable().beginExpression();
                ExpressionUtil.updateAssociationSynchronizationExpressionValue(
                        expression,
                        new AssociationSynchronizationExpressionEvaluatorType());

                PrismContainerValueWrapper<MappingType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                        inbound,
                        newValue,
                        getPageBase(),
                        getAssignmentHolderDetailsModel().createWrapperContext());

                inbound.getValues().add(valueWrapper);
            }

            // model for the first "inbound mapping" row (GUI limitation - only one mapping is supported)
            // point to associationSynchronization evaluator under that mapping
            return PrismContainerValueWrapperModel.fromContainerValueWrapper(
                    () -> inbound.getValues().get(0),
                    ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION));

        } catch (SchemaException e) {
            throw new RuntimeException("Cannot load inbound association synchronization evaluator", e);
        }
    }

    private @NotNull IModel<PrismContainerValueWrapper<AssociationSynchronizationExpressionEvaluatorType>> outboundEvalModel() {
        // subject -> association container value
        IModel<PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType>> assocSubjectModel =
                PrismContainerValueWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ItemPath.create(ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION));

        PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subject = assocSubjectModel.getObject();

        try {
            // provisioning rule outbound container
            PrismContainerWrapper<MappingType> outbound = subject.findContainer(ShadowAssociationDefinitionType.F_OUTBOUND);

            if (outbound.getValues().isEmpty()) {
                PrismContainerValue<MappingType> newValue = outbound.getItem().createNewValue();

                // initialize evaluator in expression
                newValue.asContainerable().beginExpression();
                ExpressionUtil.updateAssociationConstructionExpressionValue(
                        newValue.asContainerable().getExpression(),
                        new AssociationConstructionExpressionEvaluatorType());

                PrismContainerValueWrapper<MappingType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                        outbound,
                        newValue,
                        getPageBase(),
                        getAssignmentHolderDetailsModel().createWrapperContext());

                outbound.getValues().add(valueWrapper);
            }

            // model for the first "outbound mapping" row (GUI limitation - only one mapping is supported)
            // point to associationSynchronization evaluator under that mapping
            return PrismContainerValueWrapperModel.fromContainerValueWrapper(
                    () -> outbound.getValues().get(0),
                    ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION));

        } catch (SchemaException e) {
            throw new RuntimeException("Cannot load inbound association synchronization evaluator", e);
        }
    }

    private boolean isAttributeVisible() {
        try {
            ShadowAssociationDefinition assocDef = AssociationChildWrapperUtil.getShadowAssociationDefinition(
                    getAssignmentHolderDetailsModel().getRefinedSchema(), getValueModel().getObject());
            if (assocDef == null) {
                return false;
            }
            return assocDef.isComplex();
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot load resource schema", e);
            return false;
        }
    }

    public TabbedPanel<ITab> getTabPanel() {
        //noinspection unchecked
        return ((TabbedPanel<ITab>) get(ID_TAB_TABLE));
    }

    @SuppressWarnings("rawtypes")
    protected AssociationSmartAttributeMappingsTable getTable() {
        TabbedPanel<ITab> tabPanel = getTabPanel();
        return (AssociationSmartAttributeMappingsTable) tabPanel.get(TabbedPanel.TAB_PANEL_ID);
    }

    @Override
    protected String getSaveLabelKey() {
        return "InboundMappingsTableWizardPanel.saveButton";
    }

    protected abstract void inEditInboundAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab);

    protected abstract void inEditOutboundAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("InboundMappingsTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("InboundMappingsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("InboundMappingsTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected ContainerPanelConfigurationType getConfiguration(String panelType) {
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                panelType);
    }

    private ITab createInboundObjectTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("AssociationMappingsTableWizardPanel.inbound.objectsTable")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationSmartAttributeMappingsTable<>(panelId, Model.of(MappingDirection.OBJECTS), Model.of(false), inboundEvalModel(), null) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF;
                    }

                    @Override
                    protected boolean isInboundRelated() {
                        return true;
                    }

                    @Override
                    protected void performOnEditMapping(@NotNull AjaxRequestTarget target, @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        inEditInboundAttributeValue(rowModel, target, initialTab);
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-cube");
            }
        };
    }

    @Contract(" -> new")
    private @NotNull ITab createInboundAttributeTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("AssociationMappingsTableWizardPanel.inbound.attributeTable"),
                new VisibleBehaviour(this::isAttributeVisible)) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationSmartAttributeMappingsTable<>(panelId, Model.of(MappingDirection.ATTRIBUTE), Model.of(false), inboundEvalModel(), null) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AssociationSynchronizationExpressionEvaluatorType.F_ATTRIBUTE;
                    }

                    @Override
                    protected boolean isInboundRelated() {
                        return true;
                    }

                    @Override
                    protected void performOnEditMapping(@NotNull AjaxRequestTarget target, @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        inEditInboundAttributeValue(rowModel, target, initialTab);
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-cube");
            }
        };
    }

    private ITab createOutboundObjectTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("AssociationMappingsTableWizardPanel.outbound.objectsTable")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationSmartAttributeMappingsTable<>(panelId, Model.of(MappingDirection.OBJECTS), Model.of(false), outboundEvalModel(), null) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF;
                    }

                    @Override
                    protected boolean isInboundRelated() {
                        return false;
                    }

                    @Override
                    protected void performOnEditMapping(@NotNull AjaxRequestTarget target, @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        inEditOutboundAttributeValue(rowModel, target, initialTab);
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-cube");
            }
        };
    }

    @Contract(" -> new")
    private @NotNull ITab createOutboundAttributeTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("AssociationMappingsTableWizardPanel.outbound.attributeTable"),
                new VisibleBehaviour(this::isAttributeVisible)) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationSmartAttributeMappingsTable<>(panelId, Model.of(MappingDirection.ATTRIBUTE), Model.of(false), outboundEvalModel(), null) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AssociationConstructionExpressionEvaluatorType.F_ATTRIBUTE;
                    }

                    @Override
                    protected boolean isInboundRelated() {
                        return false;
                    }

                    @Override
                    protected void performOnEditMapping(@NotNull AjaxRequestTarget target, @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        inEditOutboundAttributeValue(rowModel, target, initialTab);
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-arrow-right-from-bracket");
            }
        };
    }

}
