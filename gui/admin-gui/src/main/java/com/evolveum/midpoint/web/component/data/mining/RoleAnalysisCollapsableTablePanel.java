/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.mining;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.AttributeModifier;
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
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.data.CountToolbar;
import com.evolveum.midpoint.web.component.data.PagingSizePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.TableHeadersToolbar;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * <p>NOTE: This class is experimental and may be removed in the future.</p>
 * Initializes a collapsible table for displaying items.
 *
 * <p>When using this class, ensure that specific IDs are used for the collapsible components,
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
 */
public class RoleAnalysisCollapsableTablePanel<T> extends BasePanel<T> implements Table {

    @Serial private static final long serialVersionUID = 1L;

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

    public static final String ID_FIRST_COLLAPSABLE_CONTAINER = "collapseContainerFirst";
    public static final String ID_SECOND_COLLAPSABLE_CONTAINER = "collapseContainerSecond";
    public static final String ID_COLLAPSABLE_CONTENT = "collapsableContent";

    private boolean showAsCard = true;

    private final UserProfileStorage.TableId tableId;
    private final boolean isRefreshEnabled;
    private String additionalBoxCssClasses = null;

    //interval in seconds

    public RoleAnalysisCollapsableTablePanel(String id, ISortableDataProvider<T, String> provider, List<IColumn<T, String>> columns) {
        this(id, provider, columns, null);
    }

    public RoleAnalysisCollapsableTablePanel(String id, ISortableDataProvider<T, String> provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId) {
        this(id, provider, columns, tableId, false);
    }

    public RoleAnalysisCollapsableTablePanel(String id, ISortableDataProvider<T, String> provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, boolean isRefreshEnabled) {
        super(id);
        this.tableId = tableId;
        this.isRefreshEnabled = isRefreshEnabled;
        initLayout(columns, provider);
    }

    public void setShowAsCard(boolean showAsCard) {
        this.showAsCard = showAsCard;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.initResponsiveTable();"));
    }

    protected Item<T> newRowItem(String id, int index, Item<T> item, @NotNull IModel<T> rowModel) {

        CollapsableContainerPanel webMarkupContainerFirst = new CollapsableContainerPanel(ID_FIRST_COLLAPSABLE_CONTAINER);
        webMarkupContainerFirst.setOutputMarkupId(true);
        webMarkupContainerFirst.add(AttributeModifier.replace("class", "collapse"));
        webMarkupContainerFirst.add(AttributeModifier.replace("style", "display: none;"));
        item.add(webMarkupContainerFirst);
        WebMarkupContainer webMarkupContainerContentFirst = new WebMarkupContainer(ID_COLLAPSABLE_CONTENT);
        webMarkupContainerContentFirst.setOutputMarkupId(true);
        webMarkupContainerFirst.add(webMarkupContainerContentFirst);

        CollapsableContainerPanel webMarkupContainerSecond = new CollapsableContainerPanel(ID_SECOND_COLLAPSABLE_CONTAINER);
        webMarkupContainerSecond.setOutputMarkupId(true);
        webMarkupContainerSecond.add(AttributeModifier.replace("class", "collapse"));
        webMarkupContainerSecond.add(AttributeModifier.replace("style", "display: none;"));
        item.add(webMarkupContainerSecond);

        WebMarkupContainer webMarkupContainerContentSecond = new WebMarkupContainer(ID_COLLAPSABLE_CONTENT);
        webMarkupContainerContentSecond.setOutputMarkupId(true);
        webMarkupContainerSecond.add(webMarkupContainerContentSecond);

        return customizeNewRowItem(item, rowModel);
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider<T, String> provider) {
        setOutputMarkupId(true);
        add(AttributeAppender.prepend("class", () -> isShowAsCard() ? "card" : ""));
        add(AttributeAppender.append("class", this::getAdditionalBoxCssClasses));

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);

        int pageSize = getItemsPerPage(tableId);
        CustomDataTable<T, String> table = new CustomSelectableDataTable<>(ID_TABLE, columns, provider, pageSize) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected Item<T> newRowItem(String id, int index, IModel<T> rowModel) {
                Item<T> item = super.newRowItem(id, index, rowModel);
                return RoleAnalysisCollapsableTablePanel.this.newRowItem(id, index, item, rowModel);
            }

        };
        table.setOutputMarkupId(true);
        tableContainer.add(table);
        add(tableContainer);

        TableHeadersToolbar<?> headersTop = new TableHeadersToolbar<>(table, provider) {

            @Override
            protected void refreshTable(AjaxRequestTarget target) {
                super.refreshTable(target);
                target.add(getFooter());
            }
        };
        headersTop.setOutputMarkupId(true);
        table.addTopToolbar(headersTop);

        Component header = createHeader(ID_HEADER);
        header.setOutputMarkupId(true);
        add(header);
        add(AttributeAppender.append("aria-labelledby", header.getMarkupId()));
        WebMarkupContainer footer = createFooter();
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || provider.size() > pageSize));
        add(footer);
    }

    public static @NotNull String getCollapseScript(Component webMarkupContainer, Component hide) {
        return "var collapseContainer = document.getElementById('"
                + webMarkupContainer.getMarkupId() + "');" +
                "if (collapseContainer.classList.contains('collapse') "
                + "&& collapseContainer.classList.contains('show')) {" +
                "    collapseContainer.classList.remove('show');" +
                "    collapseContainer.style.display = 'none';" +
                "} else {" +
                "    collapseContainer.classList.add('show');" +
                "    collapseContainer.style.display = '';" +
                "    collapseContainer.style.pointerEvents = 'none !important';" +
                "}" +
                "var collapseContainerSecond = document.getElementById('"
                + hide.getMarkupId() + "');" +
                "if (collapseContainerSecond.classList.contains('collapse') "
                + "&& collapseContainerSecond.classList.contains('show')) {" +
                "    collapseContainerSecond.classList.remove('show');" +
                "    collapseContainerSecond.style.display = 'none';" +
                "}";
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

    public int getAutoRefreshInterval() {
        return 0;
    }

    public boolean isAutoRefreshEnabled() {
        return false;
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    @Override
    public DataTable<T, String> getDataTable() {
        Component tableContainer = get(ID_TABLE_CONTAINER);
        if (tableContainer instanceof WebMarkupContainer container) {
            Component table = container.get(ID_TABLE);
            if (table instanceof DataTable) {
                @SuppressWarnings("unchecked")
                DataTable<T, String> tableComponent = (DataTable<T, String>) table;
                return tableComponent;
            }
        }
        throw new IllegalStateException("DataTable not found or has an unexpected type.");
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

    protected WebMarkupContainer createFooter() {
        return new PagingFooter(RoleAnalysisCollapsableTablePanel.ID_FOOTER, ID_PAGING_FOOTER, this, this) {

            @Override
            protected String getPaginationCssClass() {
                return RoleAnalysisCollapsableTablePanel.this.getPaginationCssClass();
            }

            @Override
            protected boolean isPagingVisible() {
                return RoleAnalysisCollapsableTablePanel.this.isPagingVisible();
            }
        };
    }

    protected boolean isPagingVisible() {
        return true;
    }

    protected String getPaginationCssClass() {
        return "pagination-sm";
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

        public PagingFooter(String id, String markupId, RoleAnalysisCollapsableTablePanel markupProvider, Table table) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider, table);
        }

        private void initLayout(final RoleAnalysisCollapsableTablePanel<?> boxedTablePanel, final Table table) {
            WebMarkupContainer buttonToolbar = boxedTablePanel.createButtonToolbar(ID_BUTTON_TOOLBAR);
            add(buttonToolbar);

            final DataTable<?, ?> dataTable = table.getDataTable();
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);
            footerContainer.add(new VisibleBehaviour(this::isPagingVisible));

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
                        return !((SelectableBeanContainerDataProvider<?>) dataTable.getDataProvider()).isUseObjectCounting();
                    }
                    return super.isCountingDisabled();
                }

                @Override
                protected String getPaginationCssClass() {
                    return PagingFooter.this.getPaginationCssClass();
                }
            };
            footerContainer.add(nb2);

            Form<?> form = new MidpointForm<>(ID_FORM);
            footerContainer.add(form);
            PagingSizePanel menu = new PagingSizePanel(ID_PAGE_SIZE) {

                @Override
                protected void onPageSizeChangePerformed(Integer newValue, AjaxRequestTarget target) {
                    Table table = findParent(Table.class);
                    UserProfileStorage.TableId tableId = table.getTableId();

                    if (tableId != null && table.enableSavePageSize()) {
                        table.setItemsPerPage(newValue);
                    }
                    target.add(findParent(PagingFooter.class));
                    target.add((Component) table);
                }
            };
            // todo nasty hack, we should decide whether paging should be normal or "small"
            menu.setSmall(getPaginationCssClass() != null);
            form.add(menu);
            add(footerContainer);
        }

        protected String getPaginationCssClass() {
            return "pagination-sm";
        }

        protected boolean isPagingVisible() {
            return true;
        }

    }

    public boolean isShowAsCard() {
        return showAsCard;
    }

}
