/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AttributeMappingsTable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "rw-association-outbound-mappings")
@PanelInstance(identifier = "rw-association-outbound-attribute-mappings",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "OutboundMappingsTableWizardPanel.attributeMappingsTable", icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(identifier = "rw-association-outbound-object-mappings",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "OutboundMappingsTableWizardPanel.objectsTable", icon = "fa-regular fa-cube"))
public abstract class OutboundMappingsTableWizardPanel extends AbstractResourceWizardBasicPanel<AssociationConstructionExpressionEvaluatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundMappingsTableWizardPanel.class);

    private static final String ATTRIBUTE_PANEL_TYPE = "rw-association-outbound-attribute-mappings";
    private static final String OBJECT_PANEL_TYPE = "rw-association-outbound-object-mappings";

    private static final String ID_TAB_TABLE = "tabTable";

    private MappingDirection initialTab;

    public OutboundMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<AssociationConstructionExpressionEvaluatorType, ResourceDetailsModel> superHelper,
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
        tabs.add(createObjectTableTab());
        tabs.add(createAttributeTableTab());

        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>(ID_TAB_TABLE, tabs) {
            @Override
            protected void onClickTabPerformed(int index, Optional<AjaxRequestTarget> target) {
                if (getTable().isValidFormComponents(target.orElse(null))) {
                    if (index == 0) {
                        initialTab = MappingDirection.OBJECTS;
                    } else if (index == 1) {
                        initialTab = MappingDirection.ATTRIBUTE;
                    }
                    super.onClickTabPerformed(index, target);
                }
            }
        };
        tabPanel.setOutputMarkupId(true);
        switch (initialTab) {
            case OBJECTS:
                tabPanel.setSelectedTab(0);
                break;
            case ATTRIBUTE:
                tabPanel.setSelectedTab(1);
                break;
        }
        add(tabPanel);
    }

    private ITab createAttributeTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("OutboundMappingsTableWizardPanel.attributeMappingsTable"),
                new VisibleBehaviour(() -> isAttributeVisible())) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationOutboundAttributeMappingsTable(panelId, getValueModel(), getConfiguration(ATTRIBUTE_PANEL_TYPE)) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AssociationConstructionExpressionEvaluatorType.F_ATTRIBUTE;
                    }

                    @Override
                    protected MappingDirection getMappingType() {
                        return MappingDirection.ATTRIBUTE;
                    }

                    @Override
                    public void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        if (isValidFormComponentsOfRow(rowModel, target)) {
                            inEditAttributeValue(rowModel, target, initialTab);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-arrow-right-from-bracket");
            }
        };
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

    private ITab createObjectTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource("OutboundMappingsTableWizardPanel.objectsTable")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssociationOutboundAttributeMappingsTable(panelId, getValueModel(), getConfiguration(OBJECT_PANEL_TYPE)) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF;
                    }

                    @Override
                    protected MappingDirection getMappingType() {
                        return MappingDirection.OBJECTS;
                    }

                    @Override
                    public void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        if (isValidFormComponentsOfRow(rowModel, target)) {
                            inEditAttributeValue(rowModel, target, initialTab);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-cube");
            }
        };
    }

    public TabbedPanel<ITab> getTabPanel() {
        //noinspection unchecked
        return ((TabbedPanel<ITab>) get(ID_TAB_TABLE));
    }

    protected AttributeMappingsTable getTable() {
        TabbedPanel<ITab> tabPanel = getTabPanel();
        return (AttributeMappingsTable) tabPanel.get(TabbedPanel.TAB_PANEL_ID);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    @Override
    protected String getSaveLabelKey() {
        return "OutboundMappingsTableWizardPanel.saveButton";
    }

    protected abstract void inEditAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("OutboundMappingsTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("OutboundMappingsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("OutboundMappingsTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected ContainerPanelConfigurationType getConfiguration(String panelType){
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                panelType);
    }
}
