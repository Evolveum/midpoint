/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import java.io.Serial;
import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

public class RoleAnalysisViewAllPanel<T extends Serializable> extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_TITLE = "title";
    private static final String ID_PANEL = "panel";
    private static final String ID_FOOTER = "footer";

    public RoleAnalysisViewAllPanel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("class", getContainerCssClass()));
        container.add(AttributeModifier.replace("style", getContainerCssStyle()));
        add(container);
        IconWithLabel iconWithLabel = new IconWithLabel(ID_TITLE, getModel()) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return RoleAnalysisViewAllPanel.this.getIconCssClass();
            }
        };
        container.add(iconWithLabel);

        Component panelComponent = getPanelComponent(ID_PANEL);
        panelComponent.setOutputMarkupId(true);
        container.add(panelComponent);

        AjaxLinkPanel viewAllIcon = new AjaxLinkPanel(ID_FOOTER,
                getLinkModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onLinkClick(target);
            }
        };
        viewAllIcon.setOutputMarkupId(true);
        container.add(viewAllIcon);
    }

    protected Component getPanelComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected IModel<String> getLinkModel() {
        return createStringResource("RoleAnalysisDetectedAnomalyTable.view.all");
    }

    protected void onLinkClick(AjaxRequestTarget target) {
        //TODO navigate to ...
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
}
