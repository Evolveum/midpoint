/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.component.search.factory.*;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

public class SearchConfigurationWrapperFactory {

    private static List<AbstractSearchItemWrapperFactory> factories = new ArrayList<>();

    static {
        factories.add(new DeadShadowSearchItemWrapperFactory());
        factories.add(new ChoicesSearchItemWrapperFactory());
        factories.add(new AutocompleteSearchItemWrapperFactory());
        factories.add(new ReferenceSearchItemWrapperFactory());
        factories.add(new ObjectClassSearchItemWrapperFactory());
        factories.add(new ObjectClassSearchItemWrapperFactory());
        factories.add(new ItemPathSearchItemWrapperFactory());
        factories.add(new DateSearchItemWrapperFactory());
        factories.add(new TextSearchItemWrapperFactory());
    }

    public static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(PrismContainerDefinition<C> definition,
            SearchItemType item, ResourceObjectDefinition resourceObjectDefinition, ModelServiceLocator modelServiceLocator) {

        SearchItemContext searchItemContext = new SearchItemContext(definition, resourceObjectDefinition, item, modelServiceLocator);


        AbstractSearchItemWrapperFactory<?, ? extends PropertySearchItemWrapper> searchItemFactory =
                findSearchItemWrapperFactory(searchItemContext);
        PropertySearchItemWrapper searchItem = searchItemFactory.create(searchItemContext);
        return searchItem;
    }

    private static AbstractSearchItemWrapperFactory<?, ? extends PropertySearchItemWrapper> findSearchItemWrapperFactory(SearchItemContext searchItemContext) {
        return factories.stream().filter(f -> f.match(searchItemContext)).findFirst().orElse(null);
    }



}
