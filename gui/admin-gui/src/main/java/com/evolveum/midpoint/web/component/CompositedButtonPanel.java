/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconPanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

public class CompositedButtonPanel extends BasePanel<CompositedIconButtonDto> {

    private static final String ID_COMPOSITED_BUTTON = "compositedButton";
    private static final String ID_COMPOSITED_ICON = "compositedIcon";
    private static final String ID_LABEL = "label";

    public CompositedButtonPanel(String id, IModel<CompositedIconButtonDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_COMPOSITED_BUTTON);
        add(buttonContainer);

        CompositedIconPanel compositedIconPanel = new CompositedIconPanel(ID_COMPOSITED_ICON, new PropertyModel<>(getModel(), CompositedIconButtonDto.F_COMPOSITED_ICON));
        buttonContainer.add(compositedIconPanel);

        Label label = new Label(ID_LABEL, () -> {
            DisplayType displayType = getModelObject().getAdditionalButtonDisplayType();
            return WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
        });
        buttonContainer.add(label);

        buttonContainer.add(AttributeAppender.append("title", () -> {
            DisplayType displayType = getModelObject().getAdditionalButtonDisplayType();
            return WebComponentUtil.getTranslatedPolyString(displayType.getTooltip());
        }));
        buttonContainer.add(new TooltipBehavior());

        buttonContainer.add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onButtonClicked(target, getModelObject());
            }

            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setPreventDefault(true);
                super.updateAjaxAttributes(attributes);
            }

        });
    }

    protected void onButtonClicked(AjaxRequestTarget target, CompositedIconButtonDto buttonDescription) {

    }
}
