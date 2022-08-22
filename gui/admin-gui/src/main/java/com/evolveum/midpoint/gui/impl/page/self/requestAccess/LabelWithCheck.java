/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.tile.CatalogTile;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LabelWithCheck extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "text";
    private static final String ID_CHECK = "check";

    public LabelWithCheck(String id, IModel<String> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class","d-flex align-items-center gap-2"));

        Label text = new Label(ID_TEXT, getModel());
        add(text);

        WebMarkupContainer check = new WebMarkupContainer(ID_CHECK);
        check.add(AttributeAppender.append("class", () -> {
            // todo fix this copy pasted code, fix styles & reuse somehow catalog tile css
            CatalogTile.CheckState state = CatalogTile.CheckState.FULL;

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
        }));
        add(check);
    }
}
