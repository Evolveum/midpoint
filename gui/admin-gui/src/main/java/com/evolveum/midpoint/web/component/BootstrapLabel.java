/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class BootstrapLabel extends Label {

    public static enum State {
        DEFAULT, PRIMARY, SUCCESS, INFO, WARNING, DANGER;
    }

    public BootstrapLabel(String id, IModel<String> model, final IModel<State> state) {
        super(id, model);

        add(AttributeAppender.append("class", new IModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();
                sb.append("label label-");

                State s = state.getObject();
                if (s == null) {
                    s = State.DEFAULT;
                }
                sb.append(s.name().toLowerCase());

                return sb.toString();
            }
        }));
    }
}
