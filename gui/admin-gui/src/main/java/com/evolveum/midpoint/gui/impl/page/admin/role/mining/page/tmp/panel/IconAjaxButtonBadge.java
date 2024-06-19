/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class IconAjaxButtonBadge extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "label";
    private static final String ID_BADGE = "badge";

    boolean isClicked;

    public IconAjaxButtonBadge(String id, IModel<String> model, boolean isClicked) {
        super(id, model);
        this.isClicked = isClicked;
        initLayout();
        addClickBehavior();
        onLoadComponent();
    }


    protected void onLoadComponent() {

    }

    private void addClickBehavior() {
        add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onClick(target);
            }
        });
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex align-items-center gap-2 "));

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace("class", getIconCssClass()));
        image.setOutputMarkupId(true);
        add(image);

        Label label = new Label(ID_TEXT, getModel());
        label.setOutputMarkupId(true);
        add(label);

        Label badge = new Label(ID_BADGE, Model.of(getBadgeValue()));
        badge.add(AttributeAppender.append("class", "badge bg-danger "));
        badge.setOutputMarkupId(true);
        add(badge);
    }

    public Integer getBadgeValue() {
        return 0;
    }

    public String getIconCssClass() {
        return "";
    }

    public boolean isClicked() {
        return isClicked;
    }

    public void setClicked(boolean clicked) {
        isClicked = clicked;
    }

    protected void onClick(AjaxRequestTarget target) {
    }
}
