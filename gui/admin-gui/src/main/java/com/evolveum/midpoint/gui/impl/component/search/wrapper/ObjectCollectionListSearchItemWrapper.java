/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ObjectCollectionListSearchItemPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectCollectionListSearchItemWrapper extends FilterableSearchItemWrapper<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectCollectionSearchItemWrapper.class);
    private Class<?> type;
    Map<String, CompiledObjectCollectionView> collectionViewLightMap = new HashMap<>();

    public ObjectCollectionListSearchItemWrapper(@NotNull Class<?> type, List<CompiledObjectCollectionView> collectionViews) {
        this.type = type;
        initCollectionViews(collectionViews);
    }

    private void initCollectionViews(List<CompiledObjectCollectionView> collectionViews) {
        if (collectionViews == null) {
            return;
        }
        collectionViews.forEach(view -> {
            CompiledObjectCollectionView lightView = new CompiledObjectCollectionView();
            lightView.setFilter(view.getFilter());
            lightView.setViewIdentifier(view.getViewIdentifier());
            collectionViewLightMap.put(getObjectCollectionViewLabel(view), lightView);
        });
    }

    private String getObjectCollectionViewLabel (CompiledObjectCollectionView view) {
        return view.getDisplay() != null && view.getDisplay().getLabel() != null ?
                WebComponentUtil.getTranslatedPolyString(view.getDisplay().getLabel()) : view.getViewIdentifier();
    }

    public List<DisplayableValue<String>> getViewNameList() {
        return collectionViewLightMap.keySet().stream().map(name -> new SearchValue<>(name)).collect(Collectors.toList());
    }

    @Override
    public Class<ObjectCollectionListSearchItemPanel> getSearchItemPanelClass() {
        return ObjectCollectionListSearchItemPanel.class;
    }

    @Override
    public IModel<String> getName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return LocalizationUtil.translate("ObjectTypeGuiDescriptor.objectCollection");
            }
        };
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of();
    }

    @Override
    public IModel<String> getHelp() {
        return Model.of();
    }


    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        String value = getValue().getValue();
        if (value == null) {
            return null;
        }
        if (variables == null) {
            variables = new VariablesMap();
        }
        return WebComponentUtil.evaluateExpressionsInFilter(collectionViewLightMap.get(value).getFilter(), variables,
                new OperationResult("evaluate filter"), pageBase);
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

}
