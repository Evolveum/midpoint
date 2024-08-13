package com.evolveum.midpoint.gui.impl.component.tile;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.ActionStepPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class SingleSelectTileTablePanel<O extends ObjectType> extends TileTablePanel<TemplateTile<SelectableBean<O>>, SelectableBean<O>> {

    private static final Trace LOGGER = TraceManager.getTrace(ActionStepPanel.class);

    public SingleSelectTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId) {
        this(id, Model.of(ViewToggle.TILE), tableId);
    }

    public SingleSelectTileTablePanel(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId) {
        super(id, viewToggle, tableId);
    }

    @Override
    protected SelectableBeanObjectDataProvider<O> createProvider() {
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), () -> (Search) getSearchModel().getObject(), null) {
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

    protected boolean skipSearch() {
        return false;
    }

    protected PageStorage getPageStorage() {
        return null;
    }

    protected ObjectQuery getCustomQuery() {
        return null;
    }

    @Override
    protected TemplateTile<SelectableBean<O>> createTileObject(SelectableBean<O> object) {
        TemplateTile<SelectableBean<O>> t = TemplateTile.createTileFromObject(object, getPageBase());
        return t;
    }

    @Override
    protected Component createTile(String id, IModel<TemplateTile<SelectableBean<O>>> model) {

        return new SelectableFocusTilePanel<>(id, model) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                boolean oldState = getModelObject().getValue().isSelected();
                ((SelectableBeanDataProvider) getProvider()).clearSelectedObjects();
                getTilesModel().getObject().forEach(tile -> {
                    tile.setSelected(false);
                    tile.getValue().setSelected(false);
                });

                getModelObject().setSelected(!oldState);
                getModelObject().getValue().setSelected(!oldState);

                refresh(target);
            }
        };
    }

    @Override
    protected String getTileCssClasses() {
        return "col-xs-6 col-sm-6 col-md-4 col-lg-3 col-xl-5i col-xxl-5i p-2";
    }

    @Override
    protected IModel<Search> createSearchModel() {
        return new LoadableModel<>(false) {
            @Override
            protected Search load() {
                return new SearchBuilder(getType())
                        .collectionView(getCompiledCollectionViewFromPanelConfiguration())
                        .modelServiceLocator(getPageBase())
                        .additionalSearchContext(getAdditionalSearchContext())
                        .setFullTextSearchEnabled(isFullTextSearchEnabled())
                        .build();
            }
        };
    }

    protected boolean isFullTextSearchEnabled() {
        return true;
    }

    protected SearchContext getAdditionalSearchContext() {
        return new SearchContext();
    }

    protected Class<O> getType() {
        return (Class<O>) ObjectType.class;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return null;
    }

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

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return null;
    }

    @Override
    protected BoxedTablePanel createTablePanel(String idTable, ISortableDataProvider<SelectableBean<O>, String> provider, UserProfileStorage.TableId tableId) {
        BoxedTablePanel<SelectableBean<O>> table = new BoxedTablePanel<>(idTable, provider, createColumns(), tableId) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                return SingleSelectTileTablePanel.this.createTableButtonToolbar(id);
            }

            @Override
            protected Component createHeader(String headerId) {
                return createHeaderFragment(headerId);
            }

            @Override
            protected String getPaginationCssClass() {
                return null;
            }

            @Override
            protected Item customizeNewRowItem(Item<SelectableBean<O>> item, IModel<SelectableBean<O>> model) {
                SingleSelectTileTablePanel.this.customizeNewRowItem(model.getObject());

                item.add(AttributeModifier.append("class", () ->
                        model.getObject().isSelected() ? "cursor-pointer table-primary" : "cursor-pointer"));
                item.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        onSelectTableRow(model, target);
                    }
                });
                return item;
            }
        };
        table.setShowAsCard(false);
        return table;
    }

    protected void customizeNewRowItem(SelectableBean<O> value) {
    }

    void onSelectTableRow(IModel<SelectableBean<O>> model, AjaxRequestTarget target) {
        boolean oldState = model.getObject().isSelected();
        ((SelectableBeanDataProvider) getProvider()).clearSelectedObjects();

        model.getObject().setSelected(!oldState);
        if (model.getObject().isSelected()) {
            ((SelectableBeanDataProvider) getProvider()).getSelected().add(model.getObject().getValue());
        }

        refresh(target);
    }
}
