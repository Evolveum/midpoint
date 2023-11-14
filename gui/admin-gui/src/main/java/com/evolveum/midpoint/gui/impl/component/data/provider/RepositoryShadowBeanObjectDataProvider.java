/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import static com.evolveum.midpoint.schema.DefinitionProcessingOption.FULL;
import static com.evolveum.midpoint.schema.DefinitionProcessingOption.ONLY_IF_EXISTS;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class RepositoryShadowBeanObjectDataProvider extends SelectableBeanObjectDataProvider<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryShadowBeanObjectDataProvider.class);

    public RepositoryShadowBeanObjectDataProvider(Component component, IModel<Search<ShadowType>> search, Set<ShadowType> selected) {
        super(component, search, selected);
        setEmptyListOnNullQuery(true);
        setDefaultCountIfNull(Integer.MAX_VALUE);
    }

    public RepositoryShadowBeanObjectDataProvider(Component component, Set<ShadowType> selected) {
        this(component, null, selected);
    }

    @Override
    protected List<ShadowType> searchObjects(Class<ShadowType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
        List<ShadowType> shadows = super.searchObjects(type, query, options, task, result);
        if (!GetOperationOptions.isRaw(options)) {
            return shadows;
        }
        shadows.forEach(this::applyDefinition);
        return shadows;
    }

    private void applyDefinition(ShadowType shadow) {
        Task task1 = getPageBase().createSimpleTask("apply definitions");
        try {
            getPageBase().getModelInteractionService().applyDefinitions(shadow, task1, task1.getResult());
        } catch (Throwable e) {
            LoggingUtils.logException(LOGGER, "Couldn't apply definitions to shadow: {}", e);
        }
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        Collection<SelectorOptions<GetOperationOptions>> options = super.getOptions();

        if (options == null) {
            options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        } else {
            GetOperationOptions root = SelectorOptions.findRootOptions(options);
            root.setNoFetch(Boolean.TRUE);
        }
        return options;
    }

    @Override
    protected GetOperationOptionsBuilder postProcessOptions(GetOperationOptionsBuilder optionsBuilder) {
        optionsBuilder = optionsBuilder
                .item(ShadowType.F_ASSOCIATION).dontRetrieve();

        if (isEmptyListOnNullQuery()) {
            return optionsBuilder
                    .definitionProcessing(ONLY_IF_EXISTS)
                    .item(ShadowType.F_FETCH_RESULT).definitionProcessing(FULL)
                    .item(ShadowType.F_AUXILIARY_OBJECT_CLASS).definitionProcessing(FULL);
        }
        return super.postProcessOptions(optionsBuilder);
    }
}
