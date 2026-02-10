/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Wizard-style tile with status badge and optional lock indicator.
 * Renders icon, title, description plus extra header elements (badge/lock),
 * matching the association wizard tile layout.
 */
public class WizardAssociationTilePanel<T extends Tile<O>, O extends Serializable> extends TilePanel<T, O> {

    private static final String ID_BADGE = "badge";
    private static final String ID_LOCK = "lock";

    public WizardAssociationTilePanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        add(AttributeAppender.append("style", "min-height: 250px;"));

        if (getBadgeModel() == null || getBadgeModel().getObject() == null) {
            EmptyPanel badgePanel = new EmptyPanel(ID_BADGE);
            add(badgePanel);
        } else {
            BadgePanel badge = new BadgePanel(ID_BADGE, getBadgeModel());
            badge.setOutputMarkupId(true);
            add(badge);
        }

        WebMarkupContainer lock = new WebMarkupContainer(ID_LOCK);
        lock.setOutputMarkupId(true);
        lock.add(AttributeAppender.append("class", "fa fa-lock"));
        lock.add(new VisibleBehaviour(this::isLocked));
        add(lock);

        add(AttributeAppender.append("class", () -> isLocked() ? "disabled" : null));
    }

    @Override
    protected void appendTileDefaultCssClass() {
        add(AttributeAppender.append("class", "tile-panel d-flex flex-column align-items-center rounded p-3"));
    }

    @Override
    protected void appendTitleCssClass(@NotNull Label title) {
        title.add(AttributeAppender.append("class", "text-center"));
    }

    /** Override or bind to model state. */
    protected IModel<Badge> getBadgeModel() {
        return null;
    }

    /** Return CSS for the badge, e.g. "badge badge-outline-success". */
    protected String getBadgeCss() {
        return "badge badge-outline-secondary";
    }

    /** When true, show lock and visually disable tile. */
    protected boolean isLocked() {
        return false;
    }

    @Override
    protected void onClick(AjaxRequestTarget target) {
        if (isLocked()) {
            return;
        }
        super.onClick(target);
    }
}
