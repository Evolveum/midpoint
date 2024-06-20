/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.CertCampaignsStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.Serial;

import static com.evolveum.midpoint.gui.api.page.PageAdminLTE.createStringResourceStatic;

public class CampaignsPanel extends BasePanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";
    private static final String ID_NAVIGATION_PANEL = "navigationPanel";

    private LoadableDetachableModel<Search> searchModel;
    private SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider;

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
        searchModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                CertCampaignsStorage storage = getCampaignsStorage();
                if (storage.getSearch() == null) {
                    Search search = createSearch();
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

        provider = createProvider();
        MultiSelectObjectTileTablePanel<AccessCertificationCampaignType,
                        AccessCertificationCampaignType> tilesTable =
                new MultiSelectObjectTileTablePanel<>(ID_CAMPAIGNS_PANEL, createViewToggleModel(), UserProfileStorage.TableId.PAGE_CAMPAIGNS) {

                    @Serial private static final long serialVersionUID = 1L;
                    @Override
                    protected List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> createColumns() {
                        return CampaignsPanel.this.initColumns();
                    }

                    @Override
                    protected Component createHeader(String id) {
                        Component header = super.createHeader(id);
                        if (header instanceof SearchPanel) {
                            // mt-2 added because search panel now uses *-sm classes and it doesn't match rest of the layout
                            header.add(AttributeAppender.append("class", "mt-2"));
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
                        return provider;
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-12 col-md-4 col-lg-3 col-xxl-4i px-2";
                    }

                    @Override
                    protected IModel<Search> createSearchModel() {
                        return searchModel;
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
                    protected IModel<List<AccessCertificationCampaignType>> getSelectedItemsModel() {
                        return () -> new ArrayList<>(getProvider().getSelected());
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return true;
                    }

                    @Override
                    protected String getTileCssStyle() {
                        return "min-height: 340px;";
                    }

                    @Override
                    protected void onSelectTableRow(IModel<SelectableBean<AccessCertificationCampaignType>> model,
                            AjaxRequestTarget target) {
                        if (model.getObject().isSelected()) {
                            ((SelectableBeanDataProvider) getProvider()).getSelected().add(model.getObject().getValue());
                        }
                    }
                };
        add(tilesTable);
    }

    private SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
        SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), () -> searchModel.getObject(), null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getCampaignsStorage();
            }

            @Override
            public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
                return  SchemaService.get().getOperationOptionsBuilder()
                                .item(AccessCertificationCampaignType.F_CASE).retrieve(RetrieveOption.INCLUDE)
                                .build();
            }
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

    private IModel<ViewToggle> createViewToggleModel() {
        return new LoadableModel<>(false) {

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

    private List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> initColumns() {
        List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> columns =
                ColumnUtils.getDefaultCertCampaignColumns(getPageBase());

        List<InlineMenuItem> inlineMenuItems = createInlineMenu();
        inlineMenuItems.addAll(createInlineMenuForItem());

        InlineMenuButtonColumn<SelectableBean<AccessCertificationCampaignType>> actionsColumn =
                new InlineMenuButtonColumn<>(inlineMenuItems, getPageBase());
        columns.add(actionsColumn);

        return columns;
    }

    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.startSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.startSelectedCampaignsPerformed(target, getSelectedCampaigns(), getPageBase());
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.closeSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.closeSelectedCampaignsConfirmation(target, getSelectedCampaigns(), getPageBase());
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterateSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.reiterateSelectedCampaignsConfirmation(target, getSelectedCampaigns(),
                                getPageBase());
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.deleteSelected")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CampaignProcessingHelper.deleteSelectedCampaignsConfirmation(target, getSelectedCampaigns(), getPageBase());
                    }
                };
            }

        });
        return items;
    }

    private List<InlineMenuItem> createInlineMenuForItem() {

        List<InlineMenuItem> menuItems = new ArrayList<>();
        InlineMenuItem item = new InlineMenuItem(createStringResource("PageCertCampaigns.menu.close")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<AccessCertificationCampaignType>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }
                        CampaignProcessingHelper.closeCampaignConfirmation(target, getRowModel().getObject().getValue(), getPageBase());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        };
//        item.setVisibilityChecker((rowModel, header) -> isNotClosed(rowModel));
        menuItems.add(item);

        item = new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<AccessCertificationCampaignType>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }
                        CampaignProcessingHelper.reiterateCampaignConfirmation(target, getRowModel().getObject().getValue(), getPageBase());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
//        item.setVisibilityChecker((rowModel, header) -> isReiterable(rowModel));
        menuItems.add(item);

        menuItems.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<AccessCertificationCampaignType>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }
                        CampaignProcessingHelper.deleteCampaignConfirmation(target, getRowModel().getObject().getValue(), getPageBase());
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        });
        return menuItems;
    }

    private Search createSearch() {
        SearchBuilder searchBuilder = new SearchBuilder(AccessCertificationCampaignType.class)
                .modelServiceLocator(getPageBase());

        return searchBuilder.build();
    }

    private List<AccessCertificationCampaignType> getSelectedCampaigns() {
        return new ArrayList<>(provider.getSelected());
    }

    protected ObjectQuery getCustomCampaignsQuery() {
        return null;
    }

    protected Component createCampaignTile(String id,
            IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
        return new CampaignTilePanel(id, model);
    }

    protected WebMarkupContainer createNavigationPanel(String id) {
        return new WebMarkupContainer(id);
    }
}
