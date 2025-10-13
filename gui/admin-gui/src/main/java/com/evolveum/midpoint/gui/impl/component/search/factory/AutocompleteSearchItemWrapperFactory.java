/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.AutoCompleteSearchItemWrapper;

public class AutocompleteSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<String, AutoCompleteSearchItemWrapper> {

    @Override
    protected AutoCompleteSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new AutoCompleteSearchItemWrapper(ctx.getPath(), ctx.getLookupTableOid());
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return ctx.getLookupTableOid() != null;
    }
}
