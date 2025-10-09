/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByBorder;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByLink;

/**
 * @author lazyman
 */
public abstract class BasicOrderByBorder extends OrderByBorder {

    protected BasicOrderByBorder(String id, Object property, ISortStateLocator stateLocator) {
        super(id, property, stateLocator);
    }

    @Override
    protected OrderByLink newOrderByLink(String id, Object property, ISortStateLocator stateLocator) {
        return new OrderByLink(id, property, stateLocator) {

            @Override
            protected void onSortChanged() {
                BasicOrderByBorder.this.onSortChanged();
            }
        };
    }


}
