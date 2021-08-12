/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import org.apache.wicket.model.IModel;

public class SimpleCounter {

    private IModel<? extends PrismContainerWrapper<?>> model;

    public SimpleCounter(IModel<? extends PrismContainerWrapper<?>> model) {
        this.model = model;
    }

    public IModel<? extends PrismContainerWrapper<?>> getModel() {
        return model;
    }
}
