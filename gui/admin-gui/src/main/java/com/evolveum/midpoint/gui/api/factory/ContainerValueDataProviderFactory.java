/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.factory;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.prism.PrismContainerDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiListDataProviderType;

public interface ContainerValueDataProviderFactory<T extends Containerable, C extends GuiListDataProviderType> {

    /**
     *
     * @return Base class of supported data types
     */
    Class<T> getDataType();

    /**
     *
     * @return Class of supported configuration element
     */
    Class<C> getConfigurationType();

    /**
     * Checks if provided type class is supported by this data provider.
     *
     * @param type Type to check
     * @return true, if data type is supported by factory
     */
    default boolean isSupported(Class<?> type) {
        return getDataType().isAssignableFrom(type);
    }

    /**
     * Creates data provider instance
     *
     * @param component Parent component
     * @param search Search model
     * @param model List of Prism Container Values
     * @param objectType Parent object Type
     * @param oid Parent oid
     * @param path Path on which prism container values are located
     * @return Data Provider instance
     */
    ISelectableDataProvider<PrismContainerValueWrapper<T>> create(Component component, @NotNull IModel<Search<T>> search, IModel<List<PrismContainerValueWrapper<T>>> model,
                                                                  Class<? extends Objectable>  objectType, String oid, ItemPath path);

    /**
     * Creates data provider instance
     *
     * @param component Parent component
     * @param search Search model
     * @param model List of Prism Container Values
     * @param objectType Parent object Type
     * @param oid Parent oid
     * @param path Path on which prism container values are located
     * @param compiledObjectCollectionView Compiled Object collection view
     * @param customization Implementation of customization
     * @return Data Provider instance
     */

    ISelectableDataProvider<PrismContainerValueWrapper<T>> create(Component component, @NotNull IModel<Search<T>> search, IModel<List<PrismContainerValueWrapper<T>>> model,
                                                                  Class<? extends Objectable>  objectType, String oid, ItemPath path, CompiledObjectCollectionView compiledObjectCollectionView, Customization<T> customization);

    /**
     *
     * @return true if data provider uses repository for search
     */
    boolean isRepositorySearchEnabled();


    /**
     * Returns factory specialized for supplied concrete data type and configuration type.
     *
     * Implementors may create new instance of factory, if provided arguments requires
     * additional changes to creation.
     *
     * Default implementation just casts itself.
     *
     * @param <T2> Final data type
     * @param <C2> Final confiugration Type
     * @param data Data Type
     * @param configuration Configuration Type
     * @return Specialized (casted) instance of data provider factory
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    default <T2 extends Containerable, C2 extends GuiListDataProviderType> ContainerValueDataProviderFactory<T2, C2> specializedFor(Class<T2> data, Class<C2> configuration) {
        return (ContainerValueDataProviderFactory) this;
    }

    /**
     *
     * Allows customization of postFilter, pageStore and contentQuery for data providers
     *
     * @param <T> Containerable type of data
     */
    interface Customization<T extends Containerable> extends Serializable {

        PageStorage getPageStorage();

        List<PrismContainerValueWrapper<T>> postFilter(List<PrismContainerValueWrapper<T>> assignmentList);

        ObjectQuery getCustomizeContentQuery();

        PrismContainerDefinition<AssignmentType> getDefinition();
    }


}
