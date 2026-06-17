/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;

/**
 * This behavior is used for bootstrap tooltips. Just add this behaviour to {@link org.apache.wicket.markup.html.basic.Label}.
 * Label must have title set (e.g. wicket:message="title:YOUR_LOCALIZATION_PROPERTY_KEY").
 *
 * @author lazyman
 */
public class TooltipBehavior extends Behavior {

    @Override
    public void onConfigure(final Component component) {
        component.setOutputMarkupId(true);

        component.add(AttributeModifier.replace("data-bs-toggle", "tooltip"));
        addTooltipAttribute(component, "data-placement", getDataPlacement());

        String dataBoundary = getDataBoundary();
        if (StringUtils.isNotEmpty(dataBoundary)) {
            addTooltipAttribute(component, "data-boundary", dataBoundary);
        }
    }

    private void addTooltipAttribute(Component component, String attributeName, String value) {
        component.add(new AttributeModifier(attributeName, value) {
            @Override
            protected String newValue(String currentValue, String replacementValue) {
                if (StringUtils.isEmpty(currentValue)) {
                    return replacementValue;
                }
                return currentValue;
            }
        });
    }

    public String getDataPlacement() {
        return "right";
    }

    public String getDataBoundary() {
        return null;
    }
}
