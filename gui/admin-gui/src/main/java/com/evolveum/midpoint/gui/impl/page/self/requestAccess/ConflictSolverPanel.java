/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictSolverPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    public enum ConflictState {
        UNRESOLVED, SOLVED, SKIPPED
    }

    private static final String ID_TOGGLE = "toggle";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    public ConflictSolverPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        IModel<List<Toggle<ConflictState>>> toggleModel = new LoadableModel<>(false) {
            @Override
            protected List<Toggle<ConflictState>> load() {
                List<Toggle<ConflictState>> list = new ArrayList<>();

                for (ConflictState cs : ConflictState.values()) {
                    Toggle<ConflictState> t = new Toggle<>(null, getString(cs));
                    if (cs == ConflictState.UNRESOLVED) {
                        t.setActive(true);
                        t.setBadge("2");
                        t.setBadgeCss("badge badge-danger");
                    }
                    t.setValue(cs);

                    list.add(t);
                }

                return list;
            }
        };

        TogglePanel toggle = new TogglePanel(ID_TOGGLE, toggleModel);
        add(toggle);

        ListView items = new ListView<>(ID_ITEMS) {

            @Override
            protected void populateItem(ListItem item) {
                item.add(new Label(ID_ITEM, () -> "asdf"));
            }
        };
        add(items);
    }
}
