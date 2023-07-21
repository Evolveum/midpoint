/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.SpecificMappingProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

public abstract class AbstractSpecificMappingWizardPanel<C extends Containerable> extends AbstractWizardBasicPanel<ResourceDetailsModel> {
    private static final String ID_TAB_TABLE = "tabTable";

    private final IModel<PrismContainerWrapper<C>> containerModel;
    private final MappingDirection initialTab;

    public AbstractSpecificMappingWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerWrapper<C>> containerModel,
            MappingDirection initialTab) {
        super(id, model);
        this.containerModel = containerModel;
        this.initialTab = initialTab;
    }

    public IModel<PrismContainerWrapper<C>> getContainerModel() {
        return containerModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createTableTab(MappingDirection.INBOUND));
        tabs.add(createTableTab(MappingDirection.OUTBOUND));

        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>(ID_TAB_TABLE, tabs);
        tabPanel.setOutputMarkupId(true);
        switch (initialTab) {
            case INBOUND:
                tabPanel.setSelectedTab(0);
                break;
            case OUTBOUND:
                tabPanel.setSelectedTab(1);
                break;
        }

            tabPanel.setOutputMarkupId(true);

        add(tabPanel);
    }

    private ITab createTableTab(MappingDirection mappingDirection) {
        String key = null;
        switch (mappingDirection) {
            case INBOUND:
                key = "AbstractSpecificMappingWizardPanel.inboundTable";
                break;
            case OUTBOUND:
                key = "AbstractSpecificMappingWizardPanel.outboundTable";
                break;
        }

        return new CountablePanelTab(getPageBase().createStringResource(key)) {

            @Override
            public String getCount() {
                SpecificMappingProvider<C> provider = new SpecificMappingProvider(
                        AbstractSpecificMappingWizardPanel.this,
                        new ContainerValueWrapperFromObjectWrapperModel<>(containerModel, ItemPath.EMPTY_PATH),
                        mappingDirection);
                return String.valueOf(provider.size());
            }

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return AbstractSpecificMappingWizardPanel.this.createTablePanel(panelId, containerModel, mappingDirection);
            }
            @Override
            public IModel<String> getCssIconModel() {
                return () -> {
                    switch (mappingDirection) {
                        case INBOUND:
                            return  "fa fa-arrow-right-to-bracket";
                        case OUTBOUND:
                            return  "fa fa-arrow-right-from-bracket";
                    }
                    return "";
                };
            }
        };
    }

    protected final void editConfiguredMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection mappingDirection, AjaxRequestTarget target) {
        if (MappingDirection.INBOUND.equals(mappingDirection)) {
            editInboundMapping(valueModel, target);
        } else if (MappingDirection.OUTBOUND.equals(mappingDirection)) {
            editOutboundMapping(valueModel, target);
        }
    }

    protected abstract AbstractSpecificMappingTileTable<C> createTablePanel(
            String panelId,
            IModel<PrismContainerWrapper<C>> containerModel,
            MappingDirection mappingDirection);

    protected abstract void editOutboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target);

    protected abstract void editInboundMapping(IModel<PrismContainerValueWrapper<MappingType>> valueModel, AjaxRequestTarget target);

    protected abstract void editPredefinedMapping(
            IModel<PrismContainerValueWrapper<AbstractPredefinedActivationMappingType>> valueModel,
            MappingDirection direction, AjaxRequestTarget target);

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return true;
    }
}
