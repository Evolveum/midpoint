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

package com.evolveum.midpoint.web.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
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

        add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

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
