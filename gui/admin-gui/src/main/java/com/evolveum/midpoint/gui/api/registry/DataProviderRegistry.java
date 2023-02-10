/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.registry;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.gui.api.factory.ContainerValueDataProviderFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiListDataProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;

/**
 *
 * Data Provider registry for customizable data providers
 *
 */
public interface DataProviderRegistry {

    /**
     * Returns provider factory for supplied data type and configuraitno.
     *
     * @param <T> Data Type
     * @param dataType Data Type
     * @param viewConfig View configuration
     * @param defaultProvider Configuration type of default provider
     * @return null, if no provider matches arguments, if viewport configuration does contain concrete provider configuration returns that provider, otherwise returns default provider.
     */
    default <T extends Containerable> ContainerValueDataProviderFactory<T,?> forContainerValue(Class<T> dataType, GuiObjectListViewType viewConfig, Class<? extends GuiListDataProviderType> defaultProvider) {
        if (viewConfig != null) {
            GuiListDataProviderType providerConfig = viewConfig.getDataProvider();
            if (providerConfig != null) {
                var maybe = forContainerValue(dataType, providerConfig.getClass());
                if (maybe != null) {
                    return maybe;
                }
            }

        }
        return forContainerValue(dataType, defaultProvider);
    }

    /**
     *
     * @param <T> Data Type
     * @param <C> Configuration Type
     * @param dataType Data Type
     * @param configurationType Configuration Type
     * @return Container Value Data Provider Factory for specified type combination, or null if no provider factory matches
     */
    <T extends Containerable, C extends GuiListDataProviderType> ContainerValueDataProviderFactory<T,C> forContainerValue(Class<T> dataType, Class<C> configurationType);

}
