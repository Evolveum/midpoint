/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.verticalForm;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;

import org.apache.wicket.model.IModel;

public class VerticalFormPrismPropertyValuePanel<T> extends PrismPropertyValuePanel<T> {


    public VerticalFormPrismPropertyValuePanel(String id, IModel<PrismPropertyValueWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    protected boolean isRemoveButtonVisible() {
        return false; //TODO fix it
//        if (getModelObject() != null && getModelObject().getValueMetadata().isSingleValue()) {
//            return false;
//        }
//        return super.isRemoveButtonVisible();
    }
}
