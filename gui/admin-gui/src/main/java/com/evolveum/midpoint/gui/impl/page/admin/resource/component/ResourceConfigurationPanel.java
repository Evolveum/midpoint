/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

@PanelType(name = "resourceConnectorConfiguration")
@PanelInstance(identifier = "resourceConnectorConfiguration", applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.connector.configuration", icon = "fa fa-plug", order = 20))
public class ResourceConfigurationPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceConfigurationPanel.class);

    private static final String ID_CONFIGURATION = "configuration";
    private static final String ID_NO_CONNECTOR = "noConnectorLabel";

    public ResourceConfigurationPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        List<ITab> tabs = createConfigurationTabs();

        Label label = new Label(ID_NO_CONNECTOR, createStringResource("ReosurceConfigurationPanel.no.connector.selected"));
        label.setOutputMarkupId(true);
        label.add(new VisibleBehaviour(() -> tabs.isEmpty()));
        add(label);

        TabbedPanel<ITab> tabbedPanel = WebComponentUtil.createTabPanel(ID_CONFIGURATION, getPageBase(), tabs, null);
        tabbedPanel.add(new VisibleBehaviour(() -> !tabs.isEmpty()));
        add(tabbedPanel);

    }

    private List<ITab> createConfigurationTabs() {
        List<ITab> tabs = new ArrayList<>();
        getObjectDetailsModels().getConfigurationModel().reset();
        PrismContainerWrapper<ConnectorConfigurationType> configuration = getObjectDetailsModels().getConfigurationModelObject();
        if (configuration == null) {
            return new ArrayList<>();
        }
        PrismContainerValueWrapper<ConnectorConfigurationType> configurationValue;
        try {
            configurationValue = configuration.getValue();
        } catch (SchemaException e) {
            LOGGER.error("Cannot get value for conenctor configuration, {}", e.getMessage(), e);
            getSession().error("A problem occurred while getting value for connector configuration, " + e.getMessage());
            return null;
        }
        for (final PrismContainerWrapper<?> wrapper : configurationValue.getContainers()) {
            String tabName = wrapper.getDisplayName();
            tabs.add(new AbstractTab(new Model<>(tabName)) {
                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer getPanel(String panelId) {
                    return new SingleContainerPanel<>(panelId, Model.of(wrapper), wrapper.getTypeName());
                }
            });
        }

        return tabs;
    }

    public void updateConfigurationTabs() {

        TabbedPanel<ITab> tabbedPanel = getConfigurationTabbedPanel();
        List<ITab> tabs = tabbedPanel.getTabs().getObject();
        tabs.clear();

        tabs.addAll(createConfigurationTabs());
        if (tabs.size() == 0) {
            return;
        }
        int i = tabbedPanel.getSelectedTab();
        if (i < 0 || i > tabs.size()) {
            i = 0;
        }
        tabbedPanel.setSelectedTab(i);
    }

    @SuppressWarnings("unchecked")
    private TabbedPanel<ITab> getConfigurationTabbedPanel() {
        return (TabbedPanel<ITab>) get(ID_CONFIGURATION);
    }
}
