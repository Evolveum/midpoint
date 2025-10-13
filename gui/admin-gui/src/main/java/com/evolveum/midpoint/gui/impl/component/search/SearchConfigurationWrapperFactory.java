/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.component.search.factory.*;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

public class SearchConfigurationWrapperFactory {

    private static List<AbstractSearchItemWrapperFactory> factories = new ArrayList<>();

    static {
        factories.add(new AvailableMarkItemWrapperFactory());
        factories.add(new QNameWithoutNamespaceItemWrapperFactory());
        factories.add(new ProcessedObjectTypeItemWrapperFactory());
        factories.add(new DeadShadowSearchItemWrapperFactory());
        factories.add(new ChoicesSearchItemWrapperFactory());
        factories.add(new CertItemOutcomeSearchItemWrapperFactory());
        factories.add(new AutocompleteSearchItemWrapperFactory());
        factories.add(new ReferenceSearchItemWrapperFactory());
        factories.add(new ObjectClassSearchItemWrapperFactory());
        factories.add(new ItemPathSearchItemWrapperFactory());
        factories.add(new DateSearchItemWrapperFactory());
        factories.add(new TextSearchItemWrapperFactory());
    }

    public static PropertySearchItemWrapper createPropertySearchItemWrapper(
            Class<?> type,
            PathKeyedMap<ItemDefinition<?>> availableSearchItems,
            SearchItemType item,
            SearchContext additionalSearchContext,
            CompiledObjectCollectionView collectionView,
            ModelServiceLocator modelServiceLocator) {

        SearchItemContext searchItemContext = new SearchItemContext(type, availableSearchItems, item, additionalSearchContext,
                collectionView, modelServiceLocator);

        AbstractSearchItemWrapperFactory<?, ? extends PropertySearchItemWrapper> searchItemFactory =
                findSearchItemWrapperFactory(searchItemContext);
        if (searchItemFactory == null) {
            return null;
        }
        PropertySearchItemWrapper searchItem = searchItemFactory.create(searchItemContext);
        return searchItem;
    }

    private static AbstractSearchItemWrapperFactory<?, ? extends PropertySearchItemWrapper> findSearchItemWrapperFactory(SearchItemContext searchItemContext) {
        return factories.stream().filter(f -> f.match(searchItemContext)).findFirst().orElse(null);
    }

}
