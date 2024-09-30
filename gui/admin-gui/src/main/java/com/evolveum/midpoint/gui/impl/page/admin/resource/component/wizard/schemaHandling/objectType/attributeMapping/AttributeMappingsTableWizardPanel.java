/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelType(name = "rw-attribute-mappings")
@PanelInstance(identifier = "rw-attribute-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeMappingsTableWizardPanel.inboundTable", icon = "fa fa-arrow-right-to-bracket"))
@PanelInstance(identifier = "rw-attribute-outbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeMappingsTableWizardPanel.outboundTable", icon = "fa fa-arrow-right-from-bracket"))
public abstract class AttributeMappingsTableWizardPanel<P extends Containerable> extends AbstractResourceWizardBasicPanel<P> {

    private static final String INBOUND_PANEL_TYPE = "rw-attribute-inbounds";
    private static final String OUTBOUND_PANEL_TYPE = "rw-attribute-outbounds";

    private static final String ID_TAB_TABLE = "tabTable";

    private final MappingDirection initialTab;

    public AttributeMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<P, ResourceDetailsModel> superHelper,
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
        tabs.add(createInboundTableTab());
        tabs.add(createOutboundTableTab());

        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>(ID_TAB_TABLE, tabs) {
            @Override
            protected void onAjaxUpdate(Optional<AjaxRequestTarget> optional) {
                if (optional.isPresent()) {
                    AjaxRequestTarget target = optional.get();
                    target.add(getButtonsContainer());
                }
            }

            @Override
            protected void onClickTabPerformed(int index, Optional<AjaxRequestTarget> target) {
                if (getTable().isValidFormComponents(target.orElse(null))) {
                    super.onClickTabPerformed(index, target);
                }
            }
        };
        tabPanel.setOutputMarkupId(true);
        switch (initialTab) {
            case INBOUND:
                tabPanel.setSelectedTab(0);
                break;
            case OUTBOUND:
                tabPanel.setSelectedTab(1);
                break;
        }
        add(tabPanel);
    }

    private ITab createInboundTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource(
                "AttributeMappingsTableWizardPanel.inboundTable"),
                new VisibleBehaviour(() -> isInboundVisible())) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new InboundAttributeMappingsTable<>(panelId, getValueModel(), getConfiguration(INBOUND_PANEL_TYPE)) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AttributeMappingsTableWizardPanel.this.getItemNameOfContainerWithMappings();
                    }

                    @Override
                    public void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        if (isValidFormComponentsOfRow(rowModel, target)) {
                            inEditInboundValue(rowModel, target);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getCssIconModel() {
                return Model.of("fa fa-arrow-right-to-bracket");
            }
        };
    }

    protected abstract ItemName getItemNameOfContainerWithMappings();

    protected boolean isInboundVisible() {
        return true;
    }

    private ITab createOutboundTableTab() {
        return new IconPanelTab(
                getPageBase().createStringResource(
                "AttributeMappingsTableWizardPanel.outboundTable"),
                new VisibleBehaviour(() -> isOutboundVisible())) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new OutboundAttributeMappingsTable<>(panelId, getValueModel(), getConfiguration(OUTBOUND_PANEL_TYPE)) {
                    @Override
                    protected ItemName getItemNameOfContainerWithMappings() {
                        return AttributeMappingsTableWizardPanel.this.getItemNameOfContainerWithMappings();
                    }

                    @Override
                    public void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        if (isValidFormComponentsOfRow(rowModel, target)) {
                            inEditOutboundValue(rowModel, target);
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

    protected boolean isOutboundVisible() {
        return true;
    }

    public TabbedPanel<ITab> getTabPanel() {
        //noinspection unchecked
        return ((TabbedPanel<ITab>) get(ID_TAB_TABLE));
    }

    protected AttributeMappingsTable getTable() {
        TabbedPanel<ITab> tabPanel = getTabPanel();
        return (AttributeMappingsTable) tabPanel.get(TabbedPanel.TAB_PANEL_ID);
    }

    public MappingDirection getSelectedMappingType() {
        AttributeMappingsTable table = getTable();
        if (table instanceof InboundAttributeMappingsTable) {
            return MappingDirection.INBOUND;
        } else if (table instanceof OutboundAttributeMappingsTable) {
            return MappingDirection.OUTBOUND;
        }
        return null;
    }

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton showOverrides = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-shuffle"),
                getPageBase().createStringResource("AttributeMappingsTableWizardPanel.showOverrides")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getTable().isValidFormComponents(target)) {
                    onShowOverrides(target, getSelectedMappingType());
                }
            }
        };
        showOverrides.showTitleAsLabel(true);
        showOverrides.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(showOverrides);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected abstract void onShowOverrides(AjaxRequestTarget target, MappingDirection selectedMappingType);

    @Override
    protected String getSaveLabelKey() {
        return "AttributeMappingsTableWizardPanel.saveButton";
    }

    protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {

    }

    protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {

    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AttributeMappingsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AttributeMappingsTableWizardPanel.subText");
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
