/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.TextPanelFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.wicket.model.IModel;

import java.util.Iterator;
import java.util.List;

public class LookupAutocompletePanel<T> extends AutoCompleteTextPanel<T> {


    public LookupAutocompletePanel(String id, IModel<T> model, Class<T> type, boolean strict, String lookupTableOid) {
        super(id, model, type, strict, lookupTableOid);
    }

    @Override
    public Iterator<T> getIterator(String input) {
        return (Iterator<T>) prepareAutoCompleteList(input, getLookupTable(), ((PageBase) getPage()).getLocalizationService()).iterator();
    }

    protected List<String> prepareAutoCompleteList(String input, LookupTableType lookupTable, LocalizationService localizationService) {
        return WebComponentUtil.prepareAutoCompleteList(lookupTable, input, localizationService);
    }

}
