/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
