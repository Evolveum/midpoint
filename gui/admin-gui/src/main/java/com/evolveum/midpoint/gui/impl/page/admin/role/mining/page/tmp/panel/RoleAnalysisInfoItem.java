/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.wicket.model.Model;

public class RoleAnalysisInfoItem extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ACTION = "action";
    private static final String ID_LABEL = "description";

    public RoleAnalysisInfoItem(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {

        Label description = new Label(ID_LABEL, getModel());
        description.add(AttributeModifier.replace("class", getIconCssClass()));
        description.add(AttributeModifier.replace("title", getModel()));
        description.add(new TooltipBehavior());
        description.setOutputMarkupId(true);
        add(description);

        AjaxLinkPanel components = new AjaxLinkPanel(ID_ACTION, Model.of("Explore")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerform(target);
            }
        };
        components.setOutputMarkupId(true);
        components.add(AttributeModifier.replace("title", "Explore"));
        components.add(new TooltipBehavior());
        add(components);

    }

    protected void onClickPerform(AjaxRequestTarget target) {

    }

    protected boolean isLink() {
        return false;
    }

    public String getIconCssClass() {
        return "";
    }
}
