/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.todo;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;

// todo refactor ConflictItemPanel to be subclass of this, move css classes "under" this one (rename them to match)
/**
 * Created by Viliam Repan (lazyman).
 */
public class CardPanel<T extends Serializable> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String FRAGMENT_ID_HEADER = "header";

    private static final String ID_HEADER = "header";
    private static final String ID_TITLE = "title";
    private static final String ID_BADGE = "badge";

    public CardPanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card conflict-item"));
        add(AttributeAppender.append("class", () -> "conflict-item-success")); // todo fix viliam

        Component header = createHeader(ID_HEADER);
        add(header);
    }

    protected Component createHeader(String id) {
        Fragment fragment = new Fragment(id, FRAGMENT_ID_HEADER, this);
        fragment.add(new Label(ID_TITLE, () -> "asdf"));  // todo title
        fragment.add(new BadgePanel(ID_BADGE, () -> new Badge(Badge.State.SUCCESS.getCss(), "badge"))); // todo badge

        return fragment;
    }
}
