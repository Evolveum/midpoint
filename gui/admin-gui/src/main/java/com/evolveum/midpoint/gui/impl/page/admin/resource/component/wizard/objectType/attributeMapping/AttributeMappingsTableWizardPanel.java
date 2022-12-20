/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.TableTabbedPanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
public abstract class AttributeMappingsTableWizardPanel extends AbstractResourceWizardBasicPanel<ResourceObjectTypeDefinitionType> {

    private static final String ID_TAB_TABLE = "tabTable";

    private final WrapperContext.AttributeMappingType initialTab;

    public AttributeMappingsTableWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> superHelper,
            WrapperContext.AttributeMappingType initialTab) {
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

        TableTabbedPanel<ITab> tabPanel = new TableTabbedPanel<>(ID_TAB_TABLE, tabs) {
            @Override
            protected void onAjaxUpdate(Optional<AjaxRequestTarget> optional) {
                if (optional.isPresent()) {
                    AjaxRequestTarget target = optional.get();
                    target.add(getButtonsContainer());
                }
            }

            @Override
            protected String getIcon(int index) {
                switch (index){
                    case 0 :
                        return "fa fa-arrow-right-to-bracket";
                    case 1 :
                        return "fa fa-arrow-right-from-bracket";
                }
                return super.getIcon(index);
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
        return new AbstractTab(getPageBase().createStringResource(
                "AttributeMappingsTableWizardPanel.inboundTable")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new InboundAttributeMappingsTable(panelId, getValueModel()) {
                    @Override
                    protected void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        inEditInboundValue(rowModel, target);
                    }
                };
            }
        };
    }

    private ITab createOutboundTableTab() {
        return new AbstractTab(getPageBase().createStringResource(
                "AttributeMappingsTableWizardPanel.outboundTable")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new OutboundAttributeMappingsTable(panelId, getValueModel()) {
                    @Override
                    protected void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        inEditOutboundValue(rowModel, target);
                    }
                };
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

    public WrapperContext.AttributeMappingType getSelectedMappingType() {
        AttributeMappingsTable table = getTable();
        if (table instanceof InboundAttributeMappingsTable) {
            return WrapperContext.AttributeMappingType.INBOUND;
        } else if (table instanceof OutboundAttributeMappingsTable) {
            return WrapperContext.AttributeMappingType.OUTBOUND;
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

    protected abstract void onShowOverrides(AjaxRequestTarget target, WrapperContext.AttributeMappingType selectedMappingType);

    @Override
    protected String getSaveLabelKey() {
        return "AttributeMappingsTableWizardPanel.saveButton";
    }

    protected abstract void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target);

    protected abstract void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target);

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
}
