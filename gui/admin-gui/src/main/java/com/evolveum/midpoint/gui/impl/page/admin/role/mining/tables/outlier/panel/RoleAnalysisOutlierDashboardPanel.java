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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

public class RoleAnalysisOutlierDashboardPanel<T extends Serializable> extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_TITLE = "title";
    private static final String ID_PANEL_CONTAINER = "panel-container";
    private static final String ID_PANEL = "panel";
    private static final String ID_FOOTER = "footer";
    private static final String ID_FOOTER_CONTENT = "footer-container";

    public RoleAnalysisOutlierDashboardPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("class", getContainerCssClass()));
        container.add(AttributeModifier.replace("style", getContainerCssStyle()));
        add(container);

        WebMarkupContainer panelContainer = new WebMarkupContainer(ID_PANEL_CONTAINER);
        panelContainer.setOutputMarkupId(true);
        panelContainer.add(AttributeModifier.replace("class", getPanelContainerCssClass()));
        container.add(panelContainer);
        IconWithLabel iconWithLabel = new IconWithLabel(ID_TITLE, getModel()) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return RoleAnalysisOutlierDashboardPanel.this.getIconCssClass();
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getComponentCssClass() {
                return "d-flex align-items-center";
            }

            @Contract(pure = true)
            @Override
            protected String getComponentCssStyle() {
                return "";
            }
        };
        container.add(iconWithLabel);

        Component panelComponent = getPanelComponent(ID_PANEL);
        panelComponent.add(AttributeModifier.append(CLASS_CSS, "p-0"));
        panelComponent.setOutputMarkupId(true);
        panelContainer.add(panelComponent);

        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTENT);
        footerContainer.setOutputMarkupId(true);
        footerContainer.add(new VisibleBehaviour(this::isFooterVisible));
        container.add(footerContainer);

        Component footerComponent = getFooterComponent(ID_FOOTER);
        footerComponent.setOutputMarkupId(true);
        footerContainer.add(footerComponent);
    }

    protected String getPanelContainerCssClass() {
        return null;
    }

    protected Component getPanelComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getFooterComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected String getIconCssClass() {
        return "fa fa-exclamation-triangle text-warning";
    }

    protected String getContainerCssClass() {
        return null;
    }

    protected String getContainerCssStyle() {
        return null;
    }

    protected boolean isFooterVisible() {
        return true;
    }
}
