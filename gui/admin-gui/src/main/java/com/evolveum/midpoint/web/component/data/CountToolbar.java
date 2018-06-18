/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class CountToolbar extends AbstractToolbar {

    private static final String ID_TD = "td";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGE_SIZE = "pageSize";

    public CountToolbar(DataTable<?, ?> table) {
        super(table);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer td = new WebMarkupContainer(ID_TD);
        td.add(AttributeModifier.replace("colspan", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return String.valueOf(getTable().getColumns().size());
            }
        }));
        add(td);

        Label count = new Label(ID_COUNT, createModel(this, getTable()));
        count.setRenderBodyOnly(true);
        td.add(count);

        PageSizePopover popover = new PageSizePopover(ID_PAGE_SIZE) {

            @Override
            protected void pageSizeChanged(AjaxRequestTarget target) {
                CountToolbar.this.pageSizeChanged(target);
            }
        };
        popover.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return CountToolbar.this.isPageSizePopupVisible();
            }
        });
        td.add(popover);
    }

    private IModel<String> createModel(Component component, IPageable pageable) {
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                return createCountString(component, pageable);
            }
        };
    }

    public static String createCountString(Component component, IPageable pageable){
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
                return PageBase.createStringResourceStatic(component, "CountToolbar.label.unknownCount",
                        new Object[] { from, to }).getString();
            }

            return PageBase.createStringResourceStatic(component, "CountToolbar.label", new Object[]{from, to, count}).getString();
        }

        return PageBase.createStringResourceStatic(component, "CountToolbar.noFound").getString();
    }

    protected void pageSizeChanged(AjaxRequestTarget target) {
    }

    protected boolean isPageSizePopupVisible() {
        return true;
    }
}
