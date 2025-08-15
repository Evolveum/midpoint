/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.safeLongToInteger;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author Viliam Repan (lazyman)
 */
public class BoxedTablePanel<T> extends BasePanel<T> implements Table {

    private static final long serialVersionUID = 1L;

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
    private List<IColumn<T, String>> columns;

    //interval in seconds
    private static final int DEFAULT_REFRESH_INTERVAL = 60;

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns) {
        this(id, provider, columns, null);
    }

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId) {
        this(id, provider, columns, tableId, false);
    }

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, boolean isRefreshEnabled) {
        super(id);
        this.tableId = tableId;
        this.isRefreshEnabled = isRefreshEnabled;
        initLayout(columns, provider);
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

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider) {
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

            @Override
            protected void onPageChanged() {
                super.onPageChanged();

                BoxedTablePanel.this.onPageChanged();
            }
        };
        table.setOutputMarkupId(true);
        table.add(AttributeAppender.append("class", this::getTableAdditionalCssClasses));
        table.add(new VisibleBehaviour(this::isDataTableVisible));
        tableContainer.add(table);
        add(tableContainer);

        TableHeadersToolbar headersTop = new TableHeadersToolbar(table, provider) {

            @Override
            protected void refreshTable(AjaxRequestTarget target) {
                super.refreshTable(target);

                target.add(getFooter());
            }

            @Override
            protected void onSortChanged() {
                super.onSortChanged();

                BoxedTablePanel.this.onSortChanged();
            }
        };
        headersTop.setOutputMarkupId(true);
        table.addTopToolbar(headersTop);

        Component header = createHeader(ID_HEADER);
        header.setOutputMarkupId(true);
        add(header);
        add(AttributeAppender.append("aria-labelledby", () -> header.isVisible() ? header.getMarkupId() : null));
        WebMarkupContainer footer = createFooter(ID_FOOTER);
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || provider.size() > pageSize));
        add(footer);
    }

    //used only for debug pages, to refresh search properly when type is changed.
    public void refreshSearch() {
        addOrReplace(createHeader(ID_HEADER));
    }

    protected void onPageChanged() {
        ObjectPaging paging = createObjectPaging();
        onPagingChanged(paging);
    }

    protected void onSortChanged() {
        ObjectPaging paging = createObjectPaging();
        onPagingChanged(paging);
    }

    protected void onPagingChanged(ObjectPaging paging) {

    }

    private ObjectPaging createObjectPaging() {
        DataTable<?, ?> dataTable = getDataTable();
        IDataProvider<?> provider = dataTable.getDataProvider();

        long page = dataTable.getCurrentPage();
        long itemsPerPage = dataTable.getItemsPerPage();

        if (provider instanceof BaseSortableDataProvider<?> baseProvider) {
            return baseProvider.createPaging(page * itemsPerPage, itemsPerPage);
        }

        Integer o = safeLongToInteger(page);
        Integer size = safeLongToInteger(itemsPerPage);

        return getPrismContext().queryFactory().createPaging(o * size, size);
    }

    public String getTableAdditionalCssClasses() {
        return null;
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

    protected boolean isDataTableVisible() {
        return true;
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
                return BoxedTablePanel.this.getPaginationCssClass();
            }

            @Override
            protected boolean isPagingVisible() {
                return BoxedTablePanel.this.isPagingVisible();
            }

            @Override
            protected List<Integer> getPagingSizes() {
                return BoxedTablePanel.this.getPagingSizes();
            }

            @Override
            protected boolean shouldAddPredefinedPagingSizes() {
                return BoxedTablePanel.this.shouldAddPredefinedPagingSizes();
            }

            @Override
            protected void setPageSize(Integer newPageSize) {
                BoxedTablePanel.this.savePagingNewValue(newPageSize);
            }
        };
    }

    protected boolean isPagingVisible() {
        return true;
    }

    protected List<Integer> getPagingSizes() {
        return null;
    }

    protected boolean shouldAddPredefinedPagingSizes() {
        return false;
    }

    protected void savePagingNewValue(Integer newValue) {
        if (tableId == null) {
            return;
        }
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(tableId, newValue);
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
    public void setCurrentPageAndSort(ObjectPaging paging) {
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

        public PagingFooter(String id, String markupId, BoxedTablePanel markupProvider, Table table) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider, table);
        }

        private void initLayout(final BoxedTablePanel boxedTablePanel, final Table table) {
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
                    return PagingFooter.this.getPaginationCssClass();
                }
            };
            footerContainer.add(nb2);

            Form form = new MidpointForm(ID_FORM);
            footerContainer.add(form);
            PagingSizePanel menu = new PagingSizePanel(ID_PAGE_SIZE) {

                @Override
                protected void onPageSizeChangePerformed(Integer newPageSize, AjaxRequestTarget target) {
                    Table table = findParent(Table.class);
                    UserProfileStorage.TableId tableId = table.getTableId();

                    if (tableId != null && table.enableSavePageSize()) {
                        table.setItemsPerPage(newPageSize);
                        PagingFooter.this.setPageSize(newPageSize);
                    }
                    target.add(findParent(PagingFooter.class));
                    target.add((Component) table);
                }

                @Override
                protected List<Integer> getCustomPagingSizes() {
                    return PagingFooter.this.getPagingSizes();
                }

                @Override
                protected boolean shouldAddPredefinedPagingSizes() {
                    return PagingFooter.this.shouldAddPredefinedPagingSizes();
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

        protected List<Integer> getPagingSizes() {
            return null;
        }

        protected boolean shouldAddPredefinedPagingSizes() {
            return false;
        }

        protected void setPageSize(Integer newPageSize) {

        }
    }
}
