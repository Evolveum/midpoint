/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public class LabelWithBadgePanel extends BasePanel<Badge> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGE = "badge";

    IModel<String> labelModel;

    public LabelWithBadgePanel(String id, IModel<Badge> model, IModel<String> labelModel) {
        super(id, model);
        this.labelModel = labelModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.setOutputMarkupId(true);
        icon.add(AttributeModifier.append("class", getIconCss()));
        icon.add(new VisibleBehaviour(this::isIconVisible));
        add(icon);

        BadgePanel badge = new BadgePanel(ID_BADGE, getModel());
        badge.setOutputMarkupId(true);
        badge.add(new VisibleBehaviour(this::isBadgeVisible));
        add(badge);

        Label text = new Label(ID_LABEL, labelModel);
        text.setOutputMarkupId(true);
        text.add(AttributeAppender.append("class", getLabelCss()));
        add(text);
    }

    protected IModel<String> getLabelModel() {
        return labelModel;
    }

    protected boolean isIconVisible() {
        return true;
    }

    protected boolean isBadgeVisible() {
        return true;
    }

    protected String getLabelCss() {
        return null;
    }

    protected String getIconCss() {
        return null;
    }
}
