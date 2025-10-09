/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.converter;

import com.evolveum.midpoint.gui.impl.component.input.converter.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

public class QNameConverter extends AutoCompleteDisplayableValueConverter<QName> {
    public QNameConverter(IModel<? extends List<DisplayableValue<QName>>> values) {
        this(values, false);
    }

    public QNameConverter(IModel<? extends List<DisplayableValue<QName>>> values, boolean strict) {
        super(values, strict);
    }

    public QNameConverter() {
        this(Model.ofList(Collections.emptyList()));
    }

    @Override
    protected QName valueToObject(String value) {
        if (value.contains(":")) {
            int index = value.indexOf(":");
            return new QName(null, value.substring(index + 1), value.substring(0, index));
        }
        return new QName(value);
    }

    @Override
    protected String keyToString(QName key) {
        return StringUtils.isNotEmpty(key.getPrefix()) ? key.getPrefix() + ":" + key.getLocalPart() : key.getLocalPart();
    }
}
