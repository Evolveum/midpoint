/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.converter.AbstractConverter;

import com.evolveum.midpoint.util.DisplayableValue;

/**
 * @author Viliam Repan (lazyman)
 */
public class DisplayableRenderer<T extends Serializable> extends AbstractConverter<DisplayableValue<T>>
        implements IChoiceRenderer<DisplayableValue<T>> {

    private static final long serialVersionUID = 1L;

    private IModel<List<DisplayableValue<T>>> allChoices;

    public DisplayableRenderer(IModel<List<DisplayableValue<T>>> allChoices) {
        this.allChoices = allChoices;
    }

    @Override
    protected Class<DisplayableValue<T>> getTargetType() {
        return (Class) DisplayableValue.class;
    }

    @Override
    public Object getDisplayValue(DisplayableValue<T> object) {
        if (object == null) {
            return null;
        }

        return object.getLabel();
    }

    @Override
    public String getIdValue(DisplayableValue<T> object, int index) {
        return Integer.toString(index);
    }

    @Override
    public DisplayableValue<T> getObject(String id, IModel<? extends List<? extends DisplayableValue<T>>> choices) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return choices.getObject().get(Integer.parseInt(id));
    }

    @Override
    public DisplayableValue<T> convertToObject(String value, Locale locale) throws ConversionException {
        if (value == null) {
            return null;
        }

        List<DisplayableValue<T>> values = allChoices.getObject();
        for (DisplayableValue<T> val : values) {
            if (value.equals(val.getLabel())) {
                return val;
            }
        }

        return null;
    }
}
