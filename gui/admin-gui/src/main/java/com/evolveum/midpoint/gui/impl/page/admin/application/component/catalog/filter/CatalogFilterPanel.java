/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.filter;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.ajax.AjaxRequestTarget;

public class CatalogFilterPanel extends BasePanel {
    public CatalogFilterPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new AjaxButton("clearFilterBtn", createStringResource("IntegrationCatalog.clearFiltersBtn")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        });
/*
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new IconPanelTab(Model.of("Left tab")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return null;
            }
        });
        tabs.add(new IconPanelTab(Model.of("Right tab")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return null;
            }
        });
        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel<>("tabCenterTabbedPanel", tabs) {
            @Override
            protected void onClickTabPerformed(int index, Optional<AjaxRequestTarget> target) {

            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setSelectedTab(0);
        add(tabPanel);
        var panel = new TabbedPanel<>("tabbedPanel", List.of(
                new ITab() {
                    @Override
                    public IModel<String> getTitle() {
                        return Model.of("Left panel");
                    }

                    @Override
                    public WebMarkupContainer getPanel(String s) {
                        return new WebMarkupContainer("") {

                        };
                    }

                    @Override
                    public boolean isVisible() {
                        return true;
                    }
                },
                new ITab() {
                    @Override
                    public IModel<String> getTitle() {
                        return Model.of("Right panel");
                    }

                    @Override
                    public WebMarkupContainer getPanel(String s) {
                        return new WebMarkupContainer("") {

                        };
                    }

                    @Override
                    public boolean isVisible() {
                        return true;
                    }
                }
        ));
        panel.setSelectedTab(0);
        add(panel);
 */
    }
}
