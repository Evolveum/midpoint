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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.TableTabbedPanel;
import com.evolveum.midpoint.gui.api.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public abstract class AttributeMappingsTableWizardPanel extends AbstractWizardBasicPanel {

    private static final String ID_TAB_TABLE = "tabTable";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;
    private final WrapperContext.AttributeMappingType initialTab;

    public AttributeMappingsTableWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel,
            WrapperContext.AttributeMappingType initialTab) {
        super(id, model);
        this.valueModel = valueModel;
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
            protected void onAjaxUpdate(Optional optional) {
                if (optional.isPresent()) {
                    AjaxRequestTarget target = (AjaxRequestTarget) optional.get();
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
                if (getTable().isValidFormComponents(target.get())) {
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
                return new InboundAttributeMappingsTable(panelId, valueModel) {
                    @Override
                    protected void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        onEditValue(rowModel, target);
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
                return new OutboundAttributeMappingsTable(panelId, valueModel) {
                    @Override
                    protected void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            List<PrismContainerValueWrapper<MappingType>> listItems) {
                        onEditValue(rowModel, target);
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
        AjaxIconButton newObjectTypeButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-circle-plus"),
                () -> {
                    String ret = null;
                    switch (getSelectedMappingType()) {
                        case INBOUND:
                            ret = getPageBase().createStringResource(
                                    "AttributeMappingsTableWizardPanel.addNewAttributeMapping.inbound").getString();
                            break;
                        case OUTBOUND:
                            ret = getPageBase().createStringResource(
                                    "AttributeMappingsTableWizardPanel.addNewAttributeMapping.outbound").getString();
                            break;
                    }
                    return ret;
                }) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onAddNewObject(target);
            }
        };
        newObjectTypeButton.showTitleAsLabel(true);
        newObjectTypeButton.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(newObjectTypeButton);

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

        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getTable().isValidFormComponents(target)) {
                    onSaveResourcePerformed(target);
                }
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(AttributeAppender.append("class", "btn btn-success"));
        buttons.add(saveButton);
    }

    protected String getSubmitIcon() {
        return "fa fa-floppy-disk";
    }

    protected abstract void onShowOverrides(AjaxRequestTarget target, WrapperContext.AttributeMappingType selectedMappingType);

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("AttributeMappingsTableWizardPanel.saveButton");
    }

    private void onAddNewObject(AjaxRequestTarget target) {
        TabbedPanel<ITab> tabPanel = getTabPanel();
        AttributeMappingsTable table = (AttributeMappingsTable) tabPanel.get(TabbedPanel.TAB_PANEL_ID);
        onEditValue(Model.of(table.createNewMapping(target)), target);
    }

    protected abstract void onSaveResourcePerformed(AjaxRequestTarget target);

    private void onEditValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {
        switch (getSelectedMappingType()) {
            case INBOUND:
                inEditInboundValue(value, target);
                break;
            case OUTBOUND:
                inEditOutboundValue(value, target);
                break;
        }
    }

    protected abstract void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target);

    protected abstract void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target);

    @Override
    protected IModel<String> getBreadcrumbLabel() {
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
