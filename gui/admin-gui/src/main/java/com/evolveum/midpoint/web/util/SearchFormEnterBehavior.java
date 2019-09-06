/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

/**
 *  @author shood
 * */
public class SearchFormEnterBehavior extends Behavior {

    private Component target;

    public SearchFormEnterBehavior(Component target){
        this.target = target;
    }

    @Override
    public void onConfigure(Component component) {
        super.onConfigure(component);

        component.setOutputMarkupId(true);
        target.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response){
        super.renderHead(component, response);

        StringBuilder sb = new StringBuilder();
        sb.append("$(\"#");
        sb.append(component.getMarkupId());
        sb.append("\").on(\"keypress\",function(event) {if(event.which==13){ $(\"#");
        sb.append(target.getMarkupId());
        sb.append("\").click();return false;}});");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }
}
