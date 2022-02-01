/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.model;

import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.wicket.model.IModel;

import java.util.Collection;

public class DisplayableModel<T> implements IModel<DisplayableValue<T>> {

    private IModel<T> realValueModel;
    private Collection<? extends DisplayableValue<T>> choices;

    public DisplayableModel(IModel<T> realValueModel, Collection<? extends DisplayableValue<T>> choices) {
        this.realValueModel = realValueModel;
        this.choices = choices;
    }

    @Override
    public DisplayableValue<T> getObject() {
        Object value = realValueModel.getObject();
        for (DisplayableValue<T> dispValue : choices) {
            if (dispValue.getValue().equals(value)) {
                return dispValue;
            }
        }
        return null;
    }

    @Override
    public void setObject(DisplayableValue<T> object) {
        realValueModel.setObject(object.getValue());
    }

}
