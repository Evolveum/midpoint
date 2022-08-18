/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictSolverPanel extends BasePanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private static final String ID_DONE_CARD = "doneCard";
    private static final String ID_BACK_TO_SUMMARY = "backToSummary";
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

        WebMarkupContainer doneCard = new WebMarkupContainer(ID_DONE_CARD);
        doneCard.add(new VisibleBehaviour(() -> getModelObject().isAllConflictsSolved()));
        add(doneCard);

        AjaxLink backToSummary = new AjaxLink<>(ID_BACK_TO_SUMMARY) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                backToSummaryPerformed(target);
            }
        };
        doneCard.add(backToSummary);

        IModel<List<Toggle<ConflictState>>> toggleModel = () -> {
            List<Toggle<ConflictState>> list = new ArrayList<>();

            for (ConflictState cs : ConflictState.values()) {
                Toggle<ConflictState> t = new Toggle<>(null, getString(cs));
                t.setActive(selected.getObject() == cs);
                t.setValue(cs);

                String badgeCss = null;
                switch (cs) {
                    case UNRESOLVED:
                        badgeCss = Badge.State.DANGER.getCss();
                        break;
                    case SOLVED:
                        badgeCss = Badge.State.SUCCESS.getCss();
                        break;
                    case SKIPPED:
                        badgeCss = Badge.State.PRIMARY.getCss();
                        break;
                }

                long count = getModelObject().getConflicts().stream().filter(c -> cs.equals(c.getState())).count();
                if (count > 0) {
                    t.setBadgeCss(badgeCss);
                    t.setBadge(Long.toString(count));
                }

                list.add(t);
            }

            return list;
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
                item.add(new ConflictItemPanel(ID_ITEM, item.getModel()) {

                    @Override
                    protected void fixConflictPerformed(AjaxRequestTarget target, IModel<ConflictItem> itemToKeep) {
                        ConflictSolverPanel.this.solveConflictPerformed(target, item.getModel(), itemToKeep);
                    }
                });
            }
        };
        add(items);
    }

    protected void solveConflictPerformed(AjaxRequestTarget target, IModel<Conflict> conflictModel, IModel<ConflictItem> itemToKeepModel) {
        Conflict conflict = conflictModel.getObject();
        ConflictItem itemToKeep = itemToKeepModel.getObject();

        ConflictItem toRemove;
        if (Objects.equals(conflict.getAdded(), itemToKeep)) {
            toRemove = conflict.getExclusion();
        } else {
            toRemove = conflict.getAdded();
        }

        getModelObject().solveConflict(conflict, toRemove);

        target.add(this);
    }

    protected void backToSummaryPerformed(AjaxRequestTarget target) {

    }
}
