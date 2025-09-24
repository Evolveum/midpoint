package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.filter.CatalogFilterPanel;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace.LocalConnectorCatalogPanel;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace.MarketplaceCatalogPanel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;

import com.evolveum.midpoint.web.component.TabCenterTabbedPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.util.List;

public class IntegrationCatalogPanel extends BasePanel<Void> {
    private static final Trace LOGGER = TraceManager.getTrace(IntegrationCatalogPanel.class);

    public IntegrationCatalogPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new CatalogFilterPanel("aside"));
        add(new AjaxButton("createWithAIBtn") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.debug("Clicked: createWithAIBtn");
            }
        });
        add(new AjaxLink<Void>("requestConnectorLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.debug("Clicked: requestConnectorLink");
            }
        });
        add(new AjaxLink<Void>("importConnectorLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.debug("Clicked: importConnectorLink");
            }
        });
        add(new AjaxLink<Void>("feedbackButton") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.debug("Clicked: feedbackButton");
            }
        });
        add(createTabbedPanel());
    }

    protected WebMarkupContainer createLocalConnectorCatalogPanel(String panelId) {
        return new LocalConnectorCatalogPanel(panelId);
    }

    protected WebMarkupContainer createMarketplaceCatalogPanel(String panelId) {
        return new MarketplaceCatalogPanel(panelId);
    }

    private TabCenterTabbedPanel<ITab> createTabbedPanel() {
        List<ITab> tabs = List.of(createIntegrationCatalogTab(), createLocalConnectorsTab());
        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>("main", tabs);
        tabPanel.setOutputMarkupId(true);
        tabPanel.setSelectedTab(1); // TODO mělo by si to asi pamatovat, nebo ovodit z URL
        return tabPanel;
    }

    private ITab createLocalConnectorsTab() {
        return new PanelTab(getPageBase().createStringResource("IntegrationCatalog.tabs.localConnectors")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return IntegrationCatalogPanel.this.createLocalConnectorCatalogPanel(panelId);
            }
        };
    }

    private ITab createIntegrationCatalogTab() {
        return new PanelTab(getPageBase().createStringResource("IntegrationCatalog.tabs.integrationCatalog")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return IntegrationCatalogPanel.this.createMarketplaceCatalogPanel(panelId);
            }
        };
    }
}
