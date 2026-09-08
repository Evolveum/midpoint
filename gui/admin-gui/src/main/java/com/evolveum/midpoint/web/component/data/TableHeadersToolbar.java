/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;

import java.io.Serial;

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
//
//    @Override
//    protected void onBeforeRender() {
//        super.onBeforeRender();
//
//        /* added for WCAG issue 5.2.4 The header is not visible but is read by the screen reader */
//        RefreshingView headers = (RefreshingView) get(HEADERS_ID);
//        headers.visitChildren(WebMarkupContainer.class, new IVisitor<WebMarkupContainer, Void>() {
//            @Override
//            public void component(WebMarkupContainer headerObject, IVisit<Void> visit) {
//                headerObject.visitChildren(Label.class, new IVisitor<Label, Void>() {
//                    @Override
//                    public void component(Label labelObject, IVisit<Void> labelVisit) {
//                        if (HIDDEN_HEADER_ID.equals(labelObject.getMarkupId())) {
//                            headerObject.get(HEADER_ID).add(AttributeAppender.append("aria-hidden", "true"));
//                            visit.stop();
//                        }
//                    }
//                });
//            }
//        });
//    }

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
                AjaxOrderByLink<String> link = new AjaxOrderByLink<String>(ORDER_LINK_ID, (String) property, stateLocator) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SortOrder currentOrder = stateLocator.getSortState().getPropertySortOrder(property);
                        String columnLabel = getColumnLabel(property);

                        onAjaxClick(target);

                        TableHeadersToolbar.this.announceSortStatus(target, currentOrder, columnLabel);
                        target.focusComponent(this);
                    }

                    private String getColumnLabel(Object property) {
                        Component c = get(HEADER_BODY_ID + ":" + LABEL_ID);
                        if (c instanceof Label && c.getDefaultModelObjectAsString() != null
                                && !c.getDefaultModelObjectAsString().isEmpty()) {
                            return c.getDefaultModelObjectAsString();
                        }
                        return property.toString();
                    }
                };
                link.setOutputMarkupId(true);
                return link;
            }
        };
    }

    protected void onSortChanged() {
        getTable().setCurrentPage(0);
    }

    protected void refreshTable(AjaxRequestTarget target) {
        target.add(getTable());
    }

    private void announceSortStatus(AjaxRequestTarget target, SortOrder order, String columnLabel) {
        Table table = getTable().findParent(Table.class);
        if (table == null) {
            return;
        }

        String liveStatusId = table.getLiveStatusMarkupId();
        if (liveStatusId == null) {
            return;
        }

        String message = switch (order) {
            case ASCENDING -> LocalizationUtil.translate("TableHeadersToolbar.aria.status.sorted.ascending", columnLabel);
            case DESCENDING -> LocalizationUtil.translate("TableHeadersToolbar.aria.status.sorted.descending", columnLabel);
            default -> LocalizationUtil.translate("TableHeadersToolbar.item.unsorted", columnLabel);
        };

        target.appendJavaScript(
                String.format("MidPointTheme.updateStatusMessage('%s', '%s', %d)", liveStatusId, message, 300));
    }
}
