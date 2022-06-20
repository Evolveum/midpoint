/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

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

    public ConflictItemPanel(String id, IModel<Conflict> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        BadgePanel badge = new BadgePanel(ID_BADGE, () -> {
            Conflict c = getModelObject();
            Badge b = new Badge();
            b.setCssClass(c.isWarning() ? Badge.State.WARNING : Badge.State.DANGER);

            String key = c.isWarning() ? "ConflictItemPanel.badgeWarning" : "ConflictItemPanel.badgeFatalConflict";
            b.setText(getString(key));

            return b;
        });
        add(badge);
    }
}
