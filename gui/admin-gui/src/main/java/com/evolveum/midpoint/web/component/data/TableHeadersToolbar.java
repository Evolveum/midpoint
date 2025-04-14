/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;

import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

/**
 * @author lazyman
 */
public class TableHeadersToolbar<T> extends AjaxFallbackHeadersToolbar<String> {

    public static final String HIDDEN_HEADER_ID = "hiddenHeaderId";

    public TableHeadersToolbar(DataTable<T, String> table, ISortStateLocator stateLocator) {
        super(table, stateLocator);

    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        /* added for WCAG issue 5.2.4 The header is not visible but is read by the screen reader */
        RefreshingView headers = (RefreshingView) get("headers");
        headers.visitChildren(WebMarkupContainer.class, new IVisitor<WebMarkupContainer, Void>() {
            @Override
            public void component(WebMarkupContainer headerObject, IVisit<Void> visit) {
                headerObject.visitChildren(Label.class, new IVisitor<Label, Void>() {
                    @Override
                    public void component(Label labelObject, IVisit<Void> labelVisit) {
                        if (HIDDEN_HEADER_ID.equals(labelObject.getMarkupId())) {
                            headerObject.get("header").add(AttributeAppender.append("aria-hidden", "true"));
                            visit.stop();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected WebMarkupContainer newSortableHeader(String headerId, final String property, final ISortStateLocator locator) {
        IDataProvider provider = getTable().getDataProvider();
        if (provider instanceof BaseSortableDataProvider) {
            BaseSortableDataProvider sortableDataProvider = (BaseSortableDataProvider) provider;
            if (sortableDataProvider.isOrderingDisabled()) {
                return new WebMarkupContainer(headerId);
            }
        }

        return new AjaxFallbackOrderByBorder(headerId, property, locator) {

            @Override
            protected void onSortChanged() {
                TableHeadersToolbar.this.onSortChanged();
            }

            @Override
            protected void onAjaxClick(AjaxRequestTarget target) {
                refreshTable(target);
            }

            @Override
            public void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                ISortState sortState = locator.getSortState();
                SortOrder dir = sortState.getPropertySortOrder(property);
                String cssClass;
                if (dir == SortOrder.ASCENDING) {
                    cssClass = "sortable asc";
                } else if (dir == SortOrder.DESCENDING) {
                    cssClass = "sortable desc";
                } else {
                    cssClass = "sortable";
                }

                if (!Strings.isEmpty(cssClass)) {
                    tag.append("class", cssClass, " ");
                }
            }
        };
    }

    protected void onSortChanged() {
        getTable().setCurrentPage(0);
    }

    protected void refreshTable(AjaxRequestTarget target) {
        target.add(getTable());
    }
}
