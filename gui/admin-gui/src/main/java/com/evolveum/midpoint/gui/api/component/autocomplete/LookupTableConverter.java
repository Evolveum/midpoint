/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.Locale;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.Component;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author skublik
 */

public class LookupTableConverter<C> implements IConverter<C> {

    private static final long serialVersionUID = 1L;
    private IConverter<C> originConverter;
    private String lookupTableOid;
    private Component baseComponent;
    private boolean strict;

    public LookupTableConverter(IConverter<C> originConverter, Component baseComponent, boolean strict) {
        this.originConverter = originConverter;
        this.baseComponent = baseComponent;
        this.strict = strict;
    }

    @Override
    public C convertToObject(String value, Locale locale) throws ConversionException {
        LookupTableType lookupTable = getLookupTable();
        for (LookupTableRowType row : lookupTable.getRow()) {
            if (value.equals(row.getKey()) || value.equals(LocalizationUtil.translateLookupTableRowLabel(row))) {
                return originConverter.convertToObject(row.getKey(), locale);
            }
        }

        boolean differentValue = true;
        if (baseComponent != null && baseComponent.getDefaultModelObject() != null
                && baseComponent.getDefaultModelObject().equals(value)) {
            differentValue = false;
        }

        if (differentValue && strict) {
            throw new ConversionException("Cannot convert " + value);
        }

        return originConverter.convertToObject(value, locale);
    }

    @Override
    public String convertToString(C key, Locale arg1) {
        LookupTableType lookupTable = getLookupTable();
        if (lookupTable != null) {
            for (LookupTableRowType row : lookupTable.getRow()) {
                if (key.toString().equals(row.getKey())) {
                    return LocalizationUtil.translateLookupTableRowLabel(row);
                }
            }
        }
        return key.toString();
    }

    protected LookupTableType getLookupTable() {
        if (lookupTableOid != null) {
            return WebModelServiceUtils.loadLookupTable(lookupTableOid, getPageBase());
        }
        return null;
    }

    private PageBase getPageBase() {
        return (PageBase) baseComponent.getPage();
    }

}
