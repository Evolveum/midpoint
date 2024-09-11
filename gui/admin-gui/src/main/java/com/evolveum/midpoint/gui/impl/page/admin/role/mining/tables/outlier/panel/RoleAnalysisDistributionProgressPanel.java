/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import java.io.Serial;
import java.io.Serializable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class RoleAnalysisDistributionProgressPanel<T extends Serializable> extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER_PANEL = "container-panel";
    private static final String ID_PANEL = "panel";

    private static final String ID_CONTAINER_FOOTER = "container-footer";
    private static final String ID_LEGEND = "legend";

    private static final String ID_CONTAINER_HEADER = "container-header";
    private static final String ID_HEADER_PANEL = "header-panel";
    public RoleAnalysisDistributionProgressPanel(String id) {
        super(id);
        initLayout();
    }

    protected void initLayout() {

        WebMarkupContainer containerHeader = new WebMarkupContainer(ID_CONTAINER_HEADER);
        containerHeader.setOutputMarkupId(true);
        add(containerHeader);

        Component headerPanel = getHeaderPanelComponent(ID_HEADER_PANEL);
        headerPanel.setOutputMarkupId(true);
        containerHeader.add(headerPanel);

        WebMarkupContainer containerPanel = new WebMarkupContainer(ID_CONTAINER_PANEL);
        containerPanel.setOutputMarkupId(true);
        add(containerPanel);

        Component panel = getPanelComponent(ID_PANEL);
        panel.setOutputMarkupId(true);
        containerPanel.add(panel);

        WebMarkupContainer containerFooter = new WebMarkupContainer(ID_CONTAINER_FOOTER);
        containerFooter.setOutputMarkupId(true);
        containerFooter.add(AttributeAppender.replace("class", getContainerLegendCssClass()));
        add(containerFooter);


        Component legend = getLegendComponent(ID_LEGEND);
        legend.setOutputMarkupId(true);
        containerFooter.add(legend);


    }

    protected Component getLegendComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getPanelComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getHeaderPanelComponent(String id) {
        WebMarkupContainer container = new WebMarkupContainer(id);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> false));
        return container;
    }

    protected String getContainerLegendCssClass() {
        return "d-flex flex-wrap justify-content-between pt-2 pb-0 px-0";
    }
}
