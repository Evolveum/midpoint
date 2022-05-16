/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.result;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsPanel extends Border {

    private static final long serialVersionUID = 1L;

    private static final String ID_LABEL = "label";

    private IModel<String> label;

    public DetailsPanel(String id, IModel<String> label) {
        super(id);

        this.label = label;

        initLayout();
    }

    private void initLayout() {
        Label title = new Label(ID_LABEL, label);
        addToBorder(title);
    }
}
