/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.util.List;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import org.apache.wicket.util.time.Duration;

/**
 * @author Viliam Repan (lazyman)
 */
public class BoxedTablePanel<T> extends BasePanel<T> implements Table {

    private static final long serialVersionUID = 1L;

    private static final String ID_BOX = "box";
    private static final String ID_HEADER = "header";
    private static final String ID_FOOTER = "footer";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_CONTAINER = "tableContainer";

    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_MENU = "menu";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";

    private UserProfileStorage.TableId tableId;
    private boolean showPaging;
    private String additionalBoxCssClasses = null;
    private boolean isRefreshEnabled;

    //interval in seconds
    private static final int DEFAULT_REFRESH_INTERVAL = 60;

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns) {
        this(id, provider, columns, null, UserProfileStorage.DEFAULT_PAGING_SIZE);
    }

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId) {
        this(id, provider, columns, tableId, UserProfileStorage.DEFAULT_PAGING_SIZE);
    }

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
                           UserProfileStorage.TableId tableId, int pageSize) {
        this(id, provider, columns, tableId, pageSize, false);
    }

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, int pageSize, boolean isRefreshEnabled) {
        super(id);
        this.tableId = tableId;
        this.isRefreshEnabled = isRefreshEnabled;

        initLayout(columns, provider, pageSize);
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider, int pageSize) {
        setOutputMarkupId(true);
        WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
        box.add(new AttributeAppender("class", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                String boxCssClasses = getAdditionalBoxCssClasses();
                if (boxCssClasses == null) {
                    return "";
                } else {
                    return " " + boxCssClasses;
                }
            }
        }));
        add(box);

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);

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
        box.add(tableContainer);
        table.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(computeRefreshInterval())) {
            @Override
            protected boolean shouldTrigger() {
                return isAutoRefreshEnabled();
            }
        });

        TableHeadersToolbar headersTop = new TableHeadersToolbar(table, provider);
        headersTop.setOutputMarkupId(true);
        table.addTopToolbar(headersTop);

        box.add(createHeader(ID_HEADER));
        WebMarkupContainer footer = createFooter(ID_FOOTER);
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() ||  provider.size() > pageSize));
        box.add(footer);
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
    };

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

    protected boolean hideFooterIfSinglePage(){
        return false;
    }

    @Override
    public DataTable getDataTable() {
        return (DataTable) get(ID_BOX).get(ID_TABLE_CONTAINER).get(ID_TABLE);
    }

    public WebMarkupContainer getDataTableContainer() {
        return (WebMarkupContainer) get(ID_BOX).get(ID_TABLE_CONTAINER);
    }

    @Override
    public UserProfileStorage.TableId getTableId() {
        return tableId;
    }

    @Override
    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
    }

    @Override
    public int getItemsPerPage() {
        return (int) getDataTable().getItemsPerPage();
    }

    @Override
    public void setShowPaging(boolean show) {
        // todo make use of this [lazyman]
        this.showPaging = show;

        if (!show) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(10);
        }
    }

    public WebMarkupContainer getHeader() {
        return (WebMarkupContainer) get(ID_BOX).get(ID_HEADER);
    }

    public WebMarkupContainer getFooter() {
        return (WebMarkupContainer) get(ID_BOX).get(ID_FOOTER);
    }

    protected WebMarkupContainer createHeader(String headerId) {
        WebMarkupContainer header = new WebMarkupContainer(headerId);
        header.setVisible(false);
        return header;
    }

    protected WebMarkupContainer createFooter(String footerId) {
        return new PagingFooter(footerId, ID_PAGING_FOOTER, this, this);
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

            final Label count = new Label(ID_COUNT, new IModel<String>() {

                @Override
                public String getObject() {
                    return CountToolbar.createCountString(PagingFooter.this, dataTable);
                }
            });
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, dataTable, true) {

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    target.add(count);
                }

                @Override
                protected boolean isCountingDisabled(){
                    if (dataTable.getDataProvider() instanceof SelectableBeanObjectDataProvider){
                        return !((SelectableBeanObjectDataProvider)dataTable.getDataProvider()).isUseObjectCounting();
                    }
                    return super.isCountingDisabled();
                }
            };
            footerContainer.add(nb2);

            TableConfigurationPanel menu = new TableConfigurationPanel(ID_MENU) {

                @Override
                protected void pageSizeChanged(AjaxRequestTarget target) {
                    Table table = findParent(Table.class);
                    UserProfileStorage.TableId tableId = table.getTableId();

                    if (tableId != null) {
                        PageBase page = (PageBase) getPage();
                        Integer pageSize = page.getSessionStorage().getUserProfile().getPagingSize(tableId);

                        table.setItemsPerPage(pageSize);
                    }
                    target.add(findParent(PagingFooter.class));
                    target.add((Component) table);
                }

            };
            footerContainer.add(menu);
            add(footerContainer);
        }

        public Component getFooterButtonToolbar() {
            return get(ID_BUTTON_TOOLBAR);
        }

        public Component getFooterMenu() {
            return get(ID_FOOTER_CONTAINER).get(ID_MENU);
        }

        public Component getFooterCountLabel() {
            return get(ID_FOOTER_CONTAINER).get(ID_COUNT);
        }

        public Component getFooterPaging() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGING);
        }
    }
}
