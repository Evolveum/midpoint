/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.danekja.java.util.function.serializable.SerializablePredicate;

/**
 * Filter of an attribute table that keeps only the rows meeting its condition while it is on.
 *
 * @param <T> row type of the table
 */
public record AttributesFilter<T>(IModel<String> label, IModel<Boolean> on, IModel<Boolean> visible,
                                  SerializablePredicate<T> condition) implements Serializable {

    public AttributesFilter(IModel<String> label, SerializablePredicate<T> condition) {
        this(label, () -> true, condition);
    }

    /**
     * Creates a filter that is off.
     *
     * @param label label of the filter
     * @param visible whether the filter is offered
     * @param condition condition a row must meet while the filter is on
     */
    public AttributesFilter(IModel<String> label, IModel<Boolean> visible, SerializablePredicate<T> condition) {
        this(label, Model.of(false), visible, condition);
    }

    /**
     * Tells whether the filter is offered and turned on.
     *
     * @return true if the filter is applied
     */
    public boolean isApplied() {
        return Boolean.TRUE.equals(visible.getObject()) && Boolean.TRUE.equals(on.getObject());
    }
}
