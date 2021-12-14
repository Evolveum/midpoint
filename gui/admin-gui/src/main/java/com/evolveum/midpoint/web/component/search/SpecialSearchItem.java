/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class SpecialSearchItem<S extends SpecialSearchItemDefinition, T extends Serializable> extends SearchItem<S> implements Serializable {

    private static final long serialVersionUID = 1L;

    private IModel<T> valueModel;

    public SpecialSearchItem(Search search) {
        this(search, null);
    }

    public SpecialSearchItem(Search search, IModel<T> valueModel) {
        this(search, valueModel, null);
    }

    public SpecialSearchItem(Search search, IModel<T> valueModel, S def) {
        super(search, def);
        this.valueModel = valueModel;
    }

    public IModel<T> getValueModel() {
        return valueModel;
    }

    @Override
    public Type getSearchItemType() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    public abstract ObjectFilter transformToFilter(PageBase pageBase, VariablesMap variables);

    public abstract SearchSpecialItemPanel createSearchItemPanel(String id);

}
