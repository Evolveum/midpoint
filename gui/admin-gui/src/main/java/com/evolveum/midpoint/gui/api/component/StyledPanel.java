/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class StyledPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    public StyledPanel(@NotNull String id, IModel<T> model, IModel<String> cssClass) {
        this(id, model, cssClass, null);
    }

    public StyledPanel(@NotNull String id, IModel<T> model, IModel<String> cssClass, IModel<String> title) {
        super(id, model);

        if (cssClass != null) {
            add(AttributeAppender.append("class", cssClass));
        }

        if (title != null) {
            add(AttributeAppender.append("title", title));
        }
    }
}
