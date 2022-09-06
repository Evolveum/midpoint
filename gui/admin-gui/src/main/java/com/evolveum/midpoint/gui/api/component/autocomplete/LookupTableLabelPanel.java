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

public class LookupTableLabelPanel extends Label {

    private String lookupTableOid;

    public LookupTableLabelPanel(String id, IModel<?> model, String lookupTableOid) {
        super(id, model);
        this.lookupTableOid = lookupTableOid;
    }

    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        IConverter<C> originConverter = super.getConverter(type);
        if (lookupTableOid == null) {
            return originConverter;
        }

        return new LookupTableConverter<C>(originConverter, lookupTableOid, this, true);
    }
}
