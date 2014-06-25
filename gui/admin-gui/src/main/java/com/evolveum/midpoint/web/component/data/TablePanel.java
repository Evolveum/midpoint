/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author lazyman
 */
public class TablePanel<T> extends Panel {

    private static final String ID_TABLE = "table";
    private static final String ID_PAGING = "paging";

    private IModel<Boolean> showPaging = new Model<Boolean>(true);
    private IModel<Boolean> showCount = new Model<Boolean>(true);

    private UserProfileStorage.TableId tableId;

    public TablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns) {
        this(id, provider, columns, null);
    }

    public TablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
                      UserProfileStorage.TableId tableId) {
        super(id);
        Validate.notNull(provider, "Object type must not be null.");
        Validate.notNull(columns, "Columns must not be null.");

        add(AttributeModifier.prepend("style", "display: table; width: 100%;"));

        this.tableId = tableId;

        initLayout(columns, provider);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        DataTable table = (DataTable) get(ID_TABLE);
        PageBase page = (PageBase) getPage();
        UserProfileStorage userProfile = page.getSessionStorage().getUserProfile();
        table.setItemsPerPage(userProfile.getPagingSize(tableId));
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider) {
        DataTable<T, String> table = new SelectableDataTable<T>(ID_TABLE, columns, provider,
                UserProfileStorage.DEFAULT_PAGING_SIZE);

        table.setOutputMarkupId(true);

        TableHeadersToolbar headers = new TableHeadersToolbar(table, provider);
        headers.setOutputMarkupId(true);
        table.addTopToolbar(headers);

        CountToolbar count = new CountToolbar(table) {

            @Override
            protected void pageSizeChanged(AjaxRequestTarget target) {
                PageBase page = (PageBase) getPage();
                Integer pageSize = page.getSessionStorage().getUserProfile().getPagingSize(tableId);

                setItemsPerPage(pageSize);
                target.add(getNavigatorPanel());
                target.add(getDataTable());
            }

            @Override
            protected boolean isPageSizePopupVisible() {
                return tableId != null;
            }

        };
        addVisibleBehaviour(count, showCount);
        table.addBottomToolbar(count);

        add(table);

        NavigatorPanel nb2 = new NavigatorPanel(ID_PAGING, table, showPagedPaging(provider));
        addVisibleBehaviour(nb2, showPaging);
        add(nb2);
    }

    public UserProfileStorage.TableId getTableId() {
        return tableId;
    }

    private void addVisibleBehaviour(Component comp, final IModel<Boolean> model) {
        comp.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return model.getObject();
            }
        });
    }

    private boolean showPagedPaging(ISortableDataProvider provider) {
        if (!(provider instanceof BaseSortableDataProvider)) {
            return true;
        }

        BaseSortableDataProvider baseProvider = (BaseSortableDataProvider) provider;
        return baseProvider.isSizeAvailable();
    }

    public DataTable getDataTable() {
        return (DataTable) get(ID_TABLE);
    }

    public NavigatorPanel getNavigatorPanel() {
        return (NavigatorPanel) get(ID_PAGING);
    }

    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
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

    public void setShowPaging(boolean showPaging) {
        this.showPaging.setObject(showPaging);
        this.showCount.setObject(showPaging);

        if (!showPaging) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(10);
        }
    }

    public void setShowCount(boolean showCount) {
        this.showCount.setObject(showCount);
    }

    public void setTableCssClass(String cssClass) {
        Validate.notEmpty(cssClass, "Css class must not be null or empty.");

        DataTable table = getDataTable();
        table.add(new AttributeAppender("class", new Model(cssClass), " "));
    }

    public void setStyle(String value) {
        Validate.notEmpty(value, "Value must not be null or empty.");

        DataTable table = getDataTable();
        table.add(new AttributeModifier("style", new Model(value)));
    }
}
