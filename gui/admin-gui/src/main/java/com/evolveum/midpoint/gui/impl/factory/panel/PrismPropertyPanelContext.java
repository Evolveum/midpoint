/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.apache.wicket.model.IModel;

/**
 * @author katka
 *
 */
public class PrismPropertyPanelContext<T> extends ItemPanelContext<T, PrismPropertyWrapper<T>>{


    public PrismPropertyPanelContext(IModel<PrismPropertyWrapper<T>> itemWrapper) {
        super(itemWrapper);
    }

    public LookupTableType getPredefinedValues() {
        return unwrapWrapperModel().getPredefinedValues();
    }

    public boolean hasValueEnumerationRef() {
        return unwrapWrapperModel().getValueEnumerationRef() != null;
    }
}
