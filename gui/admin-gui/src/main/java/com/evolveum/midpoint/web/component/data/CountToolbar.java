/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lazyman
 */
public class CountToolbar extends AbstractToolbar {

    private static final String ID_TD = "td";

    private static final String ID_FORM = "form";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGE_SIZE = "pageSize";

    public CountToolbar(DataTable<?, ?> table) {
        super(table);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer td = new WebMarkupContainer(ID_TD);
        td.add(AttributeModifier.replace("colspan", () -> String.valueOf(getTable().getColumns().size())));
        add(td);

        Label count = new Label(ID_COUNT, createModel(this, getTable()));
        count.setRenderBodyOnly(true);
        td.add(count);

        Form form = new MidpointForm(ID_FORM);
        td.add(form);

        PagingSizePanel pageSize = new PagingSizePanel(ID_PAGE_SIZE) {

            @Override
            protected void onPageSizeChangePerformed(Integer newValue, AjaxRequestTarget target) {
                CountToolbar.this.pageSizeChanged(target);
            }
        };
        pageSize.add(new VisibleBehaviour(() -> CountToolbar.this.isPageSizePopupVisible()));
        form.add(pageSize);
    }

    private IModel<String> createModel(Component component, IPageable pageable) {
        return new LoadableModel<>() {

            @Override
            protected String load() {
                return createCountString(pageable);
            }
        };
    }

    public static String createCountString(IPageable pageable) {
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
                return PageBase.createStringResourceStatic("CountToolbar.label.unknownCount",
                        new Object[] { from, to }).getString();
            }

            return PageBase.createStringResourceStatic("CountToolbar.label", new Object[] { from, to, count }).getString();
        }

        return PageBase.createStringResourceStatic("CountToolbar.noFound").getString();
    }

    protected void pageSizeChanged(AjaxRequestTarget target) {
    }

    protected boolean isPageSizePopupVisible() {
        return true;
    }
}
