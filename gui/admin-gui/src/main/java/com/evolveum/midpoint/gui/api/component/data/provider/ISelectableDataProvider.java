/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.data.provider;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;

import com.evolveum.midpoint.prism.query.ObjectQuery;

public interface ISelectableDataProvider<S> extends ISortableDataProvider<S, String> {

    void setQuery(ObjectQuery query);
}
