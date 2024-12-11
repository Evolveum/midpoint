/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BadgePanel extends BasePanel<Badge> {

    private static final long serialVersionUID = 1L;
    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "text";

    public BadgePanel(String id, IModel<Badge> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "span");
    }

    private void initLayout() {
        add(AttributeModifier.replace("title", getModelObject().getTitle()));

        add(AttributeAppender.append("class", () -> getModelObject().getCssClass()));
        add(new VisibleBehaviour(() ->
                getModelObject() != null
                        && (StringUtils.isNotEmpty(getModelObject().getText()) || StringUtils.isNotEmpty(getModelObject().getIconCssClass()))));

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIconCssClass()));
        icon.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getIconCssClass())));
        add(icon);

        Label text = new Label(ID_TEXT, () -> getModelObject().getText());
        text.add(AttributeAppender.append("class", () -> getModelObject().getTextCssClass()));
        add(text);
    }
}
