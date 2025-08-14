/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import java.io.Serial;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class CompositedIconTextPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LAYERED_ICON = "layeredIcon";
    private static final String ID_BASIC_ICON = "basicIcon";

    String basicIconCss;
    String text;
    String additionalLabelClass;

    public CompositedIconTextPanel(String id, String basicIconCss, String text, String additionalLabelClass) {
        super(id);
        this.basicIconCss = basicIconCss;
        this.text = text;
        this.additionalLabelClass = additionalLabelClass;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer layeredIcon = new WebMarkupContainer(ID_LAYERED_ICON);
        add(layeredIcon);

        WebComponent basicIcon = new WebComponent(ID_BASIC_ICON);
        basicIcon.add(AttributeAppender.append("class", this.basicIconCss));
        basicIcon.add(AttributeAppender.replace("style", getBasicIconCssStyle()));
        layeredIcon.add(basicIcon);

        Label text = new Label("text", Model.of(this.text));
        text.add(AttributeAppender.append("class", this.additionalLabelClass));
        text.setOutputMarkupId(true);
        layeredIcon.add(text);

    }

    protected String getBasicIconCssStyle() {
        return null;
    }
}
