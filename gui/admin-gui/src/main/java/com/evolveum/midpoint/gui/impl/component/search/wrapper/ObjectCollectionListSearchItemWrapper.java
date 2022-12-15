/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ObjectCollectionListSearchItemPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.SearchValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectCollectionListSearchItemWrapper<C extends Containerable> extends FilterableSearchItemWrapper<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectCollectionSearchItemWrapper.class);
    private Class<C> type;
    Map<String, CompiledObjectCollectionView> collectionViewLightMap = new HashMap<>();

    public ObjectCollectionListSearchItemWrapper(@NotNull Class<C> type, List<CompiledObjectCollectionView> collectionViews) {
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
    public String getName() {
        return "ObjectTypeGuiDescriptor.objectCollection";
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getHelp() {
        return null;
    }


    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        String value = getValue().getValue();
        if (value == null) {
            return null;
        }
        return collectionViewLightMap.get(value).getFilter();
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

}
