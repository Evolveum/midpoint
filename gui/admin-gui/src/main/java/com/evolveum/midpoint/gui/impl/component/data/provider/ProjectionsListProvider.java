/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
