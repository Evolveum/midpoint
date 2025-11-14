/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.table;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

/**
 * Collapsable data table with collapsible content per row.
 */
public abstract class CollapsableDataTable<T, S> extends DataTable<T, S> {

    public CollapsableDataTable(String id,
            List<? extends IColumn<T, S>> columns,
            IDataProvider<T> dataProvider,
            long rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
        setOutputMarkupId(true);
    }

    @Override
    protected Item<T> newRowItem(String id, int index, IModel<T> model) {
        return new CollapsableRowItem(id, index, model);
    }

    /** Row item class that maintains its own expanded/collapsed state. */
    public class CollapsableRowItem extends Item<T> {

        private static final String COLLAPSIBLE_CONTAINER_ID = "collapseContainer";
        public static final String COLLAPSIBLE_CONTENT_ID = "collapsibleContent";

        @Serial private static final long serialVersionUID = 1L;
        private boolean expanded = false;

        public CollapsableRowItem(String id, int index, IModel<T> model) {
            super(id, index, model);
            applyDefaultMarkupSetting();
        }

        private void applyDefaultMarkupSetting() {
            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();

            var container = buildCollapseContainer();
            add(container);

            Component collapsibleContent = CollapsableDataTable.this.createCollapsibleContent(COLLAPSIBLE_CONTENT_ID, getModel());
            collapsibleContent.setOutputMarkupId(true);
            container.add(collapsibleContent);
        }

        private @NotNull WebMarkupContainer buildCollapseContainer() {
            WebMarkupContainer collapsibleContainer = new WebMarkupContainer(COLLAPSIBLE_CONTAINER_ID);
            collapsibleContainer.add(new VisibleBehaviour(this::isExpanded));
            collapsibleContainer.setOutputMarkupId(true);
            collapsibleContainer.setOutputMarkupPlaceholderTag(true);
            return collapsibleContainer;
        }

        public Component getCollapsibleContainer() {
            return get(COLLAPSIBLE_CONTAINER_ID);
        }

        public Component getCollapsibleContent() {
            return get(COLLAPSIBLE_CONTAINER_ID)
                    .get(COLLAPSIBLE_CONTENT_ID);
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public void toggle(@NotNull AjaxRequestTarget target) {
            this.expanded = !this.expanded;
            target.add(this);
        }
        public void toggleAndReplace(@NotNull AjaxRequestTarget target,Component componentToReplace) {
            this.expanded = !this.expanded;
            getCollapsibleContent().replaceWith(componentToReplace);
            target.add(this);
        }
    }

    /**
     * Called by parent panel to attach collapsible panels per row.
     */
    public abstract Component createCollapsibleContent(String id, IModel<T> rowModel);

}
