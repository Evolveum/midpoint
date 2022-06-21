/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictItemPanel extends BasePanel<Conflict> {

    private static final long serialVersionUID = 1L;

    private static final String ID_BADGE = "badge";
    private static final String ID_LINK1 = "link1";
    private static final String ID_LINK2 = "link2";
    private static final String ID_MESSAGE = "message";

    public ConflictItemPanel(String id, IModel<Conflict> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card conflict-item"));
        add(AttributeAppender.append("class", () -> {
            Conflict c = getModelObject();
            switch (c.getState()) {
                case SKIPPED:
                    return "conflict-item-secondary";
                case SOLVED:
                    return "conflict-item-success";
            }

            return c.isWarning() ? "conflict-item-warning" : "conflict-item-danger";
        }));

        BadgePanel badge = new BadgePanel(ID_BADGE, () -> {
            Conflict c = getModelObject();
            Badge b = new Badge();
            b.setCssClass(c.isWarning() ? Badge.State.WARNING : Badge.State.DANGER);

            String key = c.isWarning() ? "ConflictItemPanel.badgeWarning" : "ConflictItemPanel.badgeFatalConflict";
            b.setText(getString(key));

            return b;
        });
        add(badge);

        AjaxButton link1 = new AjaxButton(ID_LINK1, () -> getModelObject().getAdded().getName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {

            }
        };
        add(link1);

        AjaxButton link2 = new AjaxButton(ID_LINK2, () -> getModelObject().getExclusion().getName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {

            }
        };
        add(link2);

        Label message = new Label(ID_MESSAGE, () -> getModelObject().getMessage());
        add(message);
    }
}
