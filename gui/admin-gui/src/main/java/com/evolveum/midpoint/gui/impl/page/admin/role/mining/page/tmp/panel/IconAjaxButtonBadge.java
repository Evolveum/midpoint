/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class IconAjaxButtonBadge extends BasePanel<RoleAnalysisAttributeAnalysisDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TEXT = "label";
    private static final String ID_BADGE = "badge";

    private static final String STATUS_ACTIVE = " active ";

    public IconAjaxButtonBadge(String id, IModel<RoleAnalysisAttributeAnalysisDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
        addClickBehavior();
    }

    private void initLayout() {
        add(AttributeModifier.replace(CLASS_CSS, () ->
                getModelObject().isSelected()
                ? getButtonCssClass() + STATUS_ACTIVE
                : getButtonCssClass()));

        Label image = new Label(ID_ICON);
        image.add(AttributeModifier.replace(CLASS_CSS, getIconCssClass()));
        image.setOutputMarkupId(true);
        add(image);

        IModel<String> labelModel = createStringResource("${displayNameKey}", getModel());
        Label label = new Label(ID_TEXT, labelModel);
        label.add(AttributeModifier.append(TITLE_CSS, labelModel));
        label.add(new TooltipBehavior());
        label.setOutputMarkupId(true);
        label.add(AttributeModifier.replace(CLASS_CSS, getLabelCssClass()));
        add(label);

        Label badge = new Label(ID_BADGE, getBadgeModel());
        badge.add(AttributeModifier.replace(CLASS_CSS, getBadgeCssClass()));
        badge.setOutputMarkupId(true);
        add(badge);
    }

    private void addClickBehavior() {
        add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onClick(target);
            }
        });
    }

    private String getButtonCssClass() {
        return "d-flex align-items-center gap-1 btn btn-sm btn-pill rounded-pill";
    }

    public IModel<String> getBadgeModel() {
        return () -> "(" + getModelObject().getAttributeValuesSize() + ")";
    }

    public String getIconCssClass() {
        if (getModelObject().isSelected()) {
            return "fa fa-check ml-1";
        } else {
            Class<?> type = getModelObject().getType();
            if (type == null) {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
            }
            return (UserType.class.equals(type) ? GuiStyleConstants.CLASS_OBJECT_USER_ICON : GuiStyleConstants.CLASS_OBJECT_ROLE_ICON) + " ml-1";
        }

    }

    protected void onClick(AjaxRequestTarget target) {

    }

    protected String getBadgeCssClass() {
        return "ml-auto mr-1";
    }

    protected String getLabelCssClass() {
        return " text-truncate pill-label";
    }

}
