/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.tile.RoundedIconPanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LabelWithCheck extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "text";
    private static final String ID_CHECK = "check";

    private IModel<RoundedIconPanel.State> state;

    private IModel<String> stateTitle;

    public LabelWithCheck(String id, IModel<String> model, IModel<RoundedIconPanel.State> state, IModel<String> stateTitle) {
        super(id, model);

        this.state = state;
        this.stateTitle = stateTitle;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex align-items-center gap-2"));

        Label text = new Label(ID_TEXT, getModel());
        add(text);

        RoundedIconPanel check = new RoundedIconPanel(ID_CHECK, () -> "fa fa-check", state, stateTitle);
        add(check);
    }
}
