/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoundedIconPanel extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";

    public enum State {
        NONE, PARTIAL, FULL
    }

    private IModel<State> state;

    public RoundedIconPanel(String id, IModel<String> icon, IModel<State> state) {
        super(id, icon);

        this.state = state;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.prepend("class", "rounded-icon"));
        add(AttributeAppender.append("class", this::getCssStateClass));

        Label icon = new Label(ID_ICON);
        icon.add(AttributeAppender.append("class", getModel()));
        add(icon);
    }

    private String getCssStateClass() {
        State state = this.state.getObject();
        if (state == null) {
            return "rounded-icon-none";
        }

        switch (state) {
            case FULL:
                return "rounded-icon-full";
            case PARTIAL:
                return "rounded-icon-partial";
            case NONE:
            default:
                return "rounded-icon-none";
        }
    }
}
