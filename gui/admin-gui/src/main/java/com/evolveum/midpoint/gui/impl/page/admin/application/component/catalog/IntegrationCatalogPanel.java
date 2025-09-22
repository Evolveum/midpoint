package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.filter.CatalogFilterPanel;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace.LocalConnectorCatalogPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundAttributeMappingsTable;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.web.component.AjaxButton;

import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;
import java.util.Optional;

public class IntegrationCatalogPanel extends BasePanel {
    enum Tab {}
    private static final String INBOUND_PANEL_TYPE = "rw-attribute-inbounds";

    public IntegrationCatalogPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();


        add(new CatalogFilterPanel("filter"));
        add(new AjaxButton("createWithAIBtn") {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        });

        add(createTabbedPanel());
    }

    private TabCenterTabbedPanel<ITab> createTabbedPanel() {
        List<ITab> tabs = List.of(createLocalConnectorsTab(), createIntegrationCatalogTab());
        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>("catalog", tabs) {};
        tabPanel.setOutputMarkupId(true);
        return tabPanel;
    }

    private ITab createLocalConnectorsTab() {
        return new PanelTab(getPageBase().createStringResource("IntegrationCatalog.tabs.localConnectors")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new LocalConnectorCatalogPanel(panelId);
            }
        };
    }

    private ITab createIntegrationCatalogTab() {
        return new PanelTab(getPageBase().createStringResource("IntegrationCatalog.tabs.integrationCatalog")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new WebMarkupContainer(panelId);
            }
        };
    }

    protected void inEditInboundValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target) {}

    protected ContainerPanelConfigurationType getConfiguration(String panelType){
        return WebComponentUtil.getContainerConfiguration(
                new GuiObjectDetailsPageType() {},
                // getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                panelType);
    }
    protected IModel<PrismContainerValueWrapper<?>> getValueModel() { return Model.of(); }

}
