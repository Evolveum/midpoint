/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.widget;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WidgetType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class WidgetPanel<T extends WidgetType> extends BasePanel<T> {

    public WidgetPanel(@NotNull String id, @NotNull IModel<T> model) {
        super(id, model);
    }
}
