/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider.suggestion;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.MappingDataDto;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupedMappingDataProvider extends BaseSortableDataProvider<MappingDataDto> {

    private final ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> delegate;
    private final boolean groupSuggestions;

    private final Map<String, MappingDataDto> dtoCache = new LinkedHashMap<>();

    /** Cache for already loaded delegate rows within current request/detach cycle. */
    private List<PrismContainerValueWrapper<MappingType>> loadedRowsCache;

    /** Cache for grouped/flat DTO result within current request/detach cycle. */
    private List<MappingDataDto> groupedDataCache;

    public GroupedMappingDataProvider(
            @NotNull Component component,
            @NotNull ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> delegate,
            boolean groupSuggestions) {
        super(component);
        this.delegate = delegate;
        this.groupSuggestions = groupSuggestions;
        setSort(null);
    }

    protected void applySortToDelegate() {
        if (delegate instanceof StatusAwareDataProvider<MappingType> statusAwareDataProvider) {
            statusAwareDataProvider.setSort(getSort());
        }
    }

    @Override
    public @NotNull Iterator<? extends MappingDataDto> internalIterator(long first, long count) {
        List<MappingDataDto> data = getGroupedData();

        int from = (int) first;
        if (from >= data.size()) {
            return Collections.emptyIterator();
        }

        int to = Math.min(from + (int) count, data.size());
        return data.subList(from, to).iterator();
    }

    @Override
    protected int internalSize() {
        return getGroupedData().size();
    }

    @Override
    public @NotNull IModel<MappingDataDto> model(MappingDataDto object) {
        return Model.of(object);
    }

    @Override
    public void detach() {
        super.detach();

        loadedRowsCache = null;
        groupedDataCache = null;

        if (delegate instanceof StatusAwareDataProvider<MappingType> statusAwareDataProvider) {
            statusAwareDataProvider.getModel().detach();
        }
    }

    private @NotNull List<MappingDataDto> getGroupedData() {
        if (groupedDataCache != null) {
            return groupedDataCache;
        }

        List<PrismContainerValueWrapper<MappingType>> all = loadAllData();
        if (all.isEmpty()) {
            dtoCache.clear();
            groupedDataCache = List.of();
            return groupedDataCache;
        }

        groupedDataCache = groupSuggestions ? createGroupedData(all) : createFlatData(all);
        return groupedDataCache;
    }

    public StatusInfo<?> getSuggestionInfo(@NotNull PrismContainerValueWrapper<?> wrapper) {
        if (delegate instanceof StatusAwareDataProvider<MappingType> statusAwareDataProvider) {
            //noinspection unchecked
            return statusAwareDataProvider.getSuggestionInfo((PrismContainerValueWrapper<MappingType>) wrapper);
        }
        return null;
    }

    private @NotNull List<MappingDataDto> createGroupedData(
            @NotNull List<PrismContainerValueWrapper<MappingType>> all) {

        Map<String, List<PrismContainerValueWrapper<MappingType>>> grouped = new LinkedHashMap<>();
        List<MappingDataDto> result = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (PrismContainerValueWrapper<MappingType> wrapper : all) {
            String key;
            if (getSuggestionInfo(wrapper) != null) {
                key = resolveGroupingKey(wrapper);
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(wrapper);
            } else {
                key = resolveRowKey(wrapper);
                grouped.put(key, List.of(wrapper));
            }
        }

        for (Map.Entry<String, List<PrismContainerValueWrapper<MappingType>>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<PrismContainerValueWrapper<MappingType>> wrappers = entry.getValue();

            MappingDataDto dto = dtoCache.get(key);
            if (dto == null) {
                dto = new MappingDataDto(key, wrappers, getGroupName());
                dtoCache.put(key, dto);
            } else {
                dto.setMappings(wrappers);
            }

            seenKeys.add(key);
            result.add(dto);
        }

        dtoCache.keySet().retainAll(seenKeys);
        return result;
    }

    protected String getGroupName() {
        return "target";
    }

    private @NotNull List<MappingDataDto> createFlatData(
            @NotNull List<PrismContainerValueWrapper<MappingType>> all) {

        List<MappingDataDto> result = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (PrismContainerValueWrapper<MappingType> wrapper : all) {
            String key = resolveRowKey(wrapper);

            MappingDataDto dto = dtoCache.get(key);
            if (dto == null) {
                dto = new MappingDataDto(key, List.of(wrapper), getGroupName());
                dtoCache.put(key, dto);
            } else {
                dto.setMappings(List.of(wrapper));
            }

            seenKeys.add(key);
            result.add(dto);
        }

        dtoCache.keySet().retainAll(seenKeys);
        return result;
    }

    private @NotNull List<PrismContainerValueWrapper<MappingType>> loadAllData() {
        if (loadedRowsCache != null) {
            return loadedRowsCache;
        }

        applySortToDelegate();

        long size = delegate.size();
        if (size <= 0) {
            loadedRowsCache = List.of();
            return loadedRowsCache;
        }

        List<PrismContainerValueWrapper<MappingType>> all = new ArrayList<>();
        Iterator<? extends PrismContainerValueWrapper<MappingType>> iterator = delegate.iterator(0, size);
        iterator.forEachRemaining(all::add);

        loadedRowsCache = all;
        return loadedRowsCache;
    }

    private @NotNull String resolveRowKey(@NotNull PrismContainerValueWrapper<MappingType> wrapper) {
        return resolveGroupingKey(wrapper) + "#" + System.identityHashCode(wrapper);
    }

    protected @NotNull String resolveGroupingKey(@NotNull PrismContainerValueWrapper<MappingType> wrapper) {
        MappingType mapping = wrapper.getRealValue();
        if (mapping == null || mapping.getTarget() == null || mapping.getTarget().getPath() == null) {
            return "__null_target__";
        }
        return String.valueOf(mapping.getTarget().getPath().getItemPath());
    }

    public ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> getDelegateProvider() {
        return delegate;
    }
}
