/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.SearchConfigurationWrapper;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;

public class SearchConfigurationMerger {

    public static ScopeSearchItemConfigurationType combineScopeSearchItem(ScopeSearchItemConfigurationType scope1, ScopeSearchItemConfigurationType scope2) {
        ScopeSearchItemConfigurationType scopeConfig = combineCustomUserInterfaceFeatureType(scope1, scope2);
        if (scopeConfig != scope2) {
            if (scope2 != null && scope2.getDefaultValue() != null) {
                scopeConfig.setDefaultValue(scope2.getDefaultValue());
            } else if (scope1 != null) {
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
        feature.setDisplay(WebComponentUtil.combineDisplay(feature.getDisplay(), customFeature.getDisplay()));
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

    private static SearchItemsType combineSearchItems(SearchItemsType searchItems, SearchItemsType customSearchItems) {
        if (searchItems == null || CollectionUtils.isEmpty(searchItems.getSearchItem())) {
            return customSearchItems;
        }
        if (customSearchItems == null || CollectionUtils.isEmpty(customSearchItems.getSearchItem())) {
            return searchItems;
        }
        customSearchItems.getSearchItem().forEach(customItem -> {
            SearchItemType item = findSearchItemByPath(searchItems.getSearchItem(), customItem.getPath());
            if (item != null) {
                combineSearchItem(item, customItem);
            } else {
                searchItems.getSearchItem().add(customItem.clone());
            }
        });
        return searchItems;
    }

    private static SearchItemType findSearchItemByPath(List<SearchItemType> itemList, ItemPathType path) {
        if (path == null) {
            return null;
        }
        for (SearchItemType item : itemList) {
            if (path.equivalent(item.getPath())) {
                return item;
            }
        }
        return null;
    }

    private static SearchItemType combineSearchItem(SearchItemType item, SearchItemType customItem) {
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
        if (customItem.getDisplayName() != null) {
            item.setDisplayName(customItem.getDisplayName());
        }
        if (customItem.getParameter() != null) {
            item.setParameter(customItem.getParameter());
        }
        if (customItem.isVisibleByDefault() != null) {
            item.setVisibleByDefault(customItem.isVisibleByDefault());
        }
        return item;
    }

    private static SearchConfigurationWrapper combineSearchBoxConfiguration(SearchConfigurationWrapper config, SearchConfigurationWrapper customConfig) {
        return combineSearchBoxConfiguration(config, customConfig, false);
    }


    public static SearchBoxConfigurationType mergeConfigurations(SearchBoxConfigurationType defaultConfig, SearchBoxConfigurationType customizedConfig) {
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
            mergedConfig.setScopeConfiguration(customizedConfig.getScopeConfiguration());
        }

        if (customizedConfig.getDefaultMode() != null) {
            mergedConfig.setDefaultMode(customizedConfig.getDefaultMode());
        }

        if (customizedConfig.getDefaultObjectType() != null) {
            mergedConfig.setDefaultObjectType(customizedConfig.getDefaultObjectType());
        }

        if (customizedConfig.getObjectTypeConfiguration() != null) {
            mergedConfig.setObjectTypeConfiguration(customizedConfig.getObjectTypeConfiguration());
        }

        if (customizedConfig.getRelationConfiguration() != null) {
            mergedConfig.setRelationConfiguration(customizedConfig.getRelationConfiguration());
        }

        if (customizedConfig.getDefaultScope() != null) {
            mergedConfig.setDefaultScope(customizedConfig.getDefaultScope());
        }

        //TODO more intelligent merge for search items
//        if (customizedConfig.getSearchItems() != null) {
//            for (SearchItemType customizedItem : customizedConfig.getSearchItems().getSearchItem()) {
                combineSearchItems(mergedConfig.getSearchItems(), customizedConfig.getSearchItems());
//            }
//            mergedConfig.setSearchItems(customizedConfig.getSearchItems());
//        }
        return mergedConfig;
    }

    public static SearchConfigurationWrapper combineSearchBoxConfiguration(SearchConfigurationWrapper config,
            SearchConfigurationWrapper customConfig, boolean replaceSearchItems) {
        if (config == null) {
            return customConfig;
        }
        if (customConfig == null) {
            return config;
        }

        if (CollectionUtils.isNotEmpty(customConfig.getItemsList())) {
            if (replaceSearchItems) {
                config.getItemsList().clear();
                config.getItemsList().addAll(customConfig.getItemsList());
            } else {
                customConfig.getItemsList().forEach(item -> {
                    addOrReplaceSearchItemWrapper(config, (AbstractSearchItemWrapper) item);
                });
            }
        }
        config.setAllowToConfigureSearchItems(customConfig.isAllowToConfigureSearchItems());
        return config;
    }

    private static void addOrReplaceSearchItemWrapper(SearchConfigurationWrapper config, AbstractSearchItemWrapper customItem) {
        List<AbstractSearchItemWrapper> items = config.getItemsList();
        boolean execute = false;
        if (customItem instanceof PropertySearchItemWrapper) {
            Iterator<AbstractSearchItemWrapper> itemsIterator = items.iterator();
            while (itemsIterator.hasNext()) {
                AbstractSearchItemWrapper item = itemsIterator.next();
                if (!hasParameter(item) && item instanceof PropertySearchItemWrapper &&
                        ((PropertySearchItemWrapper<?>) item).getPath().equivalent(((PropertySearchItemWrapper<?>) customItem).getPath())) {
                    execute = true;
                } else if (item instanceof AbstractRoleSearchItemWrapper && customItem.getClass().equals(item.getClass())) {
                    execute = true;
                }
                if (execute) {
                    itemsIterator.remove();
                    items.add(customItem);
                    break;
                }
            }
            if (!execute) {
                items.add(customItem);
            }
            return;
        }

        for (AbstractSearchItemWrapper item : items) {
            if (item.getClass().equals(customItem.getClass())) {
                items.remove(item);
                items.add(customItem);
                execute = true;
                break;
            }
        }
        if (!execute) {
            items.add(customItem);
        }
    }

    private static boolean hasParameter(AbstractSearchItemWrapper searchItem) {
        return searchItem != null && StringUtils.isNotEmpty(searchItem.getParameterName());
    }
}
