/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.error;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * @author katka
 *
 */
public class ErrorPanel extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ERROR = "error";

    public ErrorPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label errorLabel = new Label(ID_ERROR, getModel());
        add(errorLabel);

    }


}
