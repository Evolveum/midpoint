/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
