/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.model.IModel;

public abstract class AbstractObjectMainPanel<T> extends BasePanel<T> {

    public AbstractObjectMainPanel(String id, LoadableModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected abstract void initLayout();

}
