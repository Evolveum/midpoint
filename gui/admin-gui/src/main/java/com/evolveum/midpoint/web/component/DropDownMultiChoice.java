/*
 * Copyright (c) 2010-2013 Evolveum
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

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

import java.util.*;

/**
 * @author lazyman
 */
public class DropDownMultiChoice<T> extends ListMultipleChoice<T> {

    public static final String FUNC_BUTTON_TEXT = "buttonText";
    public static final String FUNC_BUTTON_TITLE = "buttonTitle";

    public static final String PROP_BUTTON_CLASS = "buttonClass";
    public static final String PROP_BUTTON_WIDTH = "buttonWidth";
    public static final String PROP_BUTTON_CONTAINER = "buttonContainer";

    private IModel<Map<String, String>> options;

    public DropDownMultiChoice(String id, IModel<? extends List<T>> model,
                               IModel<? extends List<? extends T>> choices, IModel<Map<String, String>> options) {
        super(id, model, choices);
        this.options = options;
    }

    public DropDownMultiChoice(String id, IModel<List<T>> object, IModel<List<T>> choices,
                               IChoiceRenderer<T> renderer, IModel<Map<String, String>> options) {
        super(id, object, choices, renderer);
        this.options = options;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initDropdown('").append(getMarkupId()).append("',");
        appendOptions(sb);
        sb.append(");");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    private void appendOptions(StringBuilder sb) {
        Map<String, String> map;
        if (options != null && options.getObject() != null) {
            map = options.getObject();
        } else {
            map = createDefaultOptions();
        }

        sb.append('{');
       Iterator<Map.Entry<String, String>> keys = map.entrySet().iterator();
        while (keys.hasNext()) {
            final Map.Entry<String, String> key = keys.next();
            sb.append(key.getKey()).append(":");
            sb.append('\'').append(key.getValue()).append('\'');
            if (keys.hasNext()) {
                sb.append(",\n");
            }
        }
        sb.append('}');
    }

    private Map<String, String> createDefaultOptions() {
        Map<String, String> map = new HashMap<>();
        map.put(PROP_BUTTON_CLASS, "btn btn-default btn-sm");

        return map;
    }
}
