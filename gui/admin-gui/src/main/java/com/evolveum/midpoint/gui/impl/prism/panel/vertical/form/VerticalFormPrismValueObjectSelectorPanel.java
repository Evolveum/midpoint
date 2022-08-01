/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.web.component.form.ValueChoosePanel;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Referencable;

public class VerticalFormPrismValueObjectSelectorPanel<R extends Referencable> extends ValueChoosePanel<R> {

    public VerticalFormPrismValueObjectSelectorPanel(String id, IModel<R> value) {
        super(id, value);
    }
}
