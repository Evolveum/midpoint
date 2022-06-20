/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.model.Model;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictSolverPanel extends BasePanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private static final String BADGE_COLOR_UNRESOLVED = "badge badge-danger";
    private static final String BADGE_COLOR_RESOLVED = "badge badge-success";
    private static final String BADGE_COLOR_SKIPPED = "badge badge-info";

    private static final String ID_TOGGLE = "toggle";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    private IModel<ConflictState> selected = Model.of((ConflictState) null);

    public ConflictSolverPanel(String id, IModel<RequestAccess> model) {
        super(id, model);

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
                        long count = getModelObject().getConflicts().stream().filter(c -> ConflictState.UNRESOLVED.equals(c.getState())).count();
                        if (count > 0) {
                            t.setBadgeCss("badge badge-danger");
                            t.setBadge(Long.toString(count));
                        }
                    }
                    t.setValue(cs);

                    list.add(t);
                }

                return list;
            }
        };

        TogglePanel toggle = new TogglePanel(ID_TOGGLE, toggleModel);
        add(toggle);

        ListView<Conflict> items = new ListView<>(ID_ITEMS, () -> getModelObject().getConflicts()) {

            @Override
            protected void populateItem(ListItem<Conflict> item) {
                item.add(new ConflictItemPanel(ID_ITEM, item.getModel()));
            }
        };
        add(items);
    }
}
