/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisTileTableUtils.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisMigratedRolesDto;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.migration.RoleAnalysisMigratedRoleTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.migration.RoleAnalysisMigrationRoleTilePanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisMigrationRoleTileTable extends BasePanel<RoleAnalysisMigratedRolesDto> {
    private static final String ID_DATATABLE = "datatable";
    PageBase pageBase;
    IModel<List<Toggle<ViewToggle>>> items;

    public RoleAnalysisMigrationRoleTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull IModel<RoleAnalysisMigratedRolesDto> model) {
        super(id, model);
        this.pageBase = pageBase;

        this.items = new LoadableModel<>(false) {

            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

                ViewToggle object = getTable().getViewToggleModel().getObject();

                asList.setValue(ViewToggle.TABLE);
                asList.setActive(object == ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setValue(ViewToggle.TILE);
                asTile.setActive(object == ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
        add(initTable());
    }

    public TileTablePanel<RoleAnalysisMigratedRoleTileModel<RoleType>, RoleType> initTable() {

        return new TileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                UserProfileStorage.TableId.PANEL_MIGRATED_ROLES) {

            @Override
            protected String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected List<IColumn<RoleType, String>> createColumns() {
                return RoleAnalysisMigrationRoleTileTable.this.initColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                return initButtonToolbar(id);
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                return initButtonToolbar(id);
            }

            private @NotNull Fragment initButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisMigrationRoleTileTable.this);

                AjaxIconButton refreshTable = buildRefreshButton();
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = buildViewToggleTablePanel(
                        "viewToggle", items, getViewToggleModel(), this);
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected String getTilesFooterCssClasses() {
                return "card-footer";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return "  m-0";
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                return new RoleMiningProvider<>(
                        this, () -> RoleAnalysisMigrationRoleTileTable.this.getModelObject().getRoles(), false);
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisMigratedRoleTileModel createTileObject(RoleType role) {
                return new RoleAnalysisMigratedRoleTileModel<>(role, getPageBase(),
                        () -> RoleAnalysisMigrationRoleTileTable.this.getModelObject().getClusterRef(),
                        () -> RoleAnalysisMigrationRoleTileTable.this.getModelObject().getSessionRef());
            }

            @Override
            protected String getTileCssStyle() {
                return " min-height:170px ";
            }

            @Override
            protected String getTileCssClasses() {
                return "col-4 pb-3 pl-2 pr-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "row justify-content-left ";
            }

            @Override
            protected Component createTile(String id, IModel<RoleAnalysisMigratedRoleTileModel<RoleType>> model) {
                return new RoleAnalysisMigrationRoleTilePanel<>(id, model);
            }
        };
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    private @NotNull List<IColumn<RoleType, String>> initColumns() {

        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        initIconColumn(columns);

        initNameColumn(columns);

        initStatusColumn(columns);

        initMemberCountColumn(columns, getPageBase());

        initInducementCountColumn(columns, getPageBase());

        initCreateTimeStampColumn(columns, getPageBase());

        initExploreColumn(columns);

        return columns;
    }

    private void initExploreColumn(@NotNull List<IColumn<RoleType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("RoleAnalysisTable.column.title.explore")) {

            @Override
            public String getSortProperty() {
                return AbstractRoleType.F_INDUCEMENT.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    RepeatingView repeatingView = new RepeatingView(componentId);
                    item.add(AttributeModifier.append("class", "d-flex align-items-center justify-content-center"));
                    item.add(repeatingView);

                    AjaxCompositedIconSubmitButton exploreButton = buildExploreButton(repeatingView.newChildId());
                    repeatingView.add(exploreButton);
                }
            }

        });
    }

    private void initStatusColumn(@NotNull List<IColumn<RoleType, String>> columns) {
        columns.add(new AbstractColumn<>(createStringResource("Status")) {

            @Override
            public String getSortProperty() {
                return FocusType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleType>> item, String componentId,
                    IModel<RoleType> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getActivation().getEffectiveStatus()));
            }

        });
    }

    private static void initIconColumn(@NotNull List<IColumn<RoleType, String>> columns) {
        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleType> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildExploreButton(String componentId) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("Explore in the cluster")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER,
                        RoleAnalysisMigrationRoleTileTable.this.getModelObject().getClusterRef());
                parameters.add("panelId", "clusterDetails");

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append("class", "btn btn-primary btn-sm"));
        return migrationButton;
    }

    private @NotNull AjaxIconButton buildRefreshButton() {
        AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                Model.of("fa fa-refresh"), Model.of()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                onRefresh(ajaxRequestTarget);
            }
        };

        refreshTable.setOutputMarkupId(true);
        refreshTable.add(AttributeModifier.replace("title",
                createStringResource("RoleAnalysisTable.refresh")));
        refreshTable.add(new TooltipBehavior());
        return refreshTable;
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    protected void onRefresh(@NotNull AjaxRequestTarget target) {
        target.add(getTable());
    }
}
