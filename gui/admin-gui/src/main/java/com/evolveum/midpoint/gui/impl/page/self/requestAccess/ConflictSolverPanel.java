/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.Badge;

import org.apache.wicket.ajax.AjaxRequestTarget;
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

    private static final String ID_TOGGLE = "toggle";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";

    private IModel<ConflictState> selected = Model.of(ConflictState.UNRESOLVED);

    public ConflictSolverPanel(String id, IModel<RequestAccess> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        IModel<List<Toggle<ConflictState>>> toggleModel = new LoadableModel<>(false) {
            @Override
            protected List<Toggle<ConflictState>> load() {
                List<Toggle<ConflictState>> list = new ArrayList<>();

                for (ConflictState cs : ConflictState.values()) {
                    Toggle<ConflictState> t = new Toggle<>(null, getString(cs));
                    t.setActive(selected.getObject() == cs);

                    if (cs == ConflictState.UNRESOLVED) {
                        long count = getModelObject().getConflicts().stream().filter(c -> ConflictState.UNRESOLVED.equals(c.getState())).count();
                        if (count > 0) {
                            t.setBadgeCss(Badge.State.DANGER.getCss());
                            t.setBadge(Long.toString(count));
                        }
                    }
                    t.setValue(cs);

                    list.add(t);
                }

                return list;
            }
        };

        TogglePanel<ConflictState> toggle = new TogglePanel<>(ID_TOGGLE, toggleModel) {

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ConflictState>> item) {
                super.itemSelected(target, item);

                selected.setObject(item.getObject().getValue());
                target.add(ConflictSolverPanel.this);
            }
        };
        add(toggle);

        IModel<List<Conflict>> listModel = () -> {
            ConflictState state = selected.getObject();

            List<Conflict> all = getModelObject().getConflicts();
            if (state == null) {
                return all;
            }

            return all.stream().filter(c -> state == c.getState()).collect(Collectors.toList());
        };

        ListView<Conflict> items = new ListView<>(ID_ITEMS, listModel) {

            @Override
            protected void populateItem(ListItem<Conflict> item) {
                item.add(new ConflictItemPanel(ID_ITEM, item.getModel()));
            }
        };
        add(items);
    }
}
