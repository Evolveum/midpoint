/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public abstract class SearchFilterTypeModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    private IModel<SearchFilterType> baseModel;
    private PageBase pageBase;

    public SearchFilterTypeModel(IModel<SearchFilterType> valueWrapper, PageBase pageBase) {
        this.baseModel = valueWrapper;
        this.pageBase = pageBase;
    }

    protected IModel<SearchFilterType> getBaseModel() {
        return baseModel;
    }

    protected PageBase getPageBase() {
        return pageBase;
    }
}
