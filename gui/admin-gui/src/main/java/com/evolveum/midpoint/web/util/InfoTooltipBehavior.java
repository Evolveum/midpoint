/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.util;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class InfoTooltipBehavior extends TooltipBehavior {

    @Override
    public void onConfigure(Component component) {
        super.onConfigure(component);

        String cssClass = getCssClass();
        if (cssClass != null) {
            component.add(AttributeModifier.append("class", cssClass));
        }

        // wcag
        component.add(AttributeModifier.replace("role", "button"));
        component.add(AttributeModifier.append("class", "clickable-by-enter"));
        component.add(AttributeModifier.replace("tabindex", "0"));

        IModel<String> ariaLabelModel = createAriaLabelModel();
        if (ariaLabelModel != null) {
            component.add(AttributeModifier.replace("aria-label", ariaLabelModel));
        }
    }

    /**
     * Override to provide custom css class (image, icon) for the tooltip
     */
    public String getCssClass() {
        return "fa fa-info-circle text-info";
    }

    public IModel<String> createAriaLabelModel() {
        return null;
    }
}
