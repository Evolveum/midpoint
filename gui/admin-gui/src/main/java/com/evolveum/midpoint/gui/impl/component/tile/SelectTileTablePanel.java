/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SelectTileTablePanel<T extends Tile, O extends ObjectType> {

    Trace LOGGER = TraceManager.getTrace(SelectTileTablePanel.class);

    default Component createTile(String id, IModel<TemplateTile<SelectableBean<O>>> model) {

        return new SelectableObjectTilePanel<>(id, model) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                boolean oldState = getModelObject().getValue().isSelected();
                ((SelectableBeanDataProvider)getProvider()).clearSelectedObjects();
                getTilesModel().getObject().forEach(tile -> {
                    tile.setSelected(false);
                    if (tile.getValue() instanceof SelectableRow selectableRow) {
                        selectableRow.setSelected(false);
                    }
                });

                getModelObject().setSelected(!oldState);
                getModelObject().getValue().setSelected(!oldState);

                refresh(target);
            }
        };
    }

    ISortableDataProvider getProvider();

    IModel<List<T>> getTilesModel();

    void refresh(AjaxRequestTarget target);

    default Class<O> getType() {
        return (Class<O>) ObjectType.class;
    }

    default @Nullable Set<O> initialSelectedObjects(){
        return null;
    }

    default SelectableBeanObjectDataProvider<O> createProvider() {
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), () -> (Search) getSearchModel().getObject(), initialSelectedObjects()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getCustomQuery();
            }

            @Override
            public void detach() {
                preprocessSelectedDataInternal();
                super.detach();
            }

            @Override
            protected Integer countObjects(Class type, ObjectQuery query, Collection currentOptions, Task task, OperationResult result) throws CommonException {
                if (skipSearch()) {
                    return 0;
                }
                return super.countObjects(type, query, currentOptions, task, result);
            }

            @Override
            protected List searchObjects(Class type, ObjectQuery query, Collection collection, Task task, OperationResult result) throws CommonException {
                if (skipSearch()) {
                    return List.of();
                }
                return super.searchObjects(type, query, collection, task, result);
            }
        };
        provider.setCompiledObjectCollectionView(getCompiledCollectionViewFromPanelConfiguration());
        provider.setOptions(getSearchOptions());
        return provider;
    }

    PageStorage getPageStorage();

    default boolean skipSearch() {
        return false;
    }

    PageBase getPageBase();

    IModel<Search> getSearchModel();

    ObjectQuery getCustomQuery();

    private CompiledObjectCollectionView getCompiledCollectionViewFromPanelConfiguration() {
        ContainerPanelConfigurationType panelConfig = getContainerConfiguration();

        if (panelConfig == null) {
            return null;
        }
        if (panelConfig.getListView() == null) {
            return null;
        }
        CollectionRefSpecificationType collectionRefSpecificationType = panelConfig.getListView().getCollection();

        CompiledObjectCollectionView compiledCollectionViewFromPanelConfiguration = null;
        if (collectionRefSpecificationType == null) {
            compiledCollectionViewFromPanelConfiguration = new CompiledObjectCollectionView();
            getPageBase().getModelInteractionService().applyView(compiledCollectionViewFromPanelConfiguration, panelConfig.getListView());
            return compiledCollectionViewFromPanelConfiguration;
        }
        Task task = getPageBase().createSimpleTask("Compile collection");
        OperationResult result = task.getResult();
        try {
            compiledCollectionViewFromPanelConfiguration = getPageBase().getModelInteractionService().compileObjectCollectionView(
                    collectionRefSpecificationType, getType(), task, result);
        } catch (Throwable e) {
            LOGGER.error("Cannot compile object collection view for panel configuration {}. Reason: {}", panelConfig, e.getMessage(), e);
            result.recordFatalError("Cannot compile object collection view for panel configuration " + panelConfig + ". Reason: " + e.getMessage(), e);
            getPageBase().showResult(result);
        }
        return compiledCollectionViewFromPanelConfiguration;
    }

    default Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return null;
    }

    default ContainerPanelConfigurationType getContainerConfiguration() {
        return null;
    }
}
