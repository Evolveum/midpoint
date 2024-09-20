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
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

public class IconAjaxButtonBadge extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "label";
    private static final String ID_BADGE = "badge";

    boolean isClicked;
    boolean isUnique = false;

    public IconAjaxButtonBadge(String id, IModel<String> model, boolean isClicked) {
        super(id, model);
        this.isClicked = isClicked;
        initLayout();
        addClickBehavior();
        onLoadComponent();
    }

    protected void onLoadComponent() {
    //override
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
        add(AttributeModifier.append(CLASS_CSS, getAdditionalCssClass()));

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass()));
        image.setOutputMarkupId(true);
        image.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass()));
                super.onConfigure(component);
            }
        });
        add(image);

        Label label = new Label(ID_TEXT, getModel());
        label.add(AttributeModifier.append(TITLE_CSS, getModel()));
        label.add(new TooltipBehavior());
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.replace(CLASS_CSS, getLabelCssClass()));
        add(label);

        Label badge = new Label(ID_BADGE, Model.of(getBadgeValue()));
        badge.add(AttributeModifier.replace(CLASS_CSS, getBadgeCssClass()));
        badge.setOutputMarkupId(true);
        add(badge);
    }

    public String getBadgeValue() {
        return "0";
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
    //override in subclass
    }

    protected String getBadgeCssClass() {
        return null;
    }

    protected String getLabelCssClass() {
        return null;
    }

    protected String getAdditionalCssClass() {
        return "d-flex align-items-center gap-2 ";
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }
}
