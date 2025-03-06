/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.Toggle;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectObjectTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.CertCampaignsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

public class CampaignsPanel extends BasePanel<AccessCertificationCampaignType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";
    private static final String ID_NAVIGATION_PANEL = "navigationPanel";

    private LoadableDetachableModel<Search<AccessCertificationCampaignType>> searchModel;
    private LoadableDetachableModel<ViewToggle> viewToggleModel;

    public CampaignsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        viewToggleModel = createViewToggleModel();

        searchModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Search<AccessCertificationCampaignType> load() {
                CertCampaignsStorage storage = getCampaignsStorage();
                if (storage.getSearch() == null) {
                    Search<AccessCertificationCampaignType> search = createSearch();
                    storage.setSearch(search);
                    return search;
                }
                return storage.getSearch();
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer navigationPanel = createNavigationPanel(ID_NAVIGATION_PANEL);
        navigationPanel.setOutputMarkupId(true);
        add(navigationPanel);

        MultiSelectObjectTileTablePanel<AccessCertificationCampaignType,
                        AccessCertificationCampaignType> tilesTable =
                new MultiSelectObjectTileTablePanel<>(ID_CAMPAIGNS_PANEL, viewToggleModel, UserProfileStorage.TableId.PAGE_CAMPAIGNS) {

                    @Serial private static final long serialVersionUID = 1L;
                    @Override
                    protected List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> createColumns() {
                        return CampaignsPanel.this.initColumns(getSelectedItemsModel());
                    }

                    @Override
                    protected Component createHeader(String id) {
                        Component header = super.createHeader(id);
                        if (header instanceof SearchPanel) {
                            // mt-2 added because search panel now uses *-sm classes and it doesn't match rest of the layout
                            header.add(AttributeAppender.append("class", "align-content-center"));
                        }

                        return header;
                    }

                    @Override
                    protected String getTilesContainerAdditionalClass() {
                        return null;
                    }

                    @Override
                    public Component createTile(String id,
                            IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
                        return createCampaignTile(id, model);
                    }

                    @Override
                    protected boolean isSelectedItemsPanelVisible() {
                        return false;
                    }

                    @Override
                    public SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
                        return CampaignsPanel.this.createProvider();
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-12 col-md-6 col-lg-4 col-xl-3 col-xxl-5i p-2 d-flex flex-column";
                    }

                    @Override
                    protected IModel<Search> createSearchModel() {
                        return (IModel) searchModel;
                    }

                    @Override
                    protected IModel<String> getItemLabelModel(AccessCertificationCampaignType entry) {
                        return Model.of(entry.getName().getOrig());
                    }

                    @Override
                    protected void deselectItem(AccessCertificationCampaignType entry) {
                        getProvider().getSelected().remove(entry);
                    }

                    @Override
                    protected LoadableModel<List<AccessCertificationCampaignType>> getSelectedItemsModel() {
                        return new LoadableModel<>() {
                            @Override
                            protected List<AccessCertificationCampaignType> load() {
                                List<SelectableBean<AccessCertificationCampaignType>> selected =
                                        TableUtil.getSelectedModels(getTable().getDataTable());
                                return selected.stream().map(SelectableBean::getValue).toList();
                            }
                        };
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return true;
                    }

                    @Override
                    protected String getTileCssStyle() {
                        return getCampaignTileCssStyle();
                    }

                    @Override
                    protected void onSelectTableRow(IModel<SelectableBean<AccessCertificationCampaignType>> model,
                            AjaxRequestTarget target) {
                        if (model.getObject().isSelected()) {
                            ((SelectableBeanDataProvider) getProvider()).getSelected().add(model.getObject().getValue());
                        }
                    }

                    @Override
                    protected  void togglePanelItemSelectPerformed(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                        if (item == null || item.getObject() == null) {
                            return;
                        }
                        CertCampaignsStorage storage = getCampaignsStorage();
                        if (storage != null) {
                            storage.setViewToggle(item.getObject().getValue());
                        }
                    }
                };
        add(tilesTable);
    }

    private SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
        SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), searchModel, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getCustomCampaignsQuery();
            }

            @Override
            public void detach() {
                preprocessSelectedDataInternal();
                super.detach();
            }

        };
        return provider;
    }

    private LoadableDetachableModel<ViewToggle> createViewToggleModel() {
        return new LoadableDetachableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ViewToggle load() {
                CertCampaignsStorage storage = getCampaignsStorage();
                return storage != null ? storage.getViewToggle() : ViewToggle.TILE;
            }
        };
    }

    private CertCampaignsStorage getCampaignsStorage() {
        return getPageBase().getSessionStorage().getCertCampaigns();
    }

    private List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> initColumns(
            IModel<List<AccessCertificationCampaignType>> campaignsModel) {
        List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> columns =
                ColumnUtils.getDefaultCertCampaignColumns(getPageBase());

        List<CampaignStateHelper.CampaignAction> campaignActions = CampaignStateHelper.getAllCampaignActions();
        List<InlineMenuItem> inlineMenuItems = campaignActions
                .stream()
                        .map(a -> CertMiscUtil.createCampaignMenuItem(campaignsModel, a, getPageBase()))
                                .toList();

        InlineMenuButtonColumn<SelectableBean<AccessCertificationCampaignType>> actionsColumn =
                new InlineMenuButtonColumn<>(inlineMenuItems, getPageBase());
        columns.add(actionsColumn);

        return columns;
    }


    private Search<AccessCertificationCampaignType> createSearch() {
        SearchBuilder<AccessCertificationCampaignType> searchBuilder = new SearchBuilder<>(AccessCertificationCampaignType.class)
                .modelServiceLocator(getPageBase());

        return searchBuilder.build();
    }

    private List<AccessCertificationCampaignType> getSelectedCampaigns() {
        return new ArrayList<>(getCampaignsPanel().getProvider().getSelected());
    }

    protected ObjectQuery getCustomCampaignsQuery() {
        return null;
    }

    protected Component createCampaignTile(String id,
            IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
        return new CampaignTilePanel(id, model) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected LoadableModel<List<ProgressBar>> createCampaignProgressModel() {
                return CertMiscUtil.createCampaignCasesProgressBarModel(getCampaign(), getPrincipal(), getPageBase());
            }
        };
    }

    private MultiSelectObjectTileTablePanel<AccessCertificationCampaignType, AccessCertificationCampaignType> getCampaignsPanel() {
        return (MultiSelectObjectTileTablePanel<AccessCertificationCampaignType, AccessCertificationCampaignType>) get(ID_CAMPAIGNS_PANEL);
    }

    protected WebMarkupContainer createNavigationPanel(String id) {
        return new WebMarkupContainer(id);
    }

    protected String getCampaignTileCssStyle() {
        return " ";
    }
}
