/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.DisplayableValue;

public class DisplayableValueChoiceRenderer<T> implements IChoiceRenderer<T> {

    private static final long serialVersionUID = 1L;
    private List<DisplayableValue> choices;


    public DisplayableValueChoiceRenderer(List<DisplayableValue> choices) {
        this.choices = choices;
    }

    @Override
    public String getDisplayValue(T object) {
        if (object == null) {
            return null;
        }

        if (object instanceof DisplayableValue) {
            return ((DisplayableValue) object).getLabel();
        }

        return object.toString();
    }

    @Override
    public String getIdValue(T object, int index) {

        if (object != null && choices != null) {
            for (DisplayableValue v : choices) {
                if (object.equals(v.getValue())) {
                    return String.valueOf(choices.indexOf(v));
                }

            }
        }

        return Integer.toString(index);
    }

    @Override
    public T getObject(String id, IModel<? extends List<? extends T>> choices) {
        if (StringUtils.isBlank(id)){
            return null;
        }

        T value = choices.getObject().get(Integer.parseInt(id));
        if (value instanceof DisplayableValue) {
            return (T) ((DisplayableValue) value).getValue();
        }

        return value;
    }

}
