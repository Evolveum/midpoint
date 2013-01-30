/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * @author lazyman
 */
public class TablePanel<T> extends Panel {

    private static final String TABLE = "table";
    private static final String NAV_TOP = "navigatorTop";
    private static final String NAV_BOTTOM = "navigatorBottom";
    private NavigatorPanel topNavigator;
    private NavigatorPanel bottomNavigator;

    public TablePanel(String id, ISortableDataProvider provider, List<IColumn<T>> columns) {
        this(id, provider, columns, 10);
    }

    public TablePanel(String id, ISortableDataProvider provider, List<IColumn<T>> columns, int itemsPerPage) {
        super(id);
        Validate.notNull(provider, "Object type must not be null.");
        Validate.notNull(columns, "Columns must not be null.");

        initLayout(columns, itemsPerPage, provider);
    }

    private void initLayout(List<IColumn<T>> columns, int itemsPerPage, ISortableDataProvider provider) {
        DataTable<T> table = new SelectableDataTable<T>(TABLE, columns, provider, itemsPerPage);
        table.addTopToolbar(new TableHeadersToolbar(table, provider));
        table.setOutputMarkupId(true);
        add(table);
        topNavigator = new NavigatorPanel(NAV_TOP, table, showPagedPaging(provider)) {
			@Override
			protected void onAjaxEvent(AjaxRequestTarget target) {
				super.onAjaxEvent(target);
				target.add(bottomNavigator);
			}
        };
        
        bottomNavigator = new NavigatorPanel(NAV_BOTTOM, table, showPagedPaging(provider)) {
        	@Override
			protected void onAjaxEvent(AjaxRequestTarget target) {
				super.onAjaxEvent(target);
				target.add(topNavigator);
			}
        };
        
        add(topNavigator);
        add(bottomNavigator);
        
    }

    private boolean showPagedPaging(ISortableDataProvider provider) {
        if (!(provider instanceof BaseSortableDataProvider)) {
            return true;
        }

        BaseSortableDataProvider baseProvider = (BaseSortableDataProvider) provider;
        return baseProvider.isSizeAvailable();
    }

    public DataTable getDataTable() {
        return (DataTable) get("table");
    }

    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
    }

    public void setShowPaging(boolean showPaging) {
        Component nav = get(NAV_TOP);
        nav.setVisible(showPaging);

        nav = get(NAV_BOTTOM);
        nav.setVisible(showPaging);

        if (!showPaging) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(10);
        }
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

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderJavaScriptReference(new PackageResourceReference(TablePanel.class, "TablePanel.js"));
        response.renderOnLoadJavaScript("initTable()");
    }
}
