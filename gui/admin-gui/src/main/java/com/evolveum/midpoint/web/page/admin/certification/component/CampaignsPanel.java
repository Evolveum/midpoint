/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaign;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.CertCampaignsStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;
import java.io.Serial;

import static com.evolveum.midpoint.gui.api.page.PageAdminLTE.createStringResourceStatic;

public class CampaignsPanel extends BasePanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_VIEW_TOGGLE = "viewToggle";
    private static final String ID_CAMPAIGNS_PANEL = "campaignsPanel";
    private LoadableDetachableModel<Search> searchModel;

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
                if (storage == null || storage.getSearch() == null) {
                    return createSearch();
                }
                return storage.getSearch();
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider = createProvider();
        MultiSelectTileTablePanel<AccessCertificationCampaignType,
                AccessCertificationCampaignType> tilesTable =
                new MultiSelectTileTablePanel<>(ID_CAMPAIGNS_PANEL, createViewToggleModel(), UserProfileStorage.TableId.PAGE_CAMPAIGNS) {

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
                    protected Fragment createHeaderFragment(String id) {
                        Fragment headerFragment = super.createHeaderFragment(id);

                        AjaxButton button = new AjaxButton("bulkActionButton", createStringResource("PageBase.button.bulkOperation")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                // TODO implement
                            }
                        };
                        button.setOutputMarkupId(true);
//                        selectedItemsContainer.add(new VisibleBehaviour(() -> isSelectedItemsPanelVisible()));
                        headerFragment.add(button);

                        return headerFragment;
                    }

                    @Override
                    protected Component createTile(String id,
                            IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
                        return new CampaignTilePanel(id, model);
                    }

                    @Override
                    protected boolean isSelectedItemsPanelVisible() {
                        return false;
                    }

                    @Override
                    protected SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
                        return provider;
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-12 col-md-6 col-lg-4 col-xxl-6i px-2";
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

                    }

                    @Override
                    protected IModel<List<AccessCertificationCampaignType>> getSelectedItemsModel() {
                        return () -> new ArrayList<>(getProvider().getSelected());
                    }

                    @Override
                    protected boolean isTogglePanelVisible() {
                        return true;
                    }

                };
        add(tilesTable);

        IModel<List<Toggle<ViewToggle>>> items = new LoadableModel<>(false) {

            @Serial private static final long serialVersionUID = 1L;
            @Override
            protected List<Toggle<ViewToggle>> load() {

                ViewToggle toggle = tilesTable.getViewToggleModel().getObject();
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);
                asList.setActive(ViewToggle.TABLE == toggle);
                asList.setValue(ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setActive(ViewToggle.TILE == toggle);
                asTile.setValue(ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };

//        TogglePanel<ViewToggle> viewToggle = new TogglePanel<>(ID_VIEW_TOGGLE, items) {
//
//            @Override
//            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
//                super.itemSelected(target, item);
//
//                tilesTable.getViewToggleModel().setObject(item.getObject().getValue());
//                tilesTable.getTable().refreshSearch();
//                target.add(CampaignsPanel.this);
//            }
//        };
//        viewToggle.add(new VisibleEnableBehaviour(() -> items.getObject().size() > 1));
//        add(viewToggle);

    }

    private SelectableBeanObjectDataProvider<AccessCertificationCampaignType> createProvider() {
        SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), () -> searchModel.getObject(), null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getCampaignsStorage();
            }

//            @Override
//            protected ObjectQuery getCustomizeContentQuery() {
//                return getCustomQuery();
//            }

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
        List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<AccessCertificationCampaignType>, String> column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new AjaxLinkColumn<>(createStringResource("PageCertCampaigns.table.name"),
                AccessCertificationCampaignType.F_NAME.getLocalPart(), CertCampaignListItemDto.F_NAME) {
            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> rowModel) {
                campaignDetailsPerformed(rowModel.getObject().getValue().getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.description"),
                CertCampaignListItemDto.F_DESCRIPTION);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.iteration"),
                CertCampaignListItemDto.F_ITERATION);
        columns.add(column);

        column = new EnumPropertyColumn<>(createStringResource("PageCertCampaigns.table.state"),
                CertCampaignListItemDto.F_STATE) {
            @Override
            protected String translate(Enum<?> en) {
                return createStringResourceStatic(getPage(), en).getString();
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.stage"),
                CertCampaignListItemDto.F_CURRENT_STAGE_NUMBER);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.escalationLevel"),
                CertCampaignListItemDto.F_ESCALATION_LEVEL_NUMBER);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.stages"),
                CertCampaignListItemDto.F_NUMBER_OF_STAGES);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.deadline"),
                CertCampaignListItemDto.F_DEADLINE_AS_STRING);
        columns.add(column);

        column = new SingleButtonColumn<>(new Model<>(), null) {

            @Override
            public boolean isButtonEnabled(IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                final AccessCertificationCampaignType campaign = model.getObject().getValue();
                String button = determineAction(campaign);
                return button != null;
            }

            @Override
            public boolean isButtonVisible(IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                final AccessCertificationCampaignType campaign = model.getObject().getValue();

                return campaign.getState() != AccessCertificationCampaignStateType.IN_REMEDIATION
                        && campaign.getState() != AccessCertificationCampaignStateType.CLOSED;
            }

            @Override
            public String getCaption() {
                AccessCertificationCampaignType campaign = getRowModel().getObject().getValue();
                String button = determineAction(campaign);
                if (button != null) {
                    return CampaignsPanel.this.createStringResource(button).getString();
                } else {
                    return "";
                }
            }

            @Override
            public String getButtonCssColorClass() {
                return DoubleButtonColumn.ButtonColorClass.PRIMARY.toString();
            }

            @Override
            public String getButtonCssSizeClass() {
                return DoubleButtonColumn.ButtonSizeClass.SMALL.toString();
            }

            @Override
            public void clickPerformed(AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationCampaignType>> model) {
                AccessCertificationCampaignType campaign = model.getObject().getValue();
                String action = determineAction(campaign);
//                switch (action) {
//                    case OP_START_CAMPAIGN:
//                    case OP_OPEN_NEXT_STAGE:
//                        openNextStagePerformed(target, campaign);
//                        break;
//                    case OP_CLOSE_STAGE:
//                        closeStageConfirmation(target, model.getObject());
//                        break;
//                    case OP_START_REMEDIATION:
//                        startRemediationPerformed(target, campaign);
//                        break;
//                    case OP_CLOSE_CAMPAIGN: // not used
//                    default:
//                        throw new IllegalStateException("Unknown action: " + action);
//                }
            }
        };
        columns.add(column);

        List<InlineMenuItem> inlineMenuItems = createInlineMenu();
        inlineMenuItems.addAll(createInlineMenuForItem());

        InlineMenuButtonColumn<SelectableBean<AccessCertificationCampaignType>> actionsColumn =
                new InlineMenuButtonColumn<>(inlineMenuItems, getPageBase());
        columns.add(actionsColumn);

        return columns;
    }

    private void campaignDetailsPerformed(String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        getPageBase().navigateToNext(PageCertCampaign.class, parameters);
    }

    private String determineAction(AccessCertificationCampaignType campaign) {
        int currentStage = campaign.getStageNumber();
        int numOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
        AccessCertificationCampaignStateType state = campaign.getState();
        String button = null;
        switch (state) {
//            case CREATED:
//                button = numOfStages > 0 ? OP_START_CAMPAIGN : null;
//                break;
//            case IN_REVIEW_STAGE:
//                button = OP_CLOSE_STAGE;
//                break;
//            case REVIEW_STAGE_DONE:
//                button = currentStage < numOfStages ? OP_OPEN_NEXT_STAGE : OP_START_REMEDIATION;
//                break;
//            case IN_REMEDIATION:
//            case CLOSED:
//            default:
//                button = null;
//                break;
        }
        return button;
    }

    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.startSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        startSelectedCampaignsPerformed(target);
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.closeSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        closeSelectedCampaignsConfirmation(target);
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterateSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        reiterateSelectedCampaignsConfirmation(target);
                    }
                };
            }

        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.deleteSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CampaignsPanel.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        deleteSelectedCampaignsConfirmation(target);
                    }
                };
            }

        });
        return items;
    }

    private List<InlineMenuItem> createInlineMenuForItem() {

        List<InlineMenuItem> menuItems = new ArrayList<>();
        InlineMenuItem item = new InlineMenuItem(createStringResource("PageCertCampaigns.menu.close")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<CertCampaignListItemDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        closeCampaignConfirmation(target, getRowModel().getObject());
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
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<CertCampaignListItemDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        reiterateCampaignConfirmation(target, getRowModel().getObject());
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
                return new ColumnMenuAction<CertCampaignListItemDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        deleteCampaignConfirmation(target, getRowModel().getObject());
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
}
