/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

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

        component.add(AttributeModifier.replace("data-toggle", "tooltip"));
        component.add(new AttributeModifier("data-placement", getDataPlacement()) {

            @Override
            protected String newValue(String currentValue, String replacementValue) {
                if (StringUtils.isEmpty(currentValue)) {
                    return replacementValue;
                }
                return currentValue;
            }
        });
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);

        StringBuilder componentSb = new StringBuilder();
        componentSb.append("$('#");
        componentSb.append(component.getMarkupId());
        componentSb.append("')");

        StringBuilder sb = new StringBuilder();
        sb.append("if (typeof ");
        sb.append(componentSb.toString());
        sb.append(".tooltip === \"function\"){");
        sb.append(componentSb.toString());
        sb.append(".tooltip({html:true");

        if(!isInsideModal()){
            sb.append(", 'container':'body'");
        } else {
            sb.append(", 'container':'#");
            sb.append(getModalContainer(component));
            sb.append("'");
        }

        sb.append("});");
        sb.append("}");
        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    public String getModalContainer(Component component){
        return component.getMarkupId();
    }

    public String getDataPlacement(){
        return "right";
    }

    public boolean isInsideModal(){
        return false;
    }

}
