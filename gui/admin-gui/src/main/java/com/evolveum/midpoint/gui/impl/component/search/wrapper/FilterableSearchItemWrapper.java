/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;


import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.panel.AbstractSearchItemPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import java.io.Serializable;
import java.util.Objects;

public abstract class FilterableSearchItemWrapper<T> extends AbstractSearchItemWrapper<T> {

    public abstract <C> ObjectFilter createFilter(Class<C> type, PageBase pageBase, VariablesMap variables);

}
