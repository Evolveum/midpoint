/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import java.util.Locale;

/**
 * @author skublik
 */

public class LookupTableConverter<C> implements IConverter<C> {

    private static final long serialVersionUID = 1L;
    private IConverter<C> originConverter;
    private LookupTableType lookupTable = null;
    private FormComponent baseComponent;
    private boolean strict;

    public LookupTableConverter(IConverter<C> originConverter, LookupTableType lookupTable, FormComponent baseComponent, boolean strict) {
        this.originConverter = originConverter;
        this.lookupTable = lookupTable;
        this.baseComponent = baseComponent;
        this.strict = strict;
    }

    @Override
    public C convertToObject(String value, Locale locale) throws ConversionException {
        for (LookupTableRowType row : lookupTable.getRow()) {
            if (value.equals(WebComponentUtil.getLocalizedOrOriginPolyStringValue(row.getLabel() != null ? row.getLabel().toPolyString() : null))) {
                return originConverter.convertToObject(row.getKey(), locale);
            }
        }
        boolean differentValue = true;
        if (baseComponent != null && baseComponent.getModelObject() != null
                && baseComponent.getModelObject().equals(value)) {
            differentValue = false;
        }

        if (differentValue && strict) {
            throw new ConversionException("Cannot convert " + value);
        }

        return originConverter.convertToObject(value, locale);

    }

    @Override
    public String convertToString(C key, Locale arg1) {
        if (lookupTable != null) {
            for (LookupTableRowType row : lookupTable.getRow()) {
                if (key.toString().equals(row.getKey())) {
                    return WebComponentUtil.getLocalizedOrOriginPolyStringValue(row.getLabel() != null ? row.getLabel().toPolyString() : null);
                }
            }
        }
        return key.toString();
    }
}
