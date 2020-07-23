/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.prism.query.ObjectQuery;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author skublik
 */

public interface ISelectableDataProvider<O, S> extends ISortableDataProvider<S, String> {

    @NotNull
    List<O> getSelectedObjects();

    void setQuery(ObjectQuery query);
}
