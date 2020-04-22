/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.*;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

/**
 * @author katka
 *
 */
public class PrismContainerPanelContext<C extends Containerable> extends ItemPanelContext<C, PrismContainerWrapper<C>>{

    private IModel<PrismContainerValueWrapper<C>> valueWrapperModel;

    public PrismContainerPanelContext(IModel<PrismContainerWrapper<C>> itemWrapper) {
        super(itemWrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public <VW extends PrismValueWrapper<C,?>> void setRealValueModel(IModel<VW> valueWrapper) {
        super.setRealValueModel(valueWrapper);
        this.valueWrapperModel = (IModel<PrismContainerValueWrapper<C>>) valueWrapper;
    }

    public IModel<PrismContainerValueWrapper<C>> getValueWrapper() {
        return valueWrapperModel;
    }


}
