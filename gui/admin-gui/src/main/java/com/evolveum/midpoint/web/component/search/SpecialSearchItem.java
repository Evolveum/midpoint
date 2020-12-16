/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.query.ObjectFilter;

import java.io.Serializable;

/**
 * @author lskublik
 */
public abstract class SpecialSearchItem extends SearchItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public SpecialSearchItem(Search search) {
        super(search);
    }

    @Override
    public Type getSearchItemType() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    public abstract ObjectFilter createFilter();

    public abstract SearchSpecialItemPanel createSpecialSearchPanel(String id);

}
