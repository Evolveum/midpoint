/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RoleAnalysisTabbedPanel<T extends ITab> extends TabbedPanel<ITab> {

    public RoleAnalysisTabbedPanel(String id, List<ITab> tabs) {
        super(id, tabs);
    }

    public RoleAnalysisTabbedPanel(String id, List<ITab> tabs, @Nullable RightSideItemProvider rightSideItemProvider) {
        super(id, tabs, rightSideItemProvider);
    }

    public RoleAnalysisTabbedPanel(String id, List<ITab> tabs, IModel<Integer> model, @Nullable RightSideItemProvider rightSideItemProvider) {
        super(id, tabs, model, rightSideItemProvider);
    }

    public RoleAnalysisTabbedPanel(String id, IModel<List<ITab>> tabs) {
        super(id, tabs);
    }

    public RoleAnalysisTabbedPanel(String id, IModel<List<ITab>> tabs, IModel<Integer> model, RightSideItemProvider rightSideItemProvider) {
        super(id, tabs, model, rightSideItemProvider);
    }
}
