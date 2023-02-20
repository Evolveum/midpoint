/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class IconComponent extends WebComponent {

    public IconComponent(@NotNull String id, @NotNull IModel<String> cssClass) {
        this(id, cssClass, null);
    }

    public IconComponent(@NotNull String id, @NotNull IModel<String> cssClass, IModel<String> title) {
        super(id);

        add(AttributeAppender.append("class", cssClass));

        if (title != null) {
            add(AttributeAppender.append("title", title));
        }
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "i");
    }
}
