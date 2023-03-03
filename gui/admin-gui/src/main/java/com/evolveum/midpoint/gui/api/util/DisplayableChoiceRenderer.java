/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DisplayableChoiceRenderer<T extends Serializable> implements IChoiceRenderer<DisplayableValue<T>> {

    private static final long serialVersionUID = 1L;

    /**
     * TODO This impl doesn't look good, label should take preference, that's why it's there for...
     */
    @Override
    public Object getDisplayValue(DisplayableValue val) {
        Object value = val.getValue();
        String label = val.getLabel();

        if (value instanceof Enum) {
            return LocalizationUtil.translateEnum((Enum<?>) value);
        }

        if (val.getLabel() == null) {
            return LocalizationUtil.translate(String.valueOf(value));
        }

        return LocalizationUtil.translate(label);
    }

    @Override
    public String getIdValue(DisplayableValue val, int index) {
        return Integer.toString(index);
    }

    @Override
    public DisplayableValue<T> getObject(String id, IModel<? extends List<? extends DisplayableValue<T>>> choices) {
        return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
    }
}
