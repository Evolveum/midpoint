/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SearchConfigurationMerger {

    public static SearchBoxConfigurationType mergeConfigurations(SearchBoxConfigurationType defaultConfig, SearchBoxConfigurationType customizedConfig, ModelServiceLocator pageBase) {
        if (customizedConfig == null) {
            return defaultConfig.clone();
        }

        SearchBoxConfigurationType mergedConfig = defaultConfig.clone();

        if (!customizedConfig.getAllowedMode().isEmpty()) {
            mergedConfig.getAllowedMode().clear();
            mergedConfig.getAllowedMode().addAll(customizedConfig.getAllowedMode());
        }

        if (!customizedConfig.getAvailableFilter().isEmpty()) {
            mergedConfig.getAvailableFilter().clear();
            mergedConfig.getAvailableFilter().addAll(CloneUtil.cloneCollectionMembers(customizedConfig.getAvailableFilter()));
        }

        if (customizedConfig.getIndirectConfiguration() != null) {
            mergedConfig.setIndirectConfiguration(customizedConfig.getIndirectConfiguration());
        }

        if (customizedConfig.getProjectConfiguration() != null) {
            mergedConfig.setProjectConfiguration(customizedConfig.getProjectConfiguration());
        }

        if (customizedConfig.getScopeConfiguration() != null) {
            mergedConfig.setScopeConfiguration(combineScopeSearchItem(mergedConfig.getScopeConfiguration(), customizedConfig.getScopeConfiguration()));
        }

        if (customizedConfig.getDefaultMode() != null) {
            mergedConfig.setDefaultMode(customizedConfig.getDefaultMode());
        }

        if (customizedConfig.getObjectTypeConfiguration() != null) {
            mergedConfig.setObjectTypeConfiguration(customizedConfig.getObjectTypeConfiguration());
        }

        if (customizedConfig.getRelationConfiguration() != null) {
            mergedConfig.setRelationConfiguration(customizedConfig.getRelationConfiguration());
        }

        if (Boolean.FALSE.equals(customizedConfig.isAllowToConfigureSearchItems())) {
            mergedConfig.setAllowToConfigureSearchItems(Boolean.FALSE);
        }

        SearchItemsType mergedSearchItems = combineSearchItems(mergedConfig.getSearchItems(), customizedConfig.getSearchItems(), pageBase);
        mergedConfig.setSearchItems(mergedSearchItems);
        return mergedConfig;
    }

    private static SearchItemsType combineSearchItems(SearchItemsType searchItems, SearchItemsType customSearchItems, ModelServiceLocator pageBase) {
        if (customSearchItems == null || CollectionUtils.isEmpty(customSearchItems.getSearchItem())) {
            return searchItems;
        }

        final List<SearchItemType> customSearchItemsWithDisplayOrder = getSearchItemsWithDisplayOrder(customSearchItems);

        if (searchItems == null || CollectionUtils.isEmpty(searchItems.getSearchItem())) {
            customSearchItems.getSearchItem().clear();
            customSearchItems.getSearchItem().addAll(customSearchItemsWithDisplayOrder);
            return customSearchItems;
        }

        final List<SearchItemType> mergedItems = pageBase.getAdminGuiConfigurationMergeManager().mergeContainers(
                getSearchItemsWithRemovedVisibleByDefault(searchItems),
                customSearchItemsWithDisplayOrder,
                SearchConfigurationMerger::searchItemMatch,
                SearchConfigurationMerger::mergeSearchItem
        );

        searchItems.getSearchItem().clear();
        searchItems.getSearchItem().addAll(mergedItems);
        return searchItems;
    }

    /**
     * Checks all search items in the list whether they have displayOrder set.
     * For each search item without displayOrder we set its displayOrder to its index in the list.
     *
     * @param searchItems for which we need to have displayOrder set
     * @return list of SearchItem where each have displayOrder set
     */
    private static List<SearchItemType> getSearchItemsWithDisplayOrder(final SearchItemsType searchItems) {
        return IntStream
                .range(0, searchItems.getSearchItem().size())
                .mapToObj(i -> {
                    final SearchItemType customSearchItem = searchItems.getSearchItem().get(i);
                    return customSearchItem.getDisplayOrder() == null ?
                            updateDisplayOrder(customSearchItem, i) :
                            customSearchItem;
                }).collect(Collectors.toList());
    }

    /**
     * Sets displayOrder of given SearchItem to given value.
     *
     * @param searchItem for which we want set new displayOrder
     * @param newDisplayOrder new value of displayOrder
     * @return SearchItem with updated displayOrder
     */
    private static SearchItemType updateDisplayOrder(final SearchItemType searchItem, final Integer newDisplayOrder) {
        searchItem.setDisplayOrder(newDisplayOrder);
        return searchItem;
    }

    /**
     * Removes (sets to null) visibleByDefault property for all given SearchItems.
     *
     * @param searchItems for which we want to remove visibleByDefault property
     * @return list of SearchItem where each have visibleByDefault set to null
     */
    private static List<SearchItemType> getSearchItemsWithRemovedVisibleByDefault(final SearchItemsType searchItems) {
        return searchItems.getSearchItem().stream()
                .peek(searchItem -> searchItem.setVisibleByDefault(null))
                .toList();
    }

    private static Predicate<SearchItemType> searchItemMatch(SearchItemType searchItem) {
        return c -> c.getPath() != null && c.getPath().equivalent(searchItem.getPath());
    }

    public static ScopeSearchItemConfigurationType combineScopeSearchItem(ScopeSearchItemConfigurationType scope1, ScopeSearchItemConfigurationType scope2) {
        ScopeSearchItemConfigurationType scopeConfig = combineCustomUserInterfaceFeatureType(scope1, scope2);
        if (scopeConfig != scope2) {
            if (scope2 != null && scope2.getDefaultValue() != null) {
                scopeConfig.setDefaultValue(scope2.getDefaultValue());
            } else {
                scopeConfig.setDefaultValue(scope1.getDefaultValue());
            }
        }
        return scopeConfig;
    }

    private static <F extends UserInterfaceFeatureType> F combineCustomUserInterfaceFeatureType(F feature, F customFeature) {
        if (feature == null) {
            return customFeature;
        }
        if (customFeature == null) {
            return feature;
        }
        if (StringUtils.isNotEmpty(customFeature.getDescription())) {
            feature.description(customFeature.getDescription());
        }
        if (StringUtils.isNotEmpty(customFeature.getDocumentation())) {
            feature.documentation(customFeature.getDocumentation());
        }
        feature.setDisplay(GuiDisplayTypeUtil.combineDisplay(feature.getDisplay(), customFeature.getDisplay()));
        if (customFeature.getVisibility() != null) {
            feature.setVisibility(customFeature.getVisibility());
        }
        if (customFeature.getDisplayOrder() != null) {
            feature.setDisplayOrder(customFeature.getDisplayOrder());
        }
        if (customFeature.getApplicableForOperation() != null) {
            feature.setApplicableForOperation(customFeature.getApplicableForOperation());
        }
        return feature;
    }

    private static SearchItemType mergeSearchItem(SearchItemType item, SearchItemType customItem) {
        if (item == null) {
            return customItem;
        }
        if (customItem == null) {
            return item;
        }
        if (customItem.getPath() != null) {
            item.setPath(customItem.getPath());
        }
        if (customItem.getFilter() != null) {
            item.setFilter(customItem.getFilter());
        }
        if (customItem.getFilterExpression() != null) {
            item.setFilterExpression(customItem.getFilterExpression());
        }
        if (customItem.getDescription() != null) {
            item.setDescription(customItem.getDescription());
        }
        if (customItem.getDisplay() != null) {
            item.setDisplay(customItem.getDisplay());
        }
        if (customItem.getParameter() != null) {
            item.setParameter(customItem.getParameter());
        }
        if (customItem.isVisibleByDefault() != null) {
            item.setVisibleByDefault(customItem.isVisibleByDefault());
        }
        if (customItem.getDisplayOrder() != null) {
            item.setDisplayOrder(customItem.getDisplayOrder());
        }
        return item;
    }
}
