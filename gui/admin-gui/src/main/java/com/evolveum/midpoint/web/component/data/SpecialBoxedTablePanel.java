/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

public class SpecialBoxedTablePanel<T> extends BasePanel<T> implements Table {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_HEADER_FOOTER = "headerFooter";
    private static final String ID_HEADER_PAGING = "pagingFooterHeader";

    private static final String ID_HEADER = "header";
    private static final String ID_FOOTER = "footer";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_CONTAINER = "tableContainer";

    private static final String ID_PAGING_FOOTER = "pagingFooter";

    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGE_SIZE = "pageSize";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_FORM = "form";

    private boolean showAsCard = true;
    private UserProfileStorage.TableId tableId;
    private boolean showPaging;
    private String additionalBoxCssClasses = null;
    private boolean isRefreshEnabled;
    static boolean isRoleMining = false;
    private List<IColumn<T, String>> columns;

    ClusterObjectUtils.SORT sortMode;

    //interval in seconds
    private static final int DEFAULT_REFRESH_INTERVAL = 60;

    public SpecialBoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns) {
        this(id, provider, columns, null);
    }

    public SpecialBoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId) {
        this(id, provider, columns, tableId, false, false, 0, ClusterObjectUtils.SORT.NONE);
    }

    public SpecialBoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, boolean isRoleMining) {
        this(id, provider, columns, tableId, false, isRoleMining, 0, ClusterObjectUtils.SORT.NONE);
    }

    int columnCount;

    public SpecialBoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, boolean isRefreshEnabled, boolean isRoleMining, int columnCount, ClusterObjectUtils.SORT sortMode) {
        super(id);
        this.tableId = tableId;
        this.isRefreshEnabled = isRefreshEnabled;
        SpecialBoxedTablePanel.isRoleMining = isRoleMining;
        this.columnCount = columnCount;
        this.sortMode = sortMode;
        initLayout(columns, provider, columnCount);
    }

    public void goToLastPage() {
        long size = getDataTable().getDataProvider().size();
        int itemsPerPage = getItemsPerPage();

        long page = size / itemsPerPage;
        if (size % itemsPerPage != 0) {
            page++;
        }

        setCurrentPage(page);
    }

    public void setShowAsCard(boolean showAsCard) {
        this.showAsCard = showAsCard;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.initResponsiveTable();"));
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider, int colSize) {
        setOutputMarkupId(true);
        add(AttributeAppender.prepend("class", () -> showAsCard ? "card" : ""));
        add(AttributeAppender.append("class", () -> getAdditionalBoxCssClasses()));

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);

        int pageSize = getItemsPerPage(tableId);
        DataTable<T, String> table = new SelectableDataTable<T>(ID_TABLE, columns, provider, pageSize) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Item<T> newRowItem(String id, int index, IModel<T> rowModel) {
                Item<T> item = super.newRowItem(id, index, rowModel);
                return customizeNewRowItem(item, rowModel);
            }
        };
        table.setOutputMarkupId(true);
        tableContainer.add(table);
        add(tableContainer);

        if (!isRoleMining) {
            TableHeadersToolbar headersTop = new TableHeadersToolbar(table, provider) {

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    super.refreshTable(target);
                    target.add(getFooter());
                }
            };

            headersTop.setOutputMarkupId(true);
            table.addTopToolbar(headersTop);
        } else {
            RoleMiningTableHeadersToolbar headersTop = new RoleMiningTableHeadersToolbar(table, provider) {

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    super.refreshTable(target);
                    target.add(getFooter());
                }
            };

            headersTop.setOutputMarkupId(true);
            table.addTopToolbar(headersTop);

        }
        add(createHeader(ID_HEADER));
        WebMarkupContainer footer = createFooter(ID_FOOTER);
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || provider.size() > pageSize));

        WebMarkupContainer footer2 = createHeaderPaging(ID_HEADER_FOOTER);
        footer2.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || colSize > pageSize));
        add(footer2);
        add(footer);
    }

    private int computeRefreshInterval() {
        int refreshInterval = getAutoRefreshInterval();
        if (refreshInterval != 0) {
            return refreshInterval;
        }
        return DEFAULT_REFRESH_INTERVAL;
    }

    public int getAutoRefreshInterval() {
        return 0;
    }

    public boolean isAutoRefreshEnabled() {
        return false;
    }

    public String getAdditionalBoxCssClasses() {
        return additionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.additionalBoxCssClasses = boxCssClasses;
    }

    // TODO better name?
    protected Item<T> customizeNewRowItem(Item<T> item, IModel<T> model) {
        return item;
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    @Override
    public DataTable getDataTable() {
        return (DataTable) get(ID_TABLE_CONTAINER).get(ID_TABLE);
    }

    public WebMarkupContainer getDataTableContainer() {
        return (WebMarkupContainer) get(ID_TABLE_CONTAINER);
    }

    @Override
    public UserProfileStorage.TableId getTableId() {
        return tableId;
    }

    @Override
    public boolean enableSavePageSize() {
        return true;
    }

    @Override
    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
    }

    @Override
    public int getItemsPerPage() {
        return (int) getDataTable().getItemsPerPage();
    }

    private int getItemsPerPage(UserProfileStorage.TableId tableId) {
        if (tableId == null) {
            return UserProfileStorage.DEFAULT_PAGING_SIZE;
        }
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableId);
    }

    @Override
    public void setShowPaging(boolean show) {
        // todo make use of this [lazyman]
        this.showPaging = show;

        if (!show) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(UserProfileStorage.DEFAULT_PAGING_SIZE);
            if (isRoleMining) {
                setItemsPerPage(100);
            }
        }
    }

    public WebMarkupContainer getHeader() {
        return (WebMarkupContainer) get(ID_HEADER);
    }

    public WebMarkupContainer getFooter() {
        return (WebMarkupContainer) get(ID_FOOTER);
    }

    protected Component createHeader(String headerId) {
        WebMarkupContainer header = new WebMarkupContainer(headerId);
        header.setVisible(false);
        return header;
    }

    protected WebMarkupContainer createFooter(String footerId) {
        return new PagingFooter(footerId, ID_PAGING_FOOTER, this, this) {

            @Override
            protected String getPaginationCssClass() {
                return SpecialBoxedTablePanel.this.getPaginationCssClass();
            }

            @Override
            protected boolean isPagingVisible() {
                return SpecialBoxedTablePanel.this.isPagingVisible();
            }
        };
    }

    protected WebMarkupContainer createHeaderPaging(String footerId) {
        return new PagingFooterColumn(footerId, ID_HEADER_PAGING, this, this) {

            @Override
            protected String getPaginationCssClass() {
                return SpecialBoxedTablePanel.this.getPaginationCssClass();
            }

            @Override
            protected boolean isPagingVisible() {
                return SpecialBoxedTablePanel.this.isPagingVisible();
            }
        };
    }

    protected boolean isPagingVisible() {
        return true;
    }

    protected String getPaginationCssClass() {
        return "pagination-sm";
    }

    public Component getFooterButtonToolbar() {
        return ((PagingFooter) getFooter()).getFooterButtonToolbar();
    }

    public Component getFooterMenu() {
        return ((PagingFooter) getFooter()).getFooterMenu();
    }

    public Component getFooterCountLabel() {
        return ((PagingFooter) getFooter()).getFooterCountLabel();
    }

    public Component getFooterPaging() {
        return ((PagingFooter) getFooter()).getFooterPaging();
    }

    @Override
    public void setCurrentPage(ObjectPaging paging) {
        WebComponentUtil.setCurrentPage(this, paging);
    }

    @Override
    public void setCurrentPage(long page) {
        getDataTable().setCurrentPage(page);
    }

    protected WebMarkupContainer createButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    private static class PagingFooter extends Fragment {

        public PagingFooter(String id, String markupId, SpecialBoxedTablePanel markupProvider, Table table) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider, table);
        }

        private void initLayout(final SpecialBoxedTablePanel boxedTablePanel, final Table table) {
            WebMarkupContainer buttonToolbar = boxedTablePanel.createButtonToolbar(ID_BUTTON_TOOLBAR);
            add(buttonToolbar);

            final DataTable dataTable = table.getDataTable();
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);
            footerContainer.add(new VisibleBehaviour(() -> isPagingVisible()));

            final Label count = new Label(ID_COUNT, () -> CountToolbar.createCountString(dataTable));
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            NavigatorPanel nb2 = new NavigatorPanel(ID_PAGING, dataTable, true) {

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    target.add(count);
                }

                @Override
                protected boolean isCountingDisabled() {
                    if (dataTable.getDataProvider() instanceof SelectableBeanContainerDataProvider) {
                        return !((SelectableBeanContainerDataProvider) dataTable.getDataProvider()).isUseObjectCounting();
                    }
                    return super.isCountingDisabled();
                }

                @Override
                protected String getPaginationCssClass() {
                    return SpecialBoxedTablePanel.PagingFooter.this.getPaginationCssClass();
                }
            };
            footerContainer.add(nb2);

            Form form = new MidpointForm(ID_FORM);
            footerContainer.add(form);
            PagingSizePanel menu = new PagingSizePanel(ID_PAGE_SIZE) {

                @Override
                protected List<Integer> getPagingSizes() {

                    if (isRoleMining) {
                        return List.of(new Integer[] { 50, 100, 150, 200 });
                    }
                    return super.getPagingSizes();
                }

                @Override
                protected void onPageSizeChangePerformed(AjaxRequestTarget target) {
                    Table table = findParent(Table.class);
                    UserProfileStorage.TableId tableId = table.getTableId();

                    if (tableId != null && table.enableSavePageSize()) {
                        Integer pageSize = (int) getPageBase().getItemsPerPage(tableId);

                        table.setItemsPerPage(pageSize);
                    }
                    target.add(findParent(SpecialBoxedTablePanel.PagingFooter.class));
                    target.add((Component) table);
                }
            };
            // todo nasty hack, we should decide whether paging should be normal or "small"
            menu.setSmall(getPaginationCssClass() != null);
            form.add(menu);
            add(footerContainer);
        }

        public Component getFooterButtonToolbar() {
            return get(ID_BUTTON_TOOLBAR);
        }

        public Component getFooterMenu() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGE_SIZE);
        }

        public Component getFooterCountLabel() {
            return get(ID_FOOTER_CONTAINER).get(ID_COUNT);
        }

        public Component getFooterPaging() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGING);
        }

        protected String getPaginationCssClass() {
            return "pagination-sm";
        }

        protected boolean isPagingVisible() {
            return true;
        }
    }

    private class PagingFooterColumn extends Fragment {

        public PagingFooterColumn(String id, String markupId, SpecialBoxedTablePanel markupProvider, Table table) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider, table);
        }

        int pagingSize = getColumnPageCount();

        private void initLayout(final SpecialBoxedTablePanel boxedTablePanel, final Table table) {
            WebMarkupContainer buttonToolbar = boxedTablePanel.createButtonToolbar(ID_BUTTON_TOOLBAR);
            add(buttonToolbar);

            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);
            footerContainer.add(new VisibleBehaviour(() -> isPagingVisible()));

            Form<?> form = new MidpointForm<>(ID_FORM);
            footerContainer.add(form);


            Form<?> formBsProcess = new MidpointForm<>("form_bs_process");
            footerContainer.add(formBsProcess);
            OperationResult operationResult = new OperationResult("ProcessPattern");
            AjaxButton processButton = new AjaxButton("process_selections_id",
                    createStringResource("RoleMining.button.title.process")) {
                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                    BusinessRoleApplicationDto operationData = getOperationData();
                    if (operationData == null) {
                        return;
                    }

                    PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
                    setResponsePage(pageRole);
                }
            };

            processButton.setOutputMarkupId(true);
            processButton.setOutputMarkupPlaceholderTag(true);
            formBsProcess.add(processButton);

            Form<?> formSortMode = new MidpointForm<>("form_sort_model");
            footerContainer.add(formSortMode);

            ChoiceRenderer<ClusterObjectUtils.SORT> renderer = new ChoiceRenderer<>("displayString");

            DropDownChoice<ClusterObjectUtils.SORT> modeSelector = new DropDownChoice<>(
                    "modeSelector", Model.of(sortMode),
                    new ArrayList<>(EnumSet.allOf(ClusterObjectUtils.SORT.class)), renderer);
            modeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    onChangeSortMode(modeSelector.getModelObject(), target);
                }
            });

            modeSelector.setOutputMarkupId(true);
            formSortMode.add(modeSelector);

            Form<?> formCurrentPage = new MidpointForm<>("form_current_page");
            footerContainer.add(formCurrentPage);
            List<Integer> integers = List.of(new Integer[] { 100, 200, 400 });
            DropDownChoice<Integer> colCountOnPage = new DropDownChoice<>("colCountOnPage",
                    new Model<>(getColumnPageCount()), integers);
            colCountOnPage.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    onChangeSize(colCountOnPage.getModelObject(), target);
                }
            });
            colCountOnPage.setOutputMarkupId(true);
            form.add(colCountOnPage);

            int from = 1;
            int to = getColumnPageCount();
            pagingSize = getColumnPageCount();
            String separator = " - ";
            List<String> navigation = new ArrayList<>();

            if (columnCount <= to) {
                to = columnCount;
                navigation.add(from + separator + columnCount);
            } else {
                while (columnCount > to) {
                    navigation.add(from + separator + to);
                    from += pagingSize;
                    to += pagingSize;
                }
                navigation.add(from + separator + columnCount);
            }

            String[] rangeParts = getColumnPagingTitle().split(" - ");
            String title = (Integer.parseInt(rangeParts[0]) + 1) + " to "
                    + Integer.parseInt(rangeParts[1])
                    + " of "
                    + columnCount;

            Label count = new Label(ID_COUNT, Model.of(title));
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            Label label = new Label("label_dropdown", Model.of("Cols per page"));
            label.setOutputMarkupId(true);
            footerContainer.add(label);
            DropDownChoice<String> menu = new DropDownChoice<>(ID_PAGE_SIZE, new Model<>(getColumnPagingTitle()), navigation);
            menu.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    onChange(menu.getModelObject(), target);
                }
            });
            menu.setOutputMarkupId(true);
            formCurrentPage.add(menu);

            add(footerContainer);
        }

        public Component getFooterButtonToolbar() {
            return get(ID_BUTTON_TOOLBAR);
        }

        public Component getFooterMenu() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGE_SIZE);
        }

        public Component getFooterCountLabel() {
            return get(ID_FOOTER_CONTAINER).get(ID_COUNT);
        }

        public Component getFooterPaging() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGING);
        }

        protected String getPaginationCssClass() {
            return "pagination-sm";
        }

        protected boolean isPagingVisible() {
            return true;
        }
    }

    public void onChange(String value, AjaxRequestTarget target) {
    }

    public void onChangeSortMode(ClusterObjectUtils.SORT sortMode, AjaxRequestTarget target) {
    }

    public BusinessRoleApplicationDto getOperationData() {
        return null;
    }

    public int onChangeSize(int value, AjaxRequestTarget target) {
        return value;
    }

    public String getColumnPagingTitle() {
        if (columnCount < getColumnPageCount()) {
            return "0 - " + columnCount;
        }
        return "0 - " + getColumnPageCount();
    }

    public int getColumnPageCount() {
        return 100;
    }
}
