/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class BoxedTablePanel<T> extends SimplePanel {

    private static final String ID_HEADER = "header";
    private static final String ID_FOOTER = "footer";
    private static final String ID_TABLE = "table";

    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";

    public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
                           UserProfileStorage.TableId tableId, int pageSize) {
        super(id);

        initLayout(columns, provider, pageSize);
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider, int pageSize) {
        DataTable<T, String> table = new SelectableDataTable<>(ID_TABLE, columns, provider, pageSize);
        add(table);

        TableHeadersToolbar headersTop = new TableHeadersToolbar(table, provider);
        headersTop.setOutputMarkupId(true);
        table.addTopToolbar(headersTop);

        add(createHeader(ID_HEADER));
        add(createFooter(ID_FOOTER));
    }

    private DataTable getTable() {
        return (DataTable) get(ID_TABLE);
    }

    protected WebMarkupContainer createHeader(String headerId) {
        WebMarkupContainer header = new WebMarkupContainer(headerId);
        header.setVisible(false);
        return header;
    }

    protected WebMarkupContainer createFooter(String footerId) {
        return new PagingFooter(footerId, ID_PAGING_FOOTER, this, getTable());
    }

    public DataTable getDataTable() {
        return (DataTable) get(ID_TABLE);
    }

    public void setCurrentPage(ObjectPaging paging) {
        if (paging == null) {
            getDataTable().setCurrentPage(0);
            return;
        }

        long itemsPerPage = getDataTable().getItemsPerPage();
        long page = ((paging.getOffset() + itemsPerPage) / itemsPerPage) - 1;
        if (page < 0) {
            page = 0;
        }

        getDataTable().setCurrentPage(page);
    }

    private static class PagingFooter extends Fragment {

        public PagingFooter(String id, String markupId, MarkupContainer markupProvider, DataTable table) {
            super(id, markupId, markupProvider);

            initLayout(table);
        }

        private void initLayout(final DataTable table) {
            BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, table, true);
            add(nb2);

            Label count = new Label(ID_COUNT, new AbstractReadOnlyModel<String>() {

                @Override
                public String getObject() {
                    return createCountString(table);
                }
            });
            add(count);
        }

        private String createCountString(IPageable pageable) {
            long from = 0;
            long to = 0;
            long count = 0;

            if (pageable instanceof DataViewBase) {
                DataViewBase view = (DataViewBase) pageable;

                from = view.getFirstItemOffset() + 1;
                to = from + view.getItemsPerPage() - 1;
                long itemCount = view.getItemCount();
                if (to > itemCount) {
                    to = itemCount;
                }
                count = itemCount;
            } else if (pageable instanceof DataTable) {
                DataTable table = (DataTable) pageable;

                from = table.getCurrentPage() * table.getItemsPerPage() + 1;
                to = from + table.getItemsPerPage() - 1;
                long itemCount = table.getItemCount();
                if (to > itemCount) {
                    to = itemCount;
                }
                count = itemCount;
            }

            if (count > 0) {
                if (count == Integer.MAX_VALUE) {
                    return new StringResourceModel("CountToolbar.label", PagingFooter.this, null,
                            new Object[]{from, to}).getString();
                }

                return new StringResourceModel("CountToolbar.label", PagingFooter.this, null,
                        new Object[]{from, to, count}).getString();
            }

            return new StringResourceModel("CountToolbar.noFound", PagingFooter.this, null).getString();
        }
    }
}
