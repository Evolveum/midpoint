/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.Collection;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.util.DisplayableValue;

/**
 * @author katka
 *
 */
public class PrismPropertyPanelContext<T> extends ItemPanelContext<T, PrismPropertyWrapper<T>>{


    public PrismPropertyPanelContext(IModel<PrismPropertyWrapper<T>> itemWrapper) {
        super(itemWrapper);
    }

    public String getPredefinedValuesOid() {
        return unwrapWrapperModel().getPredefinedValuesOid();
    }

    public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return unwrapWrapperModel().getAllowedValues();
    }

    public Collection<? extends DisplayableValue<T>> getSuggestedValues() {
        return unwrapWrapperModel().getSuggestedValues();
    }

    public boolean hasValueEnumerationRef() {
        return unwrapWrapperModel().getValueEnumerationRef() != null;
    }

    public IModel<String> getRealValueStringModel() {
        return new ReadOnlyModel<>(() -> getValueWrapperModel().getObject().toShortString());
    }
}
