/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CardOutlineLeftPanel<T extends Serializable> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String FRAGMENT_ID_HEADER = "header";
    private static final String FRAGMENT_ID_BODY_HEADER = "bodyHeader";

    private static final String ID_HEADER = "header";
    private static final String ID_BODY_HEADER = "bodyHeader";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_BADGE = "badge";

    public CardOutlineLeftPanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card card-outline-left"));
        add(AttributeAppender.append("class", createCardOutlineCssModel()));

        Component header = createHeader(ID_HEADER);
        add(header);

        Component bodyHeader = createBodyHeader(ID_BODY_HEADER);
        add(bodyHeader);
    }

    protected Component createHeader(String id) {
        Fragment fragment = new Fragment(id, FRAGMENT_ID_HEADER, this);
        fragment.add(new Label(ID_TITLE, createTitleModel()));
        fragment.add(VisibleBehaviour.ALWAYS_INVISIBLE);

        return fragment;
    }

    protected Component createBodyHeader(String id) {
        Fragment fragment = new Fragment(id, FRAGMENT_ID_BODY_HEADER, this);
        fragment.add(AttributeAppender.append("class", "d-flex gap-2 align-items-center border-bottom border-gray pb-3 mb-3"));

        IModel<String> iconModel = createIconModel();
        IconComponent icon = new IconComponent(ID_ICON, iconModel);
        icon.add(new VisibleBehaviour(() -> iconModel.getObject() != null));
        fragment.add(icon);

        Label title = new Label(ID_TITLE, createTitleModel());
        fragment.add(title);

        IModel<Badge> badgeModel = createBadgeModel();
        BadgePanel badge = new BadgePanel(ID_BADGE, badgeModel);
        badge.add(new VisibleBehaviour(() -> badgeModel.getObject() != null));
        fragment.add(badge);

        return fragment;
    }

    protected @NotNull IModel<String> createIconModel() {
        return () -> null;
    }

    protected @NotNull IModel<String> createCardOutlineCssModel() {
        return () -> null;
    }

    protected @NotNull IModel<String> createTitleModel() {
        return () -> null;
    }

    protected @NotNull IModel<Badge> createBadgeModel() {
        return () -> null;
    }
}
