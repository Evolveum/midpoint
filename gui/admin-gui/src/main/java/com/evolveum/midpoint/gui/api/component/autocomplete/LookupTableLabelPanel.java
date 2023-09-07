/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class LookupTableLabelPanel extends Label {


    private final String lookupTableOid;

    public LookupTableLabelPanel(String id, IModel<?> model, String lookupTableOid) {
        super(id, model);
        this.lookupTableOid = lookupTableOid;
    }

    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        return new LookupTableConverter<C>(super.getConverter(type), this, true){
            @Override
            protected LookupTableType getLookupTable() {
                if (lookupTableOid != null) {
                    return WebModelServiceUtils.loadLookupTable(lookupTableOid, getPageBase());
                }
                return null;
            }
        };
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }
}
