/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.certification.column.AbstractGuiColumn;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.*;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.visit.IVisitor;
import org.apache.wicket.validation.ValidatorAdapter;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.SearchableItemsDefinitions;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.component.table.WidgetTableChartedHeader;
import com.evolveum.midpoint.gui.impl.component.table.WidgetTableHeader;
import com.evolveum.midpoint.gui.impl.page.admin.report.PageReport;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.query.ObjectPagingImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ContainerableNameColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.wicket.chartjs.ChartConfiguration;

import org.jetbrains.annotations.Nullable;

/**
 * @param <C> the container of displayed objects in table
 * @param <PO> the type of the object processed by provider
 * @author skublik
 *
 * Abstract class for List panels with table.
 */
public abstract class ContainerableListPanel<C extends Serializable, PO extends SelectableRow> extends BasePanel<C> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerableListPanel.class);

    private static final String ID_NO_VALUE_PANEL = "noValuePanel";

    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON = "button";

    private final Class<C> defaultType;

    private LoadableDetachableModel<Search<C>> searchModel;

    private String additionalBoxCssClasses;

    private Boolean manualRefreshEnabled;

    private CompiledObjectCollectionView dashboardWidgetView;
    private CompiledObjectCollectionView compiledCollectionViewFromPanelConfiguration;

    private ContainerPanelConfigurationType config;

    /**
     * Currently this is a switch to turn of the collection view loading from page urls and other places.
     * Eg. for usages in popups that have nothing to do with underlying page
     *
     * TODO this should be "false" - or the other way around, container panel should by default be too "smart" and read
     *  page params and all kinds of stuff from everywhere just because we expect that it's used on page listing "main" objects...
     */
    private boolean useCollectionView = true;

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ContainerableListPanel(String id, Class<C> defaultType) {
        this(id, defaultType, null);
    }

    public ContainerableListPanel(String id, Class<C> defaultType, ContainerPanelConfigurationType configurationType) {
        super(id);
        this.defaultType = defaultType;
//        this.options = options;
        this.config = configurationType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSearchModel();
        initLayout();
    }

    private void initSearchModel() {
        if (searchModel == null) {
            searchModel = createSearchModel();
        }
    }

    private boolean isUseStorageSearch(Search search) {
        if (search == null) {
            return false;
        }
        if (search.isForceReload()) {
            return false;
        }

        String searchByName = getSearchByNameParameterValue();
        return searchByName == null || search.searchByNameEquals(searchByName);
    }

    private LoadableDetachableModel<Search<C>> createSearchModel() {
        return new LoadableDetachableModel<>() {

            @Override
            public Search<C> load() {
                return loadSearchModel();
            }
        };
    }

    private Search<C> loadSearchModel() {
        PageStorage storage = getPageStorage();
        Search<C> search = loadSearch(storage);

        //is this correct place for loading paging?
        ObjectPaging paging = loadPaging(storage);

        if (storage != null && !isPreview()) {
            storage.setSearch(search);
            storage.setPaging(paging);
        }
        getPageBase().getPageParameters().remove(PageBase.PARAMETER_SEARCH_BY_NAME);
        return search;
    }

    private <T extends Serializable> Search<T> loadSearch(PageStorage storage) {
        Search<T> search = null;
        if (storage != null) {
            search = storage.getSearch();
        }

        if (!isUseStorageSearch(search)) {
            search = createSearch();
        }
        return search;
    }

    private ObjectPaging loadPaging(PageStorage storage) {
        ObjectPaging paging = null;
        if (storage != null) {
            paging = storage.getPaging();
        }
        if (paging == null) {
            paging = createPaging();
        }
        return paging;
    }

    private ObjectPaging createPaging() {
        CompiledObjectCollectionView view = getObjectCollectionView();
        ObjectPaging paging = null;
        if (view != null) {
            paging = ObjectQueryUtil.convertToObjectPaging(view.getPaging());
        }

        if (paging == null) {
            var defaultConfig = getDefaultObjectListConfiguration();
            if (defaultConfig != null) {
                paging = ObjectQueryUtil.convertToObjectPaging(defaultConfig.getPaging());
            }
        }

        if (paging == null) {
            paging = ObjectPagingImpl.createEmptyPaging();
        }
        paging.setMaxSize(getDefaultPageSize());
        return paging;
    }

    protected String getSearchByNameParameterValue() {
        return null;
    }

    /**
     * This is to avoid using the object collection in search defined in page as it's loaded automagically.
     * E.g. on popup where we want to do complete different search - without {@link #useCollectionView}
     * qual false, search builder will load collection from underlying page no matter what.
     *
     * @param useCollectionView
     */
    public void setUseCollectionView(boolean useCollectionView) {
        this.useCollectionView = useCollectionView;
    }

    private Search createSearch() {
        CompiledObjectCollectionView objectCollectionView = useCollectionView ? getObjectCollectionView() : null;

        SearchBuilder searchBuilder = new SearchBuilder(getType())
                .collectionView(objectCollectionView)
                .modelServiceLocator(getPageBase())
                .nameSearch(getSearchByNameParameterValue())
                .isPreview(isPreview())
                .isViewForDashboard(isCollectionViewPanelForWidget())
                .additionalSearchContext(createAdditionalSearchContext())
                .setFullTextSearchEnabled(isFulltextEnabled())
                .setTypeChanged(isTypeChanged());

        return searchBuilder.build();
    }

    protected boolean isFulltextEnabled() {
        return true;
    }

    private <T extends Serializable> boolean isTypeChanged() {
        PageStorage storage = getPageStorage();
        if (storage != null && storage.getSearch() != null) {
            Search<T> search = storage.getSearch();
            return search.isTypeChanged();
        }
        return false;
    }

    protected SearchContext createAdditionalSearchContext() {
        return null;
    }

    private void initLayout() {

        Component panelForNoValue = createPanelForNoValue();
        add(panelForNoValue);

        Component table;

        if (isCollapsableTable()) {
            table = initCollapsableItemTable();
        } else {
            table = initItemTable();
        }

        table.setOutputMarkupId(true);
        table.setOutputMarkupPlaceholderTag(true);

        add(table);

        table.add(new VisibleBehaviour(() ->
                !displayNoValuePanel() && isListPanelVisible()));

        setOutputMarkupId(true);
    }

    /**
     * Checks if the table is collapsible.
     *
     * @return {@code true} if the table is collapsible, {@code false} otherwise.
     */
    protected boolean isCollapsableTable() {
        return false;
    }

    protected boolean isListPanelVisible() {
        return true;
    }

    protected Component createHeader(String headerId) {
        return initSearch(headerId);
    }

    private <T extends ChartConfiguration> WidgetTableHeader createWidgetHeader(String headerId) {
        IModel<ChartedHeaderDto<T>> chartModel = getChartedHeaderDtoModel();
        if (chartModel != null) {
            return new WidgetTableChartedHeader<>(headerId, new PropertyModel<>(config, "display"), chartModel);
        } else {
            return new WidgetTableHeader(headerId, new PropertyModel<>(config, "display"));
        }
    }

    protected <T extends ChartConfiguration> IModel<ChartedHeaderDto<T>> getChartedHeaderDtoModel() {
        return null;
    }

    private void setUseCounting(ISelectableDataProvider provider) {
        if (provider instanceof SelectableBeanContainerDataProvider) {
            ((SelectableBeanContainerDataProvider<?>) provider).setForPreview(isPreview());
        }
    }

    protected BoxedTablePanel<PO> initItemTable() {

        List<IColumn<PO, String>> columns = createColumns();
        ISelectableDataProvider<PO> provider = createProvider();
        setDefaultSorting(provider, columns);
        setUseCounting(provider);
        BoxedTablePanel<PO> itemTable = new BoxedTablePanel<>(ID_ITEMS_TABLE,
                provider, columns, getTableId()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Component createHeader(String headerId) {
                if (isPreview()) {
                    return createWidgetHeader(headerId);
                }
                Component header = ContainerableListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;

            }

            @Override
            protected org.apache.wicket.markup.repeater.Item<PO> customizeNewRowItem(org.apache.wicket.markup.repeater.Item<PO> item, IModel<PO> model) {
                item.add(AttributeModifier.append("class", () -> GuiImplUtil.getObjectStatus(model.getObject())));

                customProcessNewRowItem(item, model);
                return item;
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                if (isPreview()) {
                    return new ButtonBar<>(id, ID_BUTTON_BAR, ContainerableListPanel.this,
                            (PreviewContainerPanelConfigurationType) config, getNavigationParametersModel());
                }
                return new ButtonBar<>(id, ID_BUTTON_BAR, ContainerableListPanel.this, createToolbarButtonsList(ID_BUTTON));
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return ContainerableListPanel.this.getAdditionalBoxCssClasses();
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return ContainerableListPanel.this.hideFooterIfSinglePage();
            }

            @Override
            protected boolean isDataTableVisible() {
                return ContainerableListPanel.this.isDataTableVisible();
            }

            @Override
            public int getAutoRefreshInterval() {
                return ContainerableListPanel.this.getAutoRefreshInterval();
            }

            @Override
            public boolean isAutoRefreshEnabled() {
                return ContainerableListPanel.this.isRefreshEnabled();
            }

            @Override
            public boolean enableSavePageSize() {
                return ContainerableListPanel.this.enableSavePageSize();
            }

            @Override
            protected boolean isPagingVisible() {
                return ContainerableListPanel.this.isPagingVisible();
            }

            @Override
            protected List<Integer> getPagingSizes() {
                return ContainerableListPanel.this.getAvailablePageSizes();
            }

            @Override
            protected boolean shouldAddPredefinedPagingSizes() {
                return ContainerableListPanel.this.shouldAddPredefinedPagingSizes();
            }

            @Override
            protected void savePagingNewValue(Integer newPageSize) {
                setPagingSizeNewValue(newPageSize);
            }

            @Override
            protected void onPagingChanged(ObjectPaging paging) {
                ContainerableListPanel.this.onPagingChanged(paging);
            }
        };
        itemTable.setOutputMarkupId(true);

        itemTable.setItemsPerPage(getDefaultPageSize());

        if (getPageStorage() != null) {
            ObjectPaging pageStorage = getPageStorage().getPaging();
            if (pageStorage != null) {
                itemTable.setCurrentPageAndSort(pageStorage);
            }
        }
        itemTable.setShowAsCard(showTableAsCard());

        return itemTable;
    }

    private void onPagingChanged(ObjectPaging paging) {
        PageStorage storage = getPageStorage();
        if (storage == null) {
            return;
        }

        storage.setPaging(paging);
    }

    private void setPagingSizeNewValue(Integer newValue) {
        PageStorage pageStorage = getPageStorage();
        if (pageStorage == null) {
            return;
        }
        ObjectPaging paging = pageStorage.getPaging();
        if (paging == null) {
            return;
        }
        paging.setMaxSize(newValue);
    }

    protected boolean showTableAsCard() {
        return true;
    }

    /**
     * <p>NOTE: This method is experimental and may be removed in the future.</p>
     * Initializes a collapsible table for displaying items.
     *
     * <p>When using this method, ensure that specific IDs are used for the collapsible components,
     * as defined in the RoleAnalysisCollapsableTablePanel class. These IDs are required for proper
     * functionality of collapsible elements.
     *
     * <p>An example of how to utilize this method is provided below:
     * <pre>{@code
     * Component firstCollapseContainer = cellItem.findParent(Item.class).get(ID_FIRST_COLLAPSABLE_CONTAINER);
     * Component secondCollapseContainer = cellItem.findParent(Item.class).get(ID_SECOND_COLLAPSABLE_CONTAINER);
     *
     * // Assuming there's a button in the table header with the ID "headerActionButton"
     * AjaxButton headerActionButton = new AjaxButton("headerActionButton") {
     *     @Override
     *     public void onSubmit(AjaxRequestTarget target) {
     *         // Your action logic here
     *         target.appendJavaScript(getCollapseScript(firstCollapseContainer, secondCollapseContainer));
     *     }
     * };
     * add(headerActionButton);
     * }</pre>
     *
     * <p>You can customize components further by overriding the {@code newRowItem} method, as shown below:
     * <pre>{@code
     * @Override
     * protected Item<SelectableBean<RoleAnalysisClusterType>> newRowItem(String id, int index,
     * IModel<SelectableBean<RoleAnalysisClusterType>> model) {
     *     // Customization logic here
     * }
     * }</pre>
     *
     * @return The initialized RoleAnalysisCollapsableTablePanel instance.
     */
    protected RoleAnalysisCollapsableTablePanel<PO> initCollapsableItemTable() {

        List<IColumn<PO, String>> columns = createColumns();
        ISelectableDataProvider<PO> provider = createProvider();
        setDefaultSorting(provider);
        setUseCounting(provider);
        RoleAnalysisCollapsableTablePanel<PO> itemTable = new RoleAnalysisCollapsableTablePanel<>(ID_ITEMS_TABLE,
                provider, columns, getTableId()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Item<PO> newRowItem(String id, int index, Item<PO> item, @NotNull IModel<PO> rowModel) {

                Item<PO> components = ContainerableListPanel.this.newRowItem(id, index, item, rowModel);
                if (components != null) {
                    return components;
                } else {
                    return super.newRowItem(id, index, item, rowModel);
                }
            }

            @Override
            public boolean isShowAsCard() {
                return ContainerableListPanel.this.showTableAsCard();
            }

            @Override
            protected Component createHeader(String headerId) {
                if (isPreview()) {
                    return createWidgetHeader(headerId);
                }
                Component header = ContainerableListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;

            }

            @Override
            protected Item<PO> customizeNewRowItem(Item<PO> item, IModel<PO> model) {
                item.add(AttributeModifier.append("class", () -> GuiImplUtil.getObjectStatus(model.getObject())));

                customProcessNewRowItem(item, model);
                return item;
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                if (isPreview()) {
                    return new ButtonBar<>(id, ID_BUTTON_BAR, ContainerableListPanel.this,
                            (PreviewContainerPanelConfigurationType) config, getNavigationParametersModel());
                }
                return new ButtonBar<>(id, ID_BUTTON_BAR, ContainerableListPanel.this, createToolbarButtonsList(ID_BUTTON));
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return ContainerableListPanel.this.getAdditionalBoxCssClasses();
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return ContainerableListPanel.this.hideFooterIfSinglePage();
            }

            @Override
            public int getAutoRefreshInterval() {
                return ContainerableListPanel.this.getAutoRefreshInterval();
            }

            @Override
            public boolean isAutoRefreshEnabled() {
                return ContainerableListPanel.this.isRefreshEnabled();
            }

            @Override
            public boolean enableSavePageSize() {
                return ContainerableListPanel.this.enableSavePageSize();
            }

            @Override
            protected boolean isPagingVisible() {
                return ContainerableListPanel.this.isPagingVisible();
            }
        };
        itemTable.setOutputMarkupId(true);

        itemTable.setItemsPerPage(getDefaultPageSize());

        if (getPageStorage() != null) {
            ObjectPaging pageStorage = getPageStorage().getPaging();
            if (pageStorage != null) {
                itemTable.setCurrentPageAndSort(pageStorage);
            }
        }

        return itemTable;
    }

    protected Item<PO> newRowItem(String id, int index, Item<PO> item, @NotNull IModel<PO> rowModel) {
        return null;
    }

    private int getDefaultPageSize() {
        if (isPreview()) {
            Integer previewSize = ((PreviewContainerPanelConfigurationType) config).getPreviewSize();
            return Objects.requireNonNullElse(previewSize, UserProfileStorage.DEFAULT_DASHBOARD_PAGING_SIZE);
        }

        PageStorage storage = getPageStorage();
        if (storage != null) {
            ObjectPaging paging = storage.getPaging();
            if (paging != null && paging.getMaxSize() != null) {
                return paging.getMaxSize();
            }
        }

        Integer configuredDefaultPageSize = getConfiguredDefaultPageSize();
        if (configuredDefaultPageSize != null) {
            return configuredDefaultPageSize;
        }

        Integer collectionViewPagingSize = getPagingMaxSize();
        if (collectionViewPagingSize != null) {
            return collectionViewPagingSize;
        }

        if (getTableId() != null) {
            return getSession().getSessionStorage().getUserProfile().getPagingSize(getTableId());
        }

        return UserProfileStorage.DEFAULT_PAGING_SIZE;
    }

    /**
     * Returns the paging max size for the query
     * @return
     */
    private @Nullable Integer getPagingMaxSize() {
        CompiledObjectCollectionView view = getObjectCollectionView();
        Integer maxSize = view != null && view.getPaging() != null ? view.getPaging().getMaxSize() : null;
        if (maxSize != null) {
            return maxSize;
        }

        var defaultListViewConfigurations = getDefaultObjectListConfiguration();
        if (defaultListViewConfigurations == null) {
            return null;
        }
        return defaultListViewConfigurations.getPaging() != null ? defaultListViewConfigurations.getPaging().getMaxSize() : null;
    }

    /**
     * Returns configured default page size from view configuration or default settings.
     * @return
     */
    private @Nullable Integer getConfiguredDefaultPageSize() {
        CompiledObjectCollectionView view = getObjectCollectionView();
        Integer defaultPageSize = view != null && view.getPaging() != null ? view.getPaging().getMaxSize() : null;
        if (defaultPageSize != null) {
            return defaultPageSize;
        }

        var defaultListViewConfigurations = getDefaultObjectListConfiguration();
        if (defaultListViewConfigurations == null) {
            return null;
        }
        return defaultListViewConfigurations.getPaging() != null ?
                defaultListViewConfigurations.getPaging().getMaxSize() : null;
    }

    protected @Nullable List<Integer> getAvailablePageSizes() {
        CompiledObjectCollectionView view = getObjectCollectionView();
        PagingOptionsType viewPagingOptions = view != null ? view.getPagingOptions() : null;
        PagingType viewPaging = view != null ? view.getPaging() : null;
        var defaultSettings = getDefaultObjectListConfiguration();

        return collectPageSizesFromPagingConfiguration(viewPagingOptions, viewPaging, defaultSettings);
    }

    private boolean shouldAddPredefinedPagingSizes() {
        CompiledObjectCollectionView view = getObjectCollectionView();
        PagingOptionsType viewPagingOptions = view != null ? view.getPagingOptions() : null;
        var defaultSettings = getDefaultObjectListConfiguration();
        return !isAvailableSizeConfigured(viewPagingOptions) &&
                (defaultSettings == null || !isAvailableSizeConfigured(defaultSettings.getPagingOptions()));
    }

    private boolean isAvailableSizeConfigured(PagingOptionsType pagingOptions) {
        return pagingOptions != null && CollectionUtils.isNotEmpty(pagingOptions.getAvailablePageSize());
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> List<Integer> collectPageSizesFromPagingConfiguration(
            PagingOptionsType viewPagingOptions, PagingType viewPaging, DC defaultSettings) {
        List<Integer> availablePageSizes = new ArrayList<>();
        //get available page sizes from default settings and view configuration
        if (availablePageSizesListExist(defaultSettings)) {
            availablePageSizes.addAll(defaultSettings.getPagingOptions().getAvailablePageSize());
        }
        if (viewPagingOptions != null && viewPagingOptions.getAvailablePageSize() != null) {
            viewPagingOptions.getAvailablePageSize().forEach(pageSize -> {
                if (!availablePageSizes.contains(pageSize)) {
                    availablePageSizes.add(pageSize);
                }
            });
        }

        //we get the max size from query paging configuration
        Integer pagingMaxSize = viewPaging != null ? viewPaging.getMaxSize() : null;
        if (pagingMaxSize == null) {
            pagingMaxSize = pagingMaxSizeExists(defaultSettings) ? defaultSettings.getPaging().getMaxSize() : null;
        }
        if (pagingMaxSize != null && !availablePageSizes.contains(pagingMaxSize)) {
            availablePageSizes.add(pagingMaxSize);
        }

        return availablePageSizes;
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> boolean availablePageSizesListExist(DC config) {
        return config != null && config.getPagingOptions() != null
                && config.getPagingOptions().getAvailablePageSize() != null;
    }

    private <DC extends DefaultGuiObjectListPanelConfigurationType> boolean pagingMaxSizeExists(DC config) {
        return config != null && config.getPaging() != null && config.getPaging().getMaxSize() != null;
    }

    protected @Nullable DefaultGuiObjectListPanelConfigurationType getDefaultObjectListConfiguration() {
        if (isPartOfDetailsPage()) {
            return WebComponentUtil.getDefaultObjectDetailsSettings();
        }
        return WebComponentUtil.getDefaultObjectCollectionViewsSettings();
    }

    protected void customProcessNewRowItem(Item<PO> item, IModel<PO> model) {
    }

    protected boolean isPagingVisible() {
        return !isPreview();
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected boolean isHeaderVisible() {
        return true;
    }

    protected boolean isPreview() {
        return config instanceof PreviewContainerPanelConfigurationType;
    }

    protected PageStorage getPageStorage(String storageKey) {
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
        if (storage == null) {
            storage = getSession().getSessionStorage().initPageStorage(storageKey);
        }
        return storage;
    }

    public PageStorage getPageStorage() {
        if (isCollectionViewPanelForWidget()) {
            return null;
        }

        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            return getPageStorage(storageKey);
        }
        return null;
    }

    protected List<CompositedIconButtonDto> createNewButtonDescription() {
        return null;
    }

    protected boolean isNewObjectButtonEnabled() {
        return true;
    }

    public BoxedTablePanel<PO> getTable() {
        //noinspection unchecked
        return (BoxedTablePanel<PO>) getTableComponent();
    }

    private RoleAnalysisCollapsableTablePanel<PO> getCollapsableTable() {
        //noinspection unchecked
        return (RoleAnalysisCollapsableTablePanel<PO>) getTableComponent();
    }

    public Component getTableComponent() {
        return get(ID_ITEMS_TABLE);
    }

    public Class<C> getType() {
        if (getSearchModel().isAttached()) {
            return getSearchModel().getObject().getTypeClass();
        }
        PageStorage storage = getPageStorage();
        if (storage != null && storage.getSearch() != null) {
            return (Class<C>) storage.getSearch().getTypeClass();
        }
        return getDefaultType();
    }

    protected Class<C> getDefaultType() {
        return defaultType;
    }

    protected boolean enableSavePageSize() {
        return true;
    }

    private List<IColumn<PO, String>> createColumns() {
        List<IColumn<PO, String>> columns = collectColumns();

        if (!isPreview()) {
            IColumn<PO, String> actionsColumn = createActionsColumn();
            if (actionsColumn != null) {
                columns.add(actionsColumn);
            }
        }
        return columns;
    }

    protected IColumn<PO, String> createActionsColumn() {
        List<InlineMenuItem> allItems = new ArrayList<>();
        List<InlineMenuItem> menuItems = createInlineMenu();
        if (menuItems != null) {
            allItems.addAll(menuItems);
        }
        addBasicActions(allItems);
        addCustomActions(allItems, this::getSelectedRealObjects);

        if (!allItems.isEmpty()) {
            InlineMenuButtonColumn<PO> actionsColumn = new InlineMenuButtonColumn<>(allItems, getPageBase()) {
                @Override
                public String getCssClass() {
                    return getInlineMenuCssClass();
                }

                @Override
                protected String getInlineMenuItemCssClass() {
                    return ContainerableListPanel.this.getInlineMenuItemCssClass();
                }

                @Override
                protected boolean isButtonMenuItemEnabled(IModel<PO> rowModel) {
                    return isMenuItemVisible(rowModel);
                }

                @Override
                public void populateItem(Item<ICellPopulator<PO>> cellItem, String componentId, IModel<PO> rowModel) {
//                        cellItem.add(Attr))
                    super.populateItem(cellItem, componentId, rowModel);
                }
            };
            return actionsColumn;
        }
        return null;
    }

    /**
     * Method define basic menu action that is default for all subclasses
     * and will be added on end of menu items list.
     */
    protected void addBasicActions(List<InlineMenuItem> menuItems) {
    }

    protected String getInlineMenuCssClass() {
        return "inline-menu-column ";
    }

    protected boolean isMenuItemVisible(IModel<PO> rowModel) {
        return true;
    }

    protected List<IColumn<PO, String>> collectColumns() {
        List<IColumn<PO, String>> columns = new ArrayList<>();

        if (!isCustomColumnsListConfigured()) {
            return initColumns();
        }

        boolean checkForNameColumn = shouldCheckForNameColumn();
        if (shouldIncludeDefaultColumns()) {
            columns = initColumns();
            checkForNameColumn = false;
        }

        columns.addAll(initViewColumns(checkForNameColumn));
        return columns;
    }

    @NotNull
    private List<IColumn<PO, String>> initViewColumns(boolean checkForNameColumn) {
        LOGGER.trace("Start to init custom columns for table of type {}", getType());
        List<IColumn<PO, String>> columns = new ArrayList<>();
        List<GuiObjectColumnType> customColumns = getGuiObjectColumnTypeList();
        if (customColumns == null) {
            return columns;
        }

        if (!shouldIncludeDefaultColumns()) {
            addingCheckAndIconColumnIfExists(columns);
        }

        columns.addAll(getViewColumnsTransformed(customColumns, checkForNameColumn));
        LOGGER.trace("Finished to init custom columns, created columns {}", columns);
        return columns;
    }

    private void addingCheckAndIconColumnIfExists(List<IColumn<PO, String>> columns) {
        if (!isPreview()) {
            IColumn<PO, String> checkboxColumn = createCheckboxColumn();
            if (checkboxColumn != null) {
                columns.add(checkboxColumn);
            }
        }

        IColumn<PO, String> iconColumn = createIconColumn();
        if (iconColumn != null) {
            columns.add(iconColumn);
        }
    }

    protected boolean shouldCheckForNameColumn() {
        return true;
    }

    protected List<IColumn<PO, String>> getViewColumnsTransformed(List<GuiObjectColumnType> customColumns) {
        return getViewColumnsTransformed(customColumns, true);
    }

    protected List<IColumn<PO, String>> getViewColumnsTransformed(List<GuiObjectColumnType> customColumns, boolean shouldCheckForNameColumn) {
        List<IColumn<PO, String>> columns = new ArrayList<>();
        if (customColumns == null || customColumns.isEmpty()) {
            return columns;
        }
        IColumn<PO, String> column;
        for (GuiObjectColumnType customColumn : customColumns) {
            AbstractGuiColumn<?, ?> predefinedColumn = findPredefinedColumn(customColumn);
            if (predefinedColumn != null) {
                if (predefinedColumn.isVisible() && !predefinedColumn.isDefaultColumn()) {
                    columns.add((IColumn<PO, String>) predefinedColumn.createColumn());
                }
                continue;
            }
            if (nothingToTransform(customColumn)) {
                continue;
            }
            ItemPath columnPath = WebComponentUtil.getPath(customColumn);
            // TODO this throws an exception for some kinds of invalid paths like e.g. fullName/norm (but we probably should fix prisms in that case!)
            ExpressionType expression = customColumn.getExport() != null ? customColumn.getExport().getExpression() : null;
            if (expression == null && noItemDefinitionFor(columnPath, customColumn)) {
                continue;
            }

            if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
                IModel<String> columnDisplayModel = createColumnDisplayModel(customColumn);
                if (ObjectType.F_NAME.equivalent(columnPath) || (customColumns.indexOf(customColumn) == 0 && shouldCheckForNameColumn)) {
                    column = createNameColumn(columnDisplayModel, customColumn, expression);
                }
                else if (AbstractRoleType.F_DISPLAY_NAME.equivalent(columnPath)) {
                    column = new ConfigurableExpressionColumn<>(columnDisplayModel, getSortProperty(customColumn, expression), customColumn, expression, getPageBase()) {
                        @Override
                        public void populateItem(Item item, String componentId, IModel rowModel) {
                            super.populateItem(item, componentId, rowModel);
                            item.add(AttributeAppender.append("class", "name-min-width"));
                        }
                    };
                }
                else {
                    column = createCustomExportableColumn(columnDisplayModel, customColumn, expression);
                }

                if (column != null) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    private boolean nothingToTransform(GuiObjectColumnType customColumn) {
        return customColumn.getPath() == null && (customColumn.getExport() == null || customColumn.getExport().getExpression() == null);
    }

    protected AbstractGuiColumn<?, ?> findPredefinedColumn(GuiObjectColumnType customColumn) {
        Class<? extends AbstractGuiColumn<?, ?>> columnClass = getPageBase().findGuiColumn(customColumn.getName());
        return instantiatePredefinedColumn(columnClass, customColumn);
    }

    private AbstractGuiColumn<?, ?> instantiatePredefinedColumn(Class<? extends AbstractGuiColumn> columnClass,
            GuiObjectColumnType columnConfig) {
        if (columnClass == null) {
            return null;
        }
        try {
            return ConstructorUtils.invokeConstructor(columnClass, columnConfig, getColumnTypeConfigContext());
        } catch (Throwable e) {
            LOGGER.trace("No constructor found for column.", e);
        }
        return null;
    }

    protected ColumnTypeConfigContext getColumnTypeConfigContext() {
        return null;
    }

    protected ItemDefinition<?> getContainerDefinitionForColumns() {
        return getPageBase().getPrismContext().getSchemaRegistry()
                .findItemDefinitionByCompileTimeClass(getType(), ItemDefinition.class);//ContainerDefinitionByCompileTimeClass(getType());
    }

    private boolean noItemDefinitionFor(ItemPath columnPath, GuiObjectColumnType customColumn) {
        if (!(getContainerDefinitionForColumns() instanceof PrismContainerDefinition)) {
            LOGGER.warn("Path expression is valid valid only for containerable tables, provided definition is {}", getContainerDefinitionForColumns());
            return true;
        }
        PrismContainerDefinition<? extends Containerable> containerDefinition = (PrismContainerDefinition<? extends Containerable>) getContainerDefinitionForColumns();
        if (columnPath != null) {
            ItemDefinition itemDefinition = containerDefinition.findItemDefinition(columnPath);
            if (itemDefinition == null) { // TODO check  && expression == null) {
                LOGGER.warn("Unknown path '{}' in a definition of column '{}'", columnPath, customColumn.getName());
                return true;
            }
        }
        return false;
    }

    private IModel<String> createColumnDisplayModel(GuiObjectColumnType customColumn) {
        DisplayType displayType = customColumn.getDisplay();
        PolyStringType label = displayType != null ? displayType.getLabel() : null;
        if (label != null) {
            if (label.getTranslation() == null || StringUtils.isEmpty(label.getTranslation().getKey())) {
                return createStringResource(LocalizationUtil.translatePolyString(label));
            }
            return new LoadableDetachableModel<>() {
                @Override
                protected String load() {
                    return LocalizationUtil.translatePolyString(label);
                }
            };
        }

        return createStringResource(getItemDisplayName(customColumn));
    }

    protected IColumn<PO, String> createCustomExportableColumn(
            IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return createCustomExportableColumn(columnDisplayModel, customColumn, () -> null, expression);
    }

    protected IColumn<PO, String> createCustomExportableColumn(
            IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, SerializableSupplier<VariablesMap> variablesSupplier, ExpressionType expression) {
        return new ConfigurableExpressionColumn<>(
                columnDisplayModel, getSortProperty(customColumn, expression), customColumn, variablesSupplier, expression, getPageBase());
    }

    protected String getSortProperty(GuiObjectColumnType customColumn, ExpressionType expressionType) {
        if (customColumn == null) {
            return null;
        }
        String sortProperty = customColumn.getSortProperty();
        if (sortProperty != null) {
            return sortProperty;
        }

        // if there is an expression, it doesn't have a meaning to sort columns
        // because such sort will work according to data in repo and if the expression
        // somehow modify the data, it could be confusing
        if (expressionType != null) {
            return null;
        }

        ItemPath path = WebComponentUtil.getPath(customColumn);
        if (path == null || path.isEmpty()) {
            return null;
        }

        Collection<ItemPath> searchablePaths = getSearchablePaths(getType());

        for (ItemPath searchablePath : searchablePaths) {
            if (searchablePath.size() > 1) {
                //TODO: do we support such orderings in repo?
                continue; //eg. activation/administrative status.. sortParam (BaseSortableDataProvider) should be changes to ItemPath..
            }

            if (searchablePath.equivalent(path)) {
                return path.toString();
            }
        }

        return null;
    }

    private Set<ItemPath> getSearchablePaths(Class<?> type) {
        return new SearchableItemsDefinitions(type, getPageBase())
                .additionalSearchContext(createAdditionalSearchContext())
                .createAvailableSearchItems()
                .keySet();
    }

    @NotNull
    private List<IColumn<PO, String>> initColumns() {
        LOGGER.trace("Start to init columns for table of type {}", getType());
        List<IColumn<PO, String>> columns = new ArrayList<>();

        addingCheckAndIconColumnIfExists(columns);

        List<IColumn<PO, String>> others = createDefaultColumns();
        if (others == null) {
            return columns;
        } else if (notContainsNameColumn(others)) {
            IColumn<PO, String> nameColumn = createNameColumn(createStringResource("ObjectType.name"), null, null);
            if (nameColumn != null) {
                columns.add(nameColumn);
            }
        }

        if (!others.isEmpty()) {
            columns.addAll(others);
        }
        LOGGER.trace("Finished to init columns, created columns {}", columns);
        return columns;
    }

    protected boolean notContainsNameColumn(@NotNull List<IColumn<PO, String>> columns) {
        return columns.stream().noneMatch(c -> c instanceof ContainerableNameColumn);
    }

    protected IColumn<PO, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    protected abstract IColumn<PO, String> createIconColumn();

    protected IColumn<PO, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return null;
    }

    protected List<IColumn<PO, String>> createDefaultColumns() {
        GuiObjectListViewType defaultView = DefaultColumnUtils.getDefaultView(getType());
        if (defaultView == null) {
            return null;
        }
        return getViewColumnsTransformed(defaultView.getColumn());
    }

    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    protected abstract ISelectableDataProvider<PO> createProvider();

    public int getSelectedObjectsCount() {
        List<PO> selectedList = getSelectedObjects();
        return selectedList.size();
    }

    public List<PO> getSelectedObjects() {
        if (isCollapsableTable()) {
            return getSelectedObjectFromCollapsableTable();
        }

        return getSelectedObjectFromDatatable(getTable().getDataTable());
    }

    private @NotNull List<PO> getSelectedObjectFromDatatable(@NotNull DataTable<?, ?> dataTable) {
        List<PO> objects = new ArrayList<>();
        dataTable.visitChildren(SelectableDataTable.SelectableRowItem.class, (IVisitor<SelectableDataTable.SelectableRowItem<PO>, Void>) (row, visit) -> {
            if (row.getModelObject().isSelected()) {
                objects.add(row.getModel().getObject());
            }
        });
        return objects;
    }

    private @NotNull List<PO> getSelectedObjectFromCollapsableTable() {
        return getSelectedObjectFromDatatable(getCollapsableTable().getDataTable());
    }

    public abstract List<C> getSelectedRealObjects();

    protected List<C> getPreselectedObjectList() {
        return null;
    }

    protected SearchPanel initSearch(String headerId) {

        return new SearchPanel<>(headerId, searchModel) {

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                ContainerableListPanel.this.refreshTable(target);
            }

            @Override
            protected void saveSearch(Search search, AjaxRequestTarget target) {
                PageStorage storage = getPageStorage();
                if (storage != null) {
                    storage.setSearch(search);
                }
            }
        };
    }

    public String getAdditionalBoxCssClasses() {
        return additionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.additionalBoxCssClasses = boxCssClasses;
    }

    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
        buttonsList.add(createDownloadButton(idButton));
        return buttonsList;
    }

    protected CsvDownloadButtonPanel createDownloadButton(String buttonId) {

        CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(buttonId) {
            @Override
            protected DataTable<?, ?> getDataTable() {
                Component tableComponent = getTableComponent();
                if (tableComponent instanceof BoxedTablePanel) {
                    return getTable().getDataTable();
                } else if (tableComponent instanceof RoleAnalysisCollapsableTablePanel) {
                    return getCollapsableTable().getDataTable();
                }

                return getTable().getDataTable();
            }

            @Override
            protected String getFilename() {
                return getType().getSimpleName() +
                        "_" + createStringResource("MainObjectListPanel.exportFileName").getString();
            }

        };
        exportDataLink.add(new VisibleBehaviour(this::isExportDataLinkVisible));
        return exportDataLink;
    }

    private boolean isExportDataLinkVisible() {
        return !WebComponentUtil.hasPopupableParent(ContainerableListPanel.this)
                && WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI);
    }

    protected String getStorageKey() {

        CompiledObjectCollectionView compiledView = getCompiledCollectionViewFromPanelConfiguration();
        if (compiledView != null) {
            return WebComponentUtil.getObjectListPageStorageKey(compiledView.getViewIdentifier());
        }

        if (isCollectionViewPanelForCompiledView()) {
            StringValue collectionName = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
            String collectionNameValue = collectionName != null ? collectionName.toString() : "";
            return WebComponentUtil.getObjectListPageStorageKey(collectionNameValue);
        } else if (isCollectionViewPanelForWidget()) {
            String widgetName = getWidgetNameOfCollection();
            return WebComponentUtil.getObjectListPageStorageKey(widgetName);
        } else if (isPreview()) {
            return WebComponentUtil.getObjectListPageStorageKey(config.getIdentifier());
        }

        return WebComponentUtil.getObjectListPageStorageKey(getDefaultType().getSimpleName());
    }

    protected boolean isRefreshEnabled() {
        if (getAutoRefreshInterval() == 0) {
            return manualRefreshEnabled != null && manualRefreshEnabled;
        }

        return Objects.requireNonNullElse(manualRefreshEnabled, true);
    }

    protected int getAutoRefreshInterval() {
        if (isCollectionViewPanel()) {
            CompiledObjectCollectionView view = getObjectCollectionView();
            if (view == null) {
                return 0;
            }

            Integer autoRefreshInterval = view.getRefreshInterval();
            return Objects.requireNonNullElse(autoRefreshInterval, 0);

        }
        return 0;
    }

    public CompiledObjectCollectionView getObjectCollectionView() {
        if (!useCollectionView) {
            return null;
        }

        CompiledObjectCollectionView containerPanelCollectionView = getCompiledCollectionViewFromPanelConfiguration();
        if (containerPanelCollectionView != null) {
            return containerPanelCollectionView;
        }

        CompiledObjectCollectionView view = getWidgetCollectionView();
        if (view != null) {
            return view;
        }
        String collectionName = getCollectionNameFromPageParameters();
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView
                (WebComponentUtil.anyClassToQName(getPageBase().getPrismContext(), getType()), collectionName);
    }

    protected String getCollectionNameFromPageParameters() {
        return WebComponentUtil.getCollectionNameParameterValueAsString(getPageBase());
    }

    private CompiledObjectCollectionView getCompiledCollectionViewFromPanelConfiguration() {
        if (compiledCollectionViewFromPanelConfiguration != null) {
            return compiledCollectionViewFromPanelConfiguration;
        }
        if (config == null) {
            return null;
        }
        GuiObjectListViewType listViewType = config.getListView();
        if (listViewType == null) {
            return null;
        }
        compiledCollectionViewFromPanelConfiguration = WebComponentUtil.getCompiledObjectCollectionView(listViewType, config, getPageBase());
        return compiledCollectionViewFromPanelConfiguration;

    }

    private CompiledObjectCollectionView getWidgetCollectionView() {
        PageParameters parameters = getPageBase().getPageParameters();
        String dashboardOid = parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_TYPE_OID).toString();

        if (StringUtils.isEmpty(dashboardOid) || StringUtils.isEmpty(getWidgetNameOfCollection())) {
            LOGGER.trace("Dashboard not defined, skipping getting collection view for dashboard");
            return null;
        }
        if (dashboardWidgetView != null) {
            return dashboardWidgetView;
        }

        Task task = getPageBase().createSimpleTask("Create view from dashboard");
        PrismObject<DashboardType> dashboard = WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid, getPageBase(), task, task.getResult());
        if (dashboard == null) {
            return null;
        }
        DashboardWidgetType widget = findWidget(dashboard.asObjectable());
        dashboardWidgetView = compileWidgetCollectionView(widget, task);

        return dashboardWidgetView;
    }

    private DashboardWidgetType findWidget(DashboardType dashboardType) {
        return dashboardType.getWidget()
                .stream()
                .filter(d -> Objects.equals(getWidgetNameOfCollection(), d.getIdentifier()))
                .findFirst().orElse(null);
    }

    private CompiledObjectCollectionView compileWidgetCollectionView(DashboardWidgetType widget, Task task) {
        CollectionRefSpecificationType collectionSpec = widget.getData().getCollection();
        try {
            @NotNull CompiledObjectCollectionView compiledView = getPageBase().getModelInteractionService()
                    .compileObjectCollectionView(collectionSpec, null, task, task.getResult());
            if (widget.getPresentation() != null && widget.getPresentation().getView() != null) {
                getPageBase().getModelInteractionService().applyView(compiledView, widget.getPresentation().getView());
            }
            compiledView.setCollection(collectionSpec);
            return compiledView;
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException |
                ExpressionEvaluationException
                | ObjectNotFoundException e) {
            LOGGER.error("Couldn't compile collection " + collectionSpec, e);
            return null;
        }
    }

    // TODO: FIX THIS, how can list component know about widget from page parameter, wrong dependency to "page".
    //  When used on page with such parameter (anywhere, e.g. in popup listing random suff) this panel will still
    //  trick it's search (at least) that collection has to be used in search...
    protected boolean isCollectionViewPanelForWidget() {
        PageParameters parameters = getPageBase().getPageParameters();
        if (useCollectionView && parameters != null) {
            StringValue widget = parameters.get(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME);
            StringValue dashboardOid = parameters.get(PageBase.PARAMETER_DASHBOARD_TYPE_OID);
            return widget != null && widget.toString() != null && dashboardOid != null && dashboardOid.toString() != null;
        }
        return false;
    }

    private String getWidgetNameOfCollection() {
        PageParameters parameters = getPageBase().getPageParameters();
        return parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME).toString();
    }

    protected boolean isCollectionViewPanelForCompiledView() {
        return useCollectionView && WebComponentUtil.getCollectionNameParameterValueAsString(getPageBase()) != null;
    }

    protected boolean isCollectionViewPanel() {
        return useCollectionView && (isCollectionViewPanelForCompiledView() || isCollectionViewPanelForWidget()
                || defaultCollectionExists() || getCompiledCollectionViewFromPanelConfiguration() != null
                || getObjectCollectionView() != null);
    }

    protected boolean defaultCollectionExists() {
        return getCollectionViewForAllObject() != null;
    }

    private CompiledObjectCollectionView getCollectionViewForAllObject() {
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView(WebComponentUtil.anyClassToQName(getPrismContext(), getType()), null);
    }

    protected ISelectableDataProvider getDataProvider() {
        Component tableComponent = getTableComponent();
        if (tableComponent instanceof BoxedTablePanel) {
            return (ISelectableDataProvider) getTable().getDataTable().getDataProvider();
        } else if (tableComponent instanceof RoleAnalysisCollapsableTablePanel) {
            return (ISelectableDataProvider) getCollapsableTable().getDataTable().getDataProvider();
        }
        return null;
    }

    public void refreshTable(AjaxRequestTarget target) {
        if (searchModel.getObject().isForceReload()) {
            resetTable(target);
        } else {
            saveSearchModel(getCurrentTablePaging());
        }

        target.add(getTableComponent());
        target.add(getFeedbackPanel());
    }

    public void resetTableColumns() {
        BoxedTablePanel<PO> table = getTable();
        table.getDataTable().getColumns().clear();
        //noinspection unchecked
        table.getDataTable().getColumns().addAll(createColumns());
    }

    public void resetTable(AjaxRequestTarget target) {
        BoxedTablePanel<PO> table = getTable();
        table.getDataTable().getColumns().clear();
        //noinspection unchecked
        table.getDataTable().getColumns().addAll(createColumns());
        table.addOrReplace(initSearch("header"));
        resetSearchModel();
        table.setCurrentPageAndSort(null);
    }

    public void resetSearchModel() {
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setPaging(null);
        }
    }

    protected void saveSearchModel(ObjectPaging paging) {
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setSearch(searchModel.getObject());
            storage.setPaging(paging);
        }
    }

    public void clearCache() {
        WebComponentUtil.clearProviderCache(getDataProvider());
    }

    @Override
    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(resourceKey, objects);
    }

    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends C>> objectsSupplier) {
    }

    public void addPerformed(AjaxRequestTarget target, List<C> selected) {
        getPageBase().hideMainPopup(target);
    }

    private List<GuiObjectColumnType> getGuiObjectColumnTypeList() {
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        return guiObjectListViewType != null ? guiObjectListViewType.getColumns() : null;
    }

    private boolean isCustomColumnsListConfigured() {
        if (!isCollectionViewPanel()) {
            return false;
        }
        List<GuiObjectColumnType> columnList = getGuiObjectColumnTypeList();
        return columnList != null && !columnList.isEmpty();
    }

    private boolean shouldIncludeDefaultColumns() {
        if (!isCollectionViewPanel()) {
            return false;
        }
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        return BooleanUtils.isTrue(guiObjectListViewType.isIncludeDefaultColumns());
    }

    private String getItemDisplayName(GuiObjectColumnType column) {
        if (!(getContainerDefinitionForColumns() instanceof PrismContainerDefinition)) {
            LOGGER.warn("Cannot determine item name, it is supported only for containerable tables, but the provided definition is {}", getContainerDefinitionForColumns());
            return "";
        }
        PrismContainerDefinition<? extends Containerable> containerDefinition = (PrismContainerDefinition<? extends Containerable>) getContainerDefinitionForColumns();
        ItemPath path = WebComponentUtil.getPath(column);
        if (path == null) {
            LOGGER.warn("Cannot get displayName for column {} because path is not defined", column);
            return "";
        }
        ItemDefinition def = containerDefinition.findItemDefinition(path);
        if (def == null) {
            return "";
        }

        return def.getDisplayName() != null ? def.getDisplayName() : def.getItemName().getLocalPart();
    }

    public ObjectPaging getCurrentTablePaging() {
        PageStorage storage = getPageStorage();
        if (storage == null) {
            return null;
        }
        return storage.getPaging();
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    protected boolean isDataTableVisible() {
        return true;
    }

    public void setManualRefreshEnabled(Boolean manualRefreshEnabled) {
        this.manualRefreshEnabled = manualRefreshEnabled;
    }

    public LoadableDetachableModel<Search<C>> getSearchModel() {
        return searchModel;
    }

    protected void createReportPerformed(AjaxRequestTarget target) {
        PrismContext prismContext = getPageBase().getPrismContext();
        PrismObjectDefinition<ReportType> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(ReportType.COMPLEX_TYPE);
        PrismObject<ReportType> obj;
        try {
            obj = def.instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't instantiate new report", e);
            getPageBase().error(getString("MainObjectListPanel.message.error.instantiateNewReport"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        ReportType report = obj.asObjectable();
        ObjectCollectionReportEngineConfigurationType objectCollection = new ObjectCollectionReportEngineConfigurationType();
        CompiledObjectCollectionView view = getObjectCollectionView();
        CollectionRefSpecificationType collection = new CollectionRefSpecificationType();
        objectCollection.setUseOnlyReportView(true);
        if (view != null) {
            objectCollection.setView(view.toGuiObjectListViewType());
            if (view.getCollection() != null && view.getCollection().getCollectionRef() != null) {
                if (!QNameUtil.match(view.getCollection().getCollectionRef().getType(), ArchetypeType.COMPLEX_TYPE)) {
                    collection.setBaseCollectionRef(view.getCollection());
                } else {
                    OperationResult result = new OperationResult(MainObjectListPanel.class.getSimpleName() + "." + "evaluateExpressionsInFilter");
                    CollectionRefSpecificationType baseCollection = new CollectionRefSpecificationType();
                    try {
                        baseCollection.setFilter(getPageBase().getQueryConverter().createSearchFilterType(
                                WebComponentUtil.evaluateExpressionsInFilter(view.getFilter(), result, getPageBase())));
                        collection.setBaseCollectionRef(baseCollection);
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create filter for archetype");
                        getPageBase().error(getString("MainObjectListPanel.message.error.createArchetypeFilter"));
                        target.add(getPageBase().getFeedbackPanel());
                    }
                }
            }
        } else {
            objectCollection.setView(getDefaultView());
        }
        SearchFilterType searchFilter = null;
        ISelectableDataProvider<?> dataProvider = getDataProvider();
        ObjectQuery query = (dataProvider instanceof BaseSortableDataProvider)
                ? ((BaseSortableDataProvider<?>) dataProvider).getQuery()
                : getSearchModel().getObject().createObjectQuery(getPageBase());
        if (query != null) {
            ObjectFilter filter = query.getFilter();
            try {
                searchFilter = getPageBase().getPrismContext().getQueryConverter().createSearchFilterType(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't create filter from search panel", e);
                getPageBase().error(getString("ExportingFilterTabPanel.message.error.serializeFilterFromSearch"));
            }
        }
        if (searchFilter != null) {
            collection.setFilter(searchFilter);
        } else {
            try {
                SearchFilterType allFilter = prismContext.getQueryConverter().createSearchFilterType(prismContext.queryFactory().createAll());
                collection.setFilter(allFilter);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create all filter", e);
                getPageBase().error(getString("MainObjectListPanel.message.error.createAllFilter"));
                target.add(getPageBase().getFeedbackPanel());
                return;
            }
        }
        objectCollection.setCollection(collection);
        report.setObjectCollection(objectCollection);
        report.getAssignment()
                .add(ObjectTypeUtil.createAssignmentTo(
                        SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value(), ObjectTypes.ARCHETYPE));
        report.getArchetypeRef()
                .add(ObjectTypeUtil.createObjectRef(
                        SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value(), ObjectTypes.ARCHETYPE));

        PageReport pageReport = new PageReport(report.asPrismObject());
        getPageBase().navigateToNext(pageReport);
    }

    protected GuiObjectListViewType getDefaultView() {
        return DefaultColumnUtils.getDefaultView(getType());
    }

    protected void setDefaultSorting(ISelectableDataProvider<PO> provider) {
        setDefaultSorting(provider, null);
    }

    protected void setDefaultSorting(ISelectableDataProvider<PO> provider, List<IColumn<PO, String>> columns) {
        if (provider instanceof SortableDataProvider && isCollectionViewPanel()) {
            boolean ascending = true;
            String orderPathString = null;
            if (getObjectCollectionView().getPaging() != null) {
                PagingType paging = getObjectCollectionView().getPaging();
                ascending = !OrderDirectionType.DESCENDING.equals(paging.getOrderDirection());
                if (getObjectCollectionView().getPaging().getOrderBy() != null) {
                    orderPathString = getPrismContext().itemPathSerializer()
                            .serializeStandalone(paging.getOrderBy().getItemPath());
                }
            }
            if (StringUtils.isEmpty(orderPathString) && columns != null) {
                for (IColumn<PO, String> column : columns) {
                    if (column instanceof AbstractExportableColumn) {
                        AbstractExportableColumn<PO, String> exportableColumn = (AbstractExportableColumn<PO, String>) column;
                        if (exportableColumn.isSortable()) {
                            orderPathString = exportableColumn.getSortProperty();
                            break;
                        }
                    }
                }

            }
            if (orderPathString == null || orderPathString.isEmpty()) {
                return;
            }
            //noinspection unchecked
            ((SortableDataProvider<PO, String>) provider).setSort(new SortParam<>(orderPathString, ascending));
        }
    }

    public ContainerPanelConfigurationType getPanelConfiguration() {
        return config;
    }

    public boolean isValidFormComponents(AjaxRequestTarget target) {
        AtomicReference<Boolean> valid = new AtomicReference<>(true);
        getTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (row, object) -> {
            validateRow((SelectableDataTable.SelectableRowItem) row, valid, target);
        });
        return valid.get();
    }

    public boolean isValidFormComponentsOfRow(IModel<PO> rowModel, AjaxRequestTarget target) {
        AtomicReference<Boolean> valid = new AtomicReference<>(true);
        getTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (row, object) -> {
            if (((SelectableDataTable.SelectableRowItem) row).getModel().equals(rowModel)) {
                validateRow((SelectableDataTable.SelectableRowItem) row, valid, target);
            }
        });
        return valid.get();
    }

    private void validateRow(SelectableDataTable.SelectableRowItem row, AtomicReference<Boolean> valid, AjaxRequestTarget target) {
        row.visitChildren(FormComponent.class, (baseFormComponent, object2) -> {
            if (!baseFormComponent.hasErrorMessage()) {
                baseFormComponent.getBehaviors().stream()
                        .filter(behaviour -> behaviour instanceof ValidatorAdapter
                                && ((ValidatorAdapter) behaviour).getValidator() instanceof NotNullValidator)
                        .map(adapter -> ((ValidatorAdapter) adapter).getValidator())
                        .forEach(validator -> ((NotNullValidator) validator).setUseModel(true));
                ((FormComponent) baseFormComponent).validate();
            }
            if (baseFormComponent.hasErrorMessage()) {
                valid.set(false);
                if (target != null) {
                    target.add(baseFormComponent);
                    InputPanel inputParent = baseFormComponent.findParent(InputPanel.class);
                    if (inputParent != null && inputParent.getParent() != null) {
                        target.addChildren(inputParent.getParent(), FeedbackLabels.class);
                    }
                }
            }
        });
    }

    public boolean isValidFormComponents() {
        return isValidFormComponents(null);
    }

    protected LoadableModel<PageParameters> getNavigationParametersModel() {
        return null;
    }

    protected String getInlineMenuItemCssClass() {
        return "btn btn-default btn-xs";
    }

    private boolean isPartOfDetailsPage() {
        return findParent(AbstractPageObjectDetails.class) != null;
    }

    /**
     * Determines whether the panel should display a special UI component
     * (e.g. {@link NoValuePanel}) when there are no values
     * present in the container.
     */
    protected boolean displayNoValuePanel() {
        return false;
    }

    /**
     * Creates a fallback UI panel to be displayed when the container model has no values.
     * <p>
     * This method constructs a {@link NoValuePanel} that visually indicates the
     * absence of configured resource object types and provides a set of actionable toolbar buttons
     * (e.g., create new or suggest type).
     * </p>
     *
     * @return A {@link Component} instance to be used as the panel when no values are present.
     */
    protected Component createPanelForNoValue() {
        NoValuePanel components = new NoValuePanel(ID_NO_VALUE_PANEL, () -> new NoValuePanelDto(
                defaultType)) {
            @Override
            protected List<Component> createToolbarButtons(String buttonsId) {
                return createToolbarButtonsList(ID_BUTTON);
            }
        };
        components.setOutputMarkupId(true);
        components.add(new VisibleBehaviour(() -> displayNoValuePanel()));
        return components;
    }
}
