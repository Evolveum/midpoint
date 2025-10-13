/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProjectionsListProvider extends MultivalueContainerListDataProvider<ShadowType> {

    public ProjectionsListProvider(Component component, @NotNull IModel<Search<ShadowType>> search, IModel<List<PrismContainerValueWrapper<ShadowType>>> model) {
        super(component, search, model);
    }

}
