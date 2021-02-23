/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    public static final String PROP_BUTTON_CLASS = "buttonClass";

    private IModel<Map<String, String>> options;

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
