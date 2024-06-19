/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class RoleAnalysisItemPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ITEM = "item";
    private static final String ID_CARD = "card";

    boolean isVisible = true;

    public RoleAnalysisItemPanel(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CARD);
        container.setOutputMarkupId(true);
        container.add(AttributeModifier.replace("class", replaceCardCssClass()));
        add(container);

        Label title = new Label("title", getModel());
        title.add(AttributeModifier.replace("title", getModel()));
        title.add(new TooltipBehavior());
        title.setOutputMarkupId(true);
        container.add(title);

        AjaxButton action = new AjaxButton("remove") {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                this.getParent().setVisible(isVisible = !isVisible);
                ajaxRequestTarget.add(this.getParent());
            }
        };
        action.add(AttributeModifier.replace("title", "Remove"));
        action.add(new TooltipBehavior());
        action.add(AttributeModifier.replace("class", replaceBtnToolCssClass()));
        action.setOutputMarkupId(true);
        container.add(action);

        WebMarkupContainer cardBody = new WebMarkupContainer("cardBody");
        cardBody.setOutputMarkupId(true);
        cardBody.add(AttributeModifier.append("class", getCardBodyCssClass()));
        cardBody.add(AttributeModifier.append("style", getCardBodyStyle()));
        container.add(cardBody);

        RepeatingView repeatingView = new RepeatingView(ID_ITEM);
        addItem(repeatingView);
        cardBody.add(repeatingView);
    }

    public String replaceBtnToolCssClass() {
        return null;
    }

    public String getCardBodyCssClass() {
        return "";
    }

    public String replaceCardCssClass() {
        return null;
    }

    public String getCardBodyStyle() {
        return "";
    }

    protected void addItem(RepeatingView repeatingView) {

    }

    protected boolean isLink() {
        return false;
    }

    public String getIconCssClass() {
        return "";
    }
}
