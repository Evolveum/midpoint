/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DisplayableValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author skublik
 */
public class AutoCompleteDisplayableValueConverter<T> implements IConverter<T> {

    private final IModel<? extends List<DisplayableValue<T>>> values;

    private final boolean strict;

    public AutoCompleteDisplayableValueConverter(IModel<? extends List<DisplayableValue<T>>> values) {
        this(values, true);
    }

    public AutoCompleteDisplayableValueConverter(IModel<? extends List<DisplayableValue<T>>> values, boolean strict) {
        this.values = values;
        this.strict = strict;
    }

    @Override
    public T convertToObject(String value, Locale locale) throws ConversionException {
        Optional<DisplayableValue<T>> displayValue = values.getObject().stream().filter(v -> v.getLabel().equals(value)).findFirst();
        if (displayValue.isPresent()) {
            return displayValue.get().getValue();
        }
        if (strict) {
            throw new ConversionException("Cannot convert " + value);
        }
        return valueToObject(value);
    }

    protected T valueToObject(String value) {
        return null;
    }

    @Override
    public String convertToString(T key, Locale locale) {
        Optional<DisplayableValue<T>> displayValue = values.getObject().stream().filter(v -> v.getValue().equals(key)).findFirst();
        if (displayValue.isPresent()) {
            return displayValue.get().getLabel();
        }
        return key == null ? "" : keyToString(key);
    }

    protected String keyToString(T key) {
        return key.toString();
    }
}
