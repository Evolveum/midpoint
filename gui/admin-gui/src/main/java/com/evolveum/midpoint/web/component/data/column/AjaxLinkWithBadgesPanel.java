/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lazyman
 */
public class AjaxLinkWithBadgesPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGES = "badges";

    public AjaxLinkWithBadgesPanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        AjaxLink<String> link = new AjaxLink<>(ID_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkWithBadgesPanel.this.onClick(target);
            }

        };
        Label label = new Label(ID_LABEL, getModel());

        link.add(label);
        link.add(new EnableBehaviour(() -> AjaxLinkWithBadgesPanel.this.isEnabled()));
        add(link);

        IModel<List<Badge>> badgesModel = createBadgesModel();
        BadgeListPanel badges = new BadgeListPanel(ID_BADGES, badgesModel);
        badges.add(new VisibleBehaviour(() -> {
            List<Badge> list = badgesModel.getObject();
            return list != null && !list.isEmpty();
        }));
        add(badges);
    }

    protected IModel<List<Badge>> createBadgesModel() {
        return () -> List.of();
    }

    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }
}
