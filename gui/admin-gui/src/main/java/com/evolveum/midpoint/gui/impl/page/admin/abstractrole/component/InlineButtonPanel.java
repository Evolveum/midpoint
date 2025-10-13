/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class InlineButtonPanel extends BasePanel<String> {

    private static final String ID_PANEL_BUTTONS = "buttons";

    public InlineButtonPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        RepeatingView panelButton = new RepeatingView(ID_PANEL_BUTTONS);
        add(panelButton);

        addPanelButton(panelButton);

    }

    protected void addPanelButton(RepeatingView repeatingView) {

    }


}
