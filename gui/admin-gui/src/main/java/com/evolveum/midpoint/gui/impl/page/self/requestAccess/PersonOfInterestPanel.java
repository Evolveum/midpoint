/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_MYSELF = "myself";
    private static final String ID_GROUP = "group";
    private static final String ID_TEAM = "team";
    private static final String ID_BACK = "back";
    private static final String ID_NEXT = "next";

    public PersonOfInterestPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        add(new TilePanel(ID_MYSELF, () -> new Tile("fas fa-user-circle", "Myself")));
        add(new TilePanel(ID_GROUP, () -> new Tile("fas fa-user-friends", "Group/Others")));
        add(new TilePanel(ID_TEAM, () -> new Tile("fas fa-users", "Team")));

        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        add(back);

        AjaxLink next = new AjaxLink<>(ID_NEXT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        add(next);
    }

    protected void onNextPerformed(AjaxRequestTarget target) {

    }

    protected void onBackPerformed(AjaxRequestTarget target) {

    }
}
