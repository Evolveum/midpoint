/*
 * Copyright (C) 2020-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.api.component.BasePanel;

public class OperationalButtonsPanel extends BasePanel<Void> {

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STATE_BUTTONS = "stateButtons";

    public OperationalButtonsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        RepeatingView repeatingView = new RepeatingView(ID_BUTTONS);
        add(repeatingView);

        addButtons(repeatingView);

        RepeatingView stateButtonsView = new RepeatingView(ID_STATE_BUTTONS);
        add(stateButtonsView);

        addStateButtons(stateButtonsView);
    }

    protected void addButtons(RepeatingView repeatingView) {

    }

    protected void addStateButtons(RepeatingView stateButtonsView) {

    }

    public boolean buttonsExist(){
        RepeatingView repeatingView = (RepeatingView) get(ID_BUTTONS);
        boolean buttonsExist = repeatingView != null && repeatingView.iterator().hasNext();
        if (buttonsExist) {
            Iterator<Component> buttonsIt = repeatingView.iterator();
            while (buttonsIt.hasNext()) {
                Component comp = buttonsIt.next();
                comp.configure();
                if (comp.isVisible()){
                    return true;
                }
            }
        }
        return false;
    }
}
