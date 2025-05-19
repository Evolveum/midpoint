/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxOrderByLink;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByLink;
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
    private static final String ORDER_LINK_ID = "orderByLink";
    private static final String HEADERS_ID = "headers";
    private static final String HEADER_ID = "header";
    private static final String HEADER_BODY_ID = "header_body";
    private static final String LABEL_ID = "label";

    public TableHeadersToolbar(DataTable<T, String> table, ISortStateLocator stateLocator) {
        super(table, stateLocator);

    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        /* added for WCAG issue 5.2.4 The header is not visible but is read by the screen reader */
        RefreshingView headers = (RefreshingView) get(HEADERS_ID);
        headers.visitChildren(WebMarkupContainer.class, new IVisitor<WebMarkupContainer, Void>() {
            @Override
            public void component(WebMarkupContainer headerObject, IVisit<Void> visit) {
                headerObject.visitChildren(Label.class, new IVisitor<Label, Void>() {
                    @Override
                    public void component(Label labelObject, IVisit<Void> labelVisit) {
                        if (HIDDEN_HEADER_ID.equals(labelObject.getMarkupId())) {
                            headerObject.get(HEADER_ID).add(AttributeAppender.append("aria-hidden", "true"));
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
                String ariaSort;
                if (dir == SortOrder.ASCENDING) {
                    cssClass = "sortable asc";
                    ariaSort = "ascending";
                } else if (dir == SortOrder.DESCENDING) {
                    cssClass = "sortable desc";
                    ariaSort = "descending";
                } else {
                    cssClass = "sortable";
                    ariaSort = "none";
                }

                if (!Strings.isEmpty(cssClass)) {
                    tag.append("class", cssClass, " ");
                    tag.append("aria-sort", ariaSort, " ");
                }
            }

            @Override
            protected OrderByLink newOrderByLink(String id, Object property, ISortStateLocator stateLocator) {
                return new AjaxOrderByLink<String>(ORDER_LINK_ID, (String) property, stateLocator) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onAjaxClick(target);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);

                        SortOrder currentOrder = stateLocator.getSortState().getPropertySortOrder(property);

                        String columnLabel = getColumnLabelFromParent();
                        if (columnLabel == null || columnLabel.isEmpty()) {
                            columnLabel = property.toString();
                        }

                        String ariaLabel = switch (currentOrder) {
                            case ASCENDING ->
                                    LocalizationUtil.translate("TableHeadersToolbar.item.sorted.ascending", columnLabel);
                            case DESCENDING ->
                                    LocalizationUtil.translate("TableHeadersToolbar.item.sorted.descending", columnLabel);
                            default -> LocalizationUtil.translate("TableHeadersToolbar.item.unsorted", columnLabel);
                        };
                        tag.put("aria-label", ariaLabel);
                    }

                    private String getColumnLabelFromParent() {
                        Component c = getParent().get(ORDER_LINK_ID + ":" + HEADER_BODY_ID + ":"+ LABEL_ID);
                        if (c instanceof Label) {
                            return c.getDefaultModelObjectAsString();
                        }
                        return null;
                    }
                };
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
