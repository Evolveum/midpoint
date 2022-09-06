/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class LookupAutocompletePanel<T> extends AutoCompleteTextPanel<T> {

    public LookupAutocompletePanel(String id, IModel<T> model, Class<T> type, boolean strict, String lookupTableOid) {
        super(id, model, type, strict, lookupTableOid);
    }

    @Override
    public Iterator<T> getIterator(String input) {
        return (Iterator<T>) prepareAutoCompleteList(input, getLookupTable()).iterator();
    }

    protected List<String> prepareAutoCompleteList(String input, LookupTableType lookupTable) {
        return WebComponentUtil.prepareAutoCompleteList(lookupTable, input);
    }
}
