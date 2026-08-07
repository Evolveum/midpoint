/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;


import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;

public abstract class FilterableSearchItemWrapper<T> extends AbstractSearchItemWrapper<T> {

    public abstract <C> ObjectFilter createFilter(Class<C> type, PageBase pageBase, VariablesMap variables);

}
