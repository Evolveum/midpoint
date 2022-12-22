/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

public class AjaxLinkTruncatePanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LINK_LABEL = "link_label";
    private static final String ID_ICON = "icon";

    public AjaxLinkTruncatePanel(String id, IModel<?> labelModel, StringResourceModel popupText, DisplayType displayType) {
        super(id);

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer("container");
        webMarkupContainer.setOutputMarkupId(true);
        webMarkupContainer.setOutputMarkupPlaceholderTag(true);
        webMarkupContainer.add(new AttributeAppender("class",getCssContainer()));
        add(webMarkupContainer);

        AjaxLink<String> link = new AjaxLink<>(ID_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkTruncatePanel.this.onClick(target);
            }

        };

        Label label = new Label(ID_LINK_LABEL, labelModel);
        label.add(new InfoTooltipBehavior() {
            @Override
            public String getCssClass() {
                return " text-truncate";
            }
        });
        label.add(AttributeModifier.replace("title", popupText));

        link.add(label);
        link.add(new EnableBehaviour(AjaxLinkTruncatePanel.this::isEnabled));
        link.add(new AttributeModifier("style", "height:" + getLabelHeight()));

        webMarkupContainer.add(new ImagePanel(ID_ICON, Model.of(displayType)));
        webMarkupContainer.add(link);
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }

    public String getLabelHeight() {
        return "100px";
    }

    public String getCssContainer(){
        return " font-weight-normal";
    }
}
