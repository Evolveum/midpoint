/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.Set;

import static com.evolveum.midpoint.schema.DefinitionProcessingOption.FULL;
import static com.evolveum.midpoint.schema.DefinitionProcessingOption.ONLY_IF_EXISTS;

public class RepositoryShadowBeanObjectDataProvider extends SelectableBeanObjectDataProvider<ShadowType> {

    public RepositoryShadowBeanObjectDataProvider(Component component, IModel<Search<ShadowType>> search, Set<ShadowType> selected) {
        super(component, search, selected);
        setEmptyListOnNullQuery(true);
        setSort(null);
        setDefaultCountIfNull(Integer.MAX_VALUE);
    }

    public RepositoryShadowBeanObjectDataProvider(Component component, Set<ShadowType> selected) {
        this(component, null, selected);
    }



    @Override
    protected GetOperationOptionsBuilder postProcessOptions(GetOperationOptionsBuilder optionsBuilder) {
        optionsBuilder = optionsBuilder
                .item(ShadowType.F_ASSOCIATION).dontRetrieve()
                .noFetch();

        if (isEmptyListOnNullQuery()) {
            return optionsBuilder
                    .definitionProcessing(ONLY_IF_EXISTS)
                    .item(ShadowType.F_FETCH_RESULT).definitionProcessing(FULL)
                    .item(ShadowType.F_AUXILIARY_OBJECT_CLASS).definitionProcessing(FULL);
        }
        return super.postProcessOptions(optionsBuilder);
    }
}
