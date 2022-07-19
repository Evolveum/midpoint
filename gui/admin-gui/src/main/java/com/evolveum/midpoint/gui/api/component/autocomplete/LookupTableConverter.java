/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import java.util.Collection;
import java.util.Locale;

/**
 * @author skublik
 */

public class LookupTableConverter<C> implements IConverter<C> {

    private static final long serialVersionUID = 1L;
    private IConverter<C> originConverter;
    private String lookupTableOid;
    private Component baseComponent;
    private boolean strict;

    public LookupTableConverter(IConverter<C> originConverter, String lookupTableOid, Component baseComponent, boolean strict) {
        this.originConverter = originConverter;
        this.lookupTableOid = lookupTableOid;
        this.baseComponent = baseComponent;
        this.strict = strict;
    }

    @Override
    public C convertToObject(String value, Locale locale) throws ConversionException {
        LookupTableType lookupTable = getLookupTable();
        for (LookupTableRowType row : lookupTable.getRow()) {
            if (value.equals(row.getKey())
                    || value.equals(WebComponentUtil.getLocalizedOrOriginPolyStringValue(row.getLabel() != null ? row.getLabel().toPolyString() : null))) {

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
                    return WebComponentUtil.getLocalizedOrOriginPolyStringValue(row.getLabel() != null ? row.getLabel().toPolyString() : null);
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
