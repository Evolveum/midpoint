/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public abstract ObjectFilter createFilter(PageBase pageBase, ExpressionVariables variables);

    public abstract SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer);

}
