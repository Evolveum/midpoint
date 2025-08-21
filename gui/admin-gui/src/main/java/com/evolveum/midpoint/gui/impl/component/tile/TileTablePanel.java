/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.NoValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.NoValuePanelDto;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class TileTablePanel<T extends Tile, O extends Serializable> extends BasePanel<O> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_NO_VALUE_PANEL = "noValuePanel";

    static final String ID_TILE_VIEW = "tileView";
    static final String ID_TILES_CONTAINER = "tilesContainer";
    protected static final String ID_TILES_FRAGMENT = "tilesFragment";
    protected static final String ID_TILES = "tiles";

    private static final String ID_HEADER_FRAGMENT = "headerFragment";
    static final String ID_HEADER = "header";
    private static final String ID_VIEW_TOGGLE = "viewToggle";
    private static final String ID_PANEL_HEADER = "panelHeader";

    protected static final String ID_TILE = "tile";
    protected static final String ID_TABLE = "table";

    static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_TILES_PAGING = "tilesPaging";

    private IModel<ViewToggle> viewToggleModel;

    private IModel<Search> searchModel;

    private UserProfileStorage.TableId tableId;

    public TileTablePanel(String id) {
        this(id, null, null);
    }

    public TileTablePanel(String id, IModel<ViewToggle> viewToggle, UserProfileStorage.TableId tableId) {
        super(id);

        if (viewToggle == null) {
            viewToggle = Model.of(ViewToggle.TILE);
        }
        this.viewToggleModel = viewToggle;
        this.tableId = tableId;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        searchModel = createSearchModel();
    }

    public IModel<ViewToggle> getViewToggleModel() {
        return viewToggleModel;
    }

    private void initLayout() {
        setOutputMarkupId(true);

        Component panelForNoValue = createPanelForNoValue();
        add(panelForNoValue);

        WebMarkupContainer tilesView = new WebMarkupContainer(ID_TILE_VIEW);
        tilesView.add(new VisibleBehaviour(() -> isTileViewVisible() && !displayNoValuePanel()));
        tilesView.setOutputMarkupId(true);
        add(tilesView);

        initHeaderFragment(tilesView);

        ISortableDataProvider<O, String> provider = createProvider();
        WebMarkupContainer tilesContainer = createTilesContainer(ID_TILES_CONTAINER, provider, tableId);
        tilesContainer.add(new VisibleBehaviour(this::isTileViewVisible));
        tilesContainer.add(AttributeModifier.append("class", getTilesContainerAdditionalClass()));
        tilesContainer.add(AttributeModifier.append("role", getTilesContainerRole()));
        tilesContainer.setOutputMarkupId(true);
        tilesView.add(tilesContainer);

        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.add(new VisibleBehaviour(this::showFooter));
        footerContainer.setOutputMarkupId(true);
        footerContainer.add(AttributeAppender.append("class", getTilesFooterCssClasses()));
        tilesView.add(footerContainer);

        NavigatorPanel tilesPaging = new NavigatorPanel(ID_TILES_PAGING, getTiles(), true) {

            @Contract(pure = true)
            @Override
            protected @Nullable String getPaginationCssClass() {
                return null;
            }
        };
        footerContainer.add(tilesPaging);

        WebMarkupContainer buttonToolbar = createTilesButtonToolbar(ID_BUTTON_TOOLBAR);
        footerContainer.add(buttonToolbar);

        BoxedTablePanel<?> table = createTablePanel(ID_TABLE, provider, tableId);
        table.add(new VisibleBehaviour(() -> isTableVisible() && !displayNoValuePanel()));
        table.setOutputMarkupId(true);
        initTable(table);
    }

    protected void initTable(@NotNull BoxedTablePanel<?> table) {
        add(table);
    }

    protected String getTilesContainerRole() {
        return "list";
    }

    public void initHeaderFragment(@NotNull WebMarkupContainer tilesView) {
        tilesView.addOrReplace(createHeaderFragment(ID_HEADER));
    }

    protected boolean showFooter() {
        return true;
    }

    protected WebMarkupContainer createTilesContainer(String idTilesContainer, ISortableDataProvider<O, String> provider, UserProfileStorage.TableId tableId) {
        Fragment tilesFragment = new Fragment(idTilesContainer, ID_TILES_FRAGMENT, TileTablePanel.this);
        tilesFragment.add(AttributeAppender.replace("class", getTileContainerCssClass()));

        PageableListView<T, O> tiles = createTilesPanel(ID_TILES, provider);
        tilesFragment.add(tiles);

        return tilesFragment;
    }

    protected PageableListView<T, O> createTilesPanel(
            String tilesId, ISortableDataProvider<O, String> provider) {
        return new PageableListView<T, O>(tilesId, provider, getTableId()) {

            @Override
            protected void populateItem(ListItem<T> item) {
                item.add(AttributeAppender.append("class", () -> getTileCssClasses()));
                item.add(AttributeAppender.append("style", () -> getTileCssStyle()));

                Component tile = createTile(ID_TILE, item.getModel());
                tile.add(AttributeAppender.append("role", getTileRole()));
                item.add(tile);
            }

            @Override
            protected List<T> createItem(O object) {
                return List.of(createTileObject(object));
            }
        };
    }

    protected String getTileRole() {
        return "listitem";
    }

    protected String getTileCssStyle() {
        return "min-height: 250px;";
    }

    protected UserProfileStorage.TableId getTableId() {
        return tableId;
    }

    protected BoxedTablePanel<?> createTablePanel(String idTable, ISortableDataProvider<O, String> provider, UserProfileStorage.TableId tableId) {
        return new BoxedTablePanel<>(idTable, provider, createColumns(), tableId) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                return TileTablePanel.this.createTableButtonToolbar(id);
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
            public String getAdditionalBoxCssClasses() {
                String additionalBoxCssClasses = TileTablePanel.this.getAdditionalBoxCssClasses();
                if (additionalBoxCssClasses != null) {
                    return additionalBoxCssClasses;
                }
                return super.getAdditionalBoxCssClasses();
            }

            @Override
            public String getTableAdditionalCssClasses() {
                return TileTablePanel.this.getAdditionalTableCssClasses();
            }
        };
    }

    protected String getAdditionalBoxCssClasses() {
        return null;
    }

    protected String getAdditionalTableCssClasses() {
        return null;
    }

    private TogglePanel createTogglePanel(String id) {
        IModel<List<Toggle<ViewToggle>>> items = new LoadableModel<>(false) {

            @Override
            protected List<Toggle<ViewToggle>> load() {

                ViewToggle toggle = getViewToggleModel().getObject();
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);
                asList.setActive(ViewToggle.TABLE == toggle);
                asList.setValue(ViewToggle.TABLE);
                asList.setTitle(LocalizationUtil.translate("TileTablePanel.switchToTable"));
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setActive(ViewToggle.TILE == toggle);
                asTile.setValue(ViewToggle.TILE);
                asTile.setTitle(LocalizationUtil.translate("TileTablePanel.switchToTile"));
                list.add(asTile);

                return list;
            }
        };

        TogglePanel<ViewToggle> viewToggle = new TogglePanel<>(id, items) {

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                super.itemSelected(target, item);

                getViewToggleModel().setObject(item.getObject().getValue());
                target.add(TileTablePanel.this);

                togglePanelItemSelectPerformed(target, item);
            }
        };
        viewToggle.add(new VisibleEnableBehaviour(this::isTogglePanelVisible));
        return viewToggle;
    }

    protected boolean isTogglePanelVisible() {
        return false;
    }

    protected List<IColumn<O, String>> createColumns() {
        return List.of();
    }

    protected abstract ISortableDataProvider createProvider();

    protected String getTilesHeaderCssClasses() {
        return "";
    }

    protected String getTilesFooterCssClasses() {
        return "pt-3";
    }

    public IModel<List<T>> getTilesModel() {
        PageableListView view = getTiles();
        return view.getModel();
    }

    public ISortableDataProvider<O, String> getProvider() {
        PageableListView view = getTiles();
        return view.getProvider();
    }

    protected PageableListView getTiles() {
        return (PageableListView) get(ID_TILE_VIEW).get(ID_TILES_CONTAINER).get(ID_TILES);
    }

    protected String getTileCssClasses() {
        return null;
    }

    protected String getTileContainerCssClass() {
        return "d-flex flex-wrap justify-content-left pt-3";
    }

    protected Component createTile(String id, IModel<T> model) {
        return new CatalogTilePanel(id, model);
    }

    protected T createTileObject(O object) {
        return null;
    }

    public void refresh(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());

        if (viewToggleModel.getObject() == ViewToggle.TABLE) {
            target.add(getTable());
        } else {
            target.add(get(ID_TILE_VIEW));
        }
    }

    public BoxedTablePanel<?> getTable() {
        return (BoxedTablePanel<?>) get(ID_TABLE);
    }

    protected NavigatorPanel getTilesNavigation() {
        return (NavigatorPanel) get(createComponentPath(ID_TILE_VIEW, ID_FOOTER_CONTAINER, ID_TILES_PAGING));
    }

    protected IModel<Search> createSearchModel() {
        return null;
    }

    public IModel<Search> getSearchModel() {
        return searchModel;
    }

    Fragment createHeaderFragment(String id) {
        Fragment fragment = new Fragment(id, ID_HEADER_FRAGMENT, TileTablePanel.this);
        fragment.setOutputMarkupId(true);

        Component header = createHeader(ID_PANEL_HEADER);
        header.add(AttributeAppender.append("class", getTilesHeaderCssClasses()));
        fragment.add(header);

        fragment.add(createTogglePanel(ID_VIEW_TOGGLE));
        fragment.add(getHeaderFragmentVisibility());

        return fragment;
    }

    protected VisibleEnableBehaviour getHeaderFragmentVisibility() {
        return VisibleBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected Component createHeader(String id) {
        if (searchModel == null) {
            return new WebMarkupContainer(id);
        }

        return new SearchPanel(id, searchModel) {

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                onSearchPerformed(target);
            }
        };
    }

    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    protected WebMarkupContainer createTilesButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    protected List<Component> createNoValueButtonToolbar(String id) {
        if (isTableVisible()) {
            return Collections.singletonList(createTableButtonToolbar(id));
        }

        return Collections.singletonList(createTilesButtonToolbar(id));
    }

    private void onSearchPerformed(AjaxRequestTarget target) {
        refresh(target);
    }

    protected String getTilesContainerAdditionalClass() {
        return null;
    }

    protected final boolean isTableVisible() {
        return viewToggleModel.getObject() == ViewToggle.TABLE;
    }

    protected final boolean isTileViewVisible() {
        return viewToggleModel.getObject() == ViewToggle.TILE;
    }

    protected void togglePanelItemSelectPerformed(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
    }

    /**
     * Determines whether the panel should display a special UI component
     * (e.g. {@link NoValuePanel}) when there are no values
     * present in the container.
     */
    public boolean displayNoValuePanel() {
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
                tableId.name())) {
            @Override
            protected @NotNull @Unmodifiable List<Component> createToolbarButtons(String buttonsId) {
                return createNoValueButtonToolbar(buttonsId);
            }
        };
        components.setOutputMarkupId(true);
        components.add(new VisibleBehaviour(this::displayNoValuePanel));
        return components;
    }
}
