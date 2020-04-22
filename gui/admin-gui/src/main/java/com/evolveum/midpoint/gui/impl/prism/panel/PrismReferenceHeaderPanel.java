/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;

/**
 * @author katka
 *
 */
public class PrismReferenceHeaderPanel<R extends Referencable> extends ItemHeaderPanel<PrismReferenceValue, PrismReference, PrismReferenceDefinition, PrismReferenceWrapper<R>> {

    private static final long serialVersionUID = 1L;

    public PrismReferenceHeaderPanel(String id, IModel<PrismReferenceWrapper<R>> model) {
        super(id, model);
    }


    @Override
    protected void initButtons() {
        // TODO Auto-generated method stub

    }


    @Override
    protected Component createTitle(IModel<String> label) {
        Label displayName = new Label(ID_LABEL, label);

        return displayName;
    }

}
