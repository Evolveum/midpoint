/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;

import org.apache.wicket.model.IModel;

public class AbstractSearchItemPanel<W extends AbstractSearchItemWrapper> extends BasePanel<W> {

    public AbstractSearchItemPanel(String id, IModel<W> model) {
        super(id, model);
    }
}
