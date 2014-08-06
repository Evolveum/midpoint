/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        StringBuilder sb = new StringBuilder();
        sb.append("$('#");
        sb.append(component.getMarkupId());
        sb.append("').tooltip({html:true});");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    public String getDataPlacement(){
        return "right";
    }
}
